package cz.svobodni.vk.openVote;

import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Iterator;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

public class XMLValidateService {
	
	private XMLSignatureFactory signatureFactory;
	private PublicKey publicKey;
	
	public XMLValidateService() {
		this.signatureFactory = XMLSignatureFactory.getInstance("DOM");
	}
	
	public XMLValidateService(String jksFilename, String jksPassword, String jksEntryName) throws Exception {
		this.signatureFactory = XMLSignatureFactory.getInstance("DOM");
		KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(jksFilename), jksPassword.toCharArray());
        Certificate cert = ks.getCertificate(jksEntryName);
        this.publicKey = cert.getPublicKey();
	}
	
	private static class X509KeySelector extends KeySelector {
		protected PublicKey publicKey;

	    public X509KeySelector(PublicKey publicKey) {
	        super();
	        this.publicKey = publicKey;
	    }

		
	    public KeySelectorResult select(KeyInfo keyInfo,
	                                    KeySelector.Purpose purpose,
	                                    AlgorithmMethod method,
	                                    XMLCryptoContext context)
	        throws KeySelectorException {
	        Iterator ki = keyInfo.getContent().iterator();
	        while (ki.hasNext()) {
	            XMLStructure info = (XMLStructure) ki.next();
	            if (!(info instanceof X509Data))
	                continue;
	            X509Data x509Data = (X509Data) info;
	            Iterator xi = x509Data.getContent().iterator();
	            while (xi.hasNext()) {
	                Object o = xi.next();
	                if (!(o instanceof X509Certificate))
	                    continue;
	                final PublicKey key = ((X509Certificate)o).getPublicKey();
	                
	                if(this.publicKey != null) {
	                	if(!this.publicKey.equals(key)) {
	                		throw new KeySelectorException();
	                	}
	                }
	                // Make sure the algorithm is compatible
	                // with the method.
	                if (algEquals(method.getAlgorithm(), key.getAlgorithm())) {
	                    return new KeySelectorResult() {
	                        public Key getKey() { return key; }
	                    };
	                }
	            }
	        }
	        throw new KeySelectorException("No key found!");
	    }

	    static boolean algEquals(String algURI, String algName) {
	        if ((algName.equalsIgnoreCase("DSA") &&
	            algURI.equalsIgnoreCase(SignatureMethod.DSA_SHA1)) ||
	            (algName.equalsIgnoreCase("RSA") &&
	            algURI.equalsIgnoreCase(SignatureMethod.RSA_SHA1))) {
	            return true;
	        } else {
	            return false;
	        }
	    }
	}
	
	public boolean validate(Document dom) throws Exception {
		
		// Pro pripad, ze XML obsahuje i vnorene podpisy, nas zajima jen ten uplne vnejsi
		int i;
		NodeList nl = dom.getFirstChild().getChildNodes();
		for(i=0;i<nl.getLength();i++) {
			if(nl.item(i).getNodeName()=="Signature" && nl.item(i).getNamespaceURI()==XMLSignature.XMLNS) {
				break;
			}
		}
		

		// Create a DOMValidateContext and specify a KeySelector
		// and document context.
		DOMValidateContext valContext = new DOMValidateContext
		    (new X509KeySelector(this.publicKey), nl.item(i));

		// Unmarshal the XMLSignature.
		XMLSignature signature = this.signatureFactory.unmarshalXMLSignature(valContext);
		
		
		// Validate the XMLSignature.
		boolean coreValidity = signature.validate(valContext);
		return coreValidity;
	}
	
	public static Document stringToDocument(String str) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder builder = dbf.newDocumentBuilder();
		Document dom = builder.parse(str);
		return dom;
	}
	
	
	//Pro validaci vnorenych dokumentu se Node premeni na Document a ten se validuje
	public static Document nodeToDocument(Node node) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);

		DocumentBuilder builder = factory.newDocumentBuilder();

		Document newDocument = builder.newDocument();
		Node node1=newDocument.importNode(node, true);
		newDocument.appendChild(node1);
		return newDocument;
	}
}
