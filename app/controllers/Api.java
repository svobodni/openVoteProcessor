package controllers;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Ballot;
import models.BallotType;
import models.BallotOption;
import models.ValidTokens;
import models.Vote;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cz.svobodni.vk.openVote.XMLSignService;
import cz.svobodni.vk.openVote.XMLValidateService;
import play.*;
import play.libs.XPath;
import play.mvc.*;
import views.html.*;
import play.db.jpa.*;
import play.mvc.BodyParser.*;
import play.cache.*;


public class Api extends Controller {
		
	@Transactional
	@BodyParser.Of(Xml.class)
	public static Result vote() throws Exception {
		Document dom = request().body().asXml();
		if(dom == null) {
			return badRequest("Expecting XML data.");
		} else {
			Map<String, String> namespaces = new HashMap<String,String>();
			namespaces.put("ballot", "http://xsd.svobodni.cz/vs/Ballot-1.0.xsd");
			namespaces.put("vote", "http://xsd.svobodni.cz/vs/Vote-1.0.xsd");
			namespaces.put("token", "http://xsd.svobodni.cz/vs/Token-1.0.xsd");
			namespaces.put("sig", "http://www.w3.org/2000/09/xmldsig#");
			
			Node tokenNode = XPath.selectNode("/vote:Vote/token:Token", dom, namespaces);
			Document tokenDom = XMLValidateService.nodeToDocument(tokenNode);
			
			
			XMLValidateService vsReP = new XMLValidateService("conf/vk2014.jks", "vk2014", "rep2014");
			
			if(vsReP.validate(tokenDom)) {
				Ballot ballot;
				try {
					ballot = (Ballot)JPA.em().createQuery("SELECT b FROM Ballot b WHERE b.externalId = :externalId").
							setParameter("externalId", XPath.selectText("/token:Token/ballot:Ballot/@Id", tokenDom, namespaces)).getSingleResult();
				} catch(Exception e) {		
				
					return notFound("Ballot not found.");
				}
				
				// TODO: kontrola casu
				
				String tokenDigestValue=XPath.selectText("/token:Token/sig:Signature//sig:DigestValue", tokenDom, namespaces);
				if(ballot.getCurrentValidTokens().getDigestValues().contains(tokenDigestValue)) {
					for(Vote v: ballot.getCastVotes()) {
						if(v.getTokenDigestValue().equals(tokenDigestValue)) {
							return forbidden("Token was already used.");
						}
					}					
					Vote vote = new Vote();
					vote.setTokenDigestValue(tokenDigestValue);
					NodeList nl = XPath.selectNodes("/vote:Vote/vote:Option", dom, namespaces);
					Set<BallotOption> voteOptions = new HashSet<BallotOption>();
					for(int i=0;i<nl.getLength();i++) {
						Element op = (Element)nl.item(i);
						boolean found=false;
						for(BallotOption bo: ballot.getOptions()) {
							if(bo.getValue().equals(op.getAttribute("value"))) {
								found=true;
								if(!voteOptions.contains(bo)) {
									voteOptions.add(bo);
								} else {
									return badRequest("Duplicate vote option.");
								}
							}
						}
						if(!found) {
							return badRequest("Vote option not found.");
						}							
					}
					if(voteOptions.size()>0 && voteOptions.size()<=ballot.getMaxChecks()) {
						vote.setBallotOptions(voteOptions);
						for(BallotOption bo: voteOptions) {
							bo.getVotes().add(vote);
						}
						XMLSignService ssVK = new XMLSignService("conf/vk2014.jks", "vk2014", "vk2014", "vk2014");
						ssVK.sign(dom);
						String voteDigestValue=XPath.selectText("/vote:Vote/sig:Signature//sig:DigestValue", dom, namespaces);
						vote.setVoteDigestValue(voteDigestValue);
						ballot.getCastVotes().add(vote);
						vote.setBallot(ballot);
						JPA.em().persist(vote);
						return ok(XMLSignService.documentToString(dom));
					} else {
						return badRequest("Too few or too many vote options.");
					}					
					
				} else {
					return forbidden("Token is invalid.");
				}
			} else {
				return badRequest("Invalid Token signature.");
			}
	  }
	}
	
	@BodyParser.Of(Xml.class)
	public static Result test() throws Exception {
		Document dom = request().body().asXml();
		if(dom == null) {
			return badRequest("Expecting XML data.");
		} else {
			XMLSignService ss = new XMLSignService("conf/vk2014.jks", "vk2014", "rep2014", "rep2014");
			dom=ss.sign(dom);
			System.out.println(XMLSignService.documentToString(dom));
			XMLValidateService vs = new XMLValidateService();
			if(vs.validate(dom)) {
				return ok(XMLSignService.documentToString(dom));
			} else {
				return internalServerError("Signature failed.");
			}
	  }
	}
	
	@Transactional
	@BodyParser.Of(Xml.class)
	public static Result ballot() throws Exception {
		Document dom = request().body().asXml();
		if(dom == null) {
			return badRequest("Expecting XML data.");
		} else {
			XMLValidateService vsReP = new XMLValidateService("conf/vk2014.jks", "vk2014", "rep2014");
			if(vsReP.validate(dom)) {
				Ballot ballot = new Ballot();
				Map<String, String> namespaces = new HashMap<String,String>();
				namespaces.put("ballot", "http://xsd.svobodni.cz/vs/Ballot-1.0.xsd");
				namespaces.put("sig", "http://www.w3.org/2000/09/xmldsig#");
								
				ballot.setExternalId(XPath.selectText("/ballot:Ballot/@Id", dom, namespaces));
				ballot.setName(XPath.selectText("/ballot:Ballot/ballot:Name", dom, namespaces));
				ballot.setDescription(XPath.selectText("/ballot:Ballot/ballot:Description", dom, namespaces));
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				ballot.setStartTime(new Timestamp(df.parse(XPath.selectText("/ballot:Ballot/ballot:StartTime", dom, namespaces)).getTime()));
				ballot.setEndTime(new Timestamp(df.parse(XPath.selectText("/ballot:Ballot/ballot:EndTime", dom, namespaces)).getTime()));
				ballot.setShowVoters(XPath.selectText("/ballot:Ballot/ballot:ShowVoters", dom, namespaces).equalsIgnoreCase("true"));				
				ballot.setBallotType(BallotType.valueOf(XPath.selectText("/ballot:Ballot/ballot:Type", dom, namespaces).toUpperCase()));
				ballot.setMaxChecks(Integer.parseInt(XPath.selectText("/ballot:Ballot/ballot:MaxChecks", dom, namespaces)));
				ballot.setSignatureSubjectName(XPath.selectText("/ballot:Ballot/sig:Signature//sig:X509SubjectName", dom, namespaces));
				ballot.setDigestValue(XPath.selectText("/ballot:Ballot/sig:Signature//sig:DigestValue", dom, namespaces));
				ballot.setOptions(new ArrayList<BallotOption>());
				ballot.setValidTokens(new ArrayList<ValidTokens>());
				JPA.em().persist(ballot);
				
				NodeList options = XPath.selectNodes("/ballot:Ballot/ballot:Option", dom, namespaces);
				for(int i=0;i<options.getLength();i++) {
					Element op = (Element)options.item(i);
					BallotOption option = new BallotOption();
					option.setBallot(ballot);
					ballot.getOptions().add(option);
					option.setName(op.getTextContent());
					option.setValue(op.getAttribute("value"));
					JPA.em().persist(option);
				}
				
				List<Ballot> oldBallots=JPA.em().
					createQuery("SELECT b FROM Ballot b WHERE b.externalId = :externalId AND NOT b.id = :id", Ballot.class).
					setParameter("id", ballot.getId()).
					setParameter("externalId", ballot.getExternalId()).getResultList();
				
				for(Ballot b: oldBallots) {
					JPA.em().remove(b);
				}
				
				return ok("ok");
			} else {
				return unauthorized("XML signature is invalid.");
			}
			
				   
	  }
	}
	
	@Transactional
	@BodyParser.Of(Xml.class)
	public static Result validTokens() throws Exception {
		Document dom = request().body().asXml();
		if(dom == null) {
			return badRequest("Expecting XML data.");
		} else {
			XMLValidateService vsReP = new XMLValidateService("conf/vk2014.jks", "vk2014", "rep2014");
			if(vsReP.validate(dom)) {
				
				Map<String, String> namespaces = new HashMap<String,String>();
				namespaces.put("vt", "http://xsd.svobodni.cz/vs/ValidTokens-1.0.xsd");
				
				Ballot ballot;
				try {
					System.out.println(XPath.selectText("/vt:ValidTokens/@Ballot", dom, namespaces));
					ballot = (Ballot)JPA.em().createQuery("SELECT b FROM Ballot b WHERE b.externalId = :externalId").
							setParameter("externalId", XPath.selectText("/vt:ValidTokens/@Ballot", dom, namespaces)).getSingleResult();
				} catch(Exception e) {		
				
					return notFound("Ballot not found.");
				}
				
				ValidTokens vt = new ValidTokens();
				vt.setCreatedTime(new java.sql.Timestamp(new Date().getTime()));
				vt.setBallot(ballot);
				ballot.getValidTokens().add(vt);
				Collection<String> dvs = new HashSet<String>();
				NodeList nl = XPath.selectNodes("/vt:ValidTokens/vt:TokenDigestValue", dom, namespaces);
				for(int i=0;i<nl.getLength();i++) {
					dvs.add(((Element)nl.item(i)).getTextContent());
				}
				vt.setDigestValues(dvs);
				JPA.em().persist(vt);
				
				return ok("ok");
			} else {
				return unauthorized("XML signature is invalid.");
			}
			
		}
	}

}
