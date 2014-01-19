package cz.svobodni.vk.openVote;

import java.io.FileInputStream;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

public class XMLSignService {
	
	private KeyInfo keyInfo;
	private KeyStore.PrivateKeyEntry keyEntry;
	private XMLSignatureFactory signatureFactory;
	
	public XMLSignService(String jksFilename, String jksPassword, String jksEntryName, String jksEntryPassword) throws Exception {
		this.signatureFactory = XMLSignatureFactory.getInstance("DOM");
		KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(jksFilename), jksPassword.toCharArray());
        this.keyEntry =
            (KeyStore.PrivateKeyEntry) ks.getEntry
                (jksEntryName, new KeyStore.PasswordProtection(jksEntryPassword.toCharArray()));
        X509Certificate cert = (X509Certificate) keyEntry.getCertificate();

        // Create the KeyInfo containing the X509Data.
        KeyInfoFactory kif = signatureFactory.getKeyInfoFactory();
        List x509Content = new ArrayList();
        x509Content.add(cert.getSubjectX500Principal().getName());
        x509Content.add(cert);
        X509Data xd = kif.newX509Data(x509Content);
        this.keyInfo = kif.newKeyInfo(Collections.singletonList(xd));
	}
	
	public Document sign(Document dom) throws Exception {
		
		
		Reference ref = this.signatureFactory.newReference
	            ("", this.signatureFactory.newDigestMethod(DigestMethod.SHA1, null),
	             Collections.singletonList
	              (this.signatureFactory.newTransform
	                (Transform.ENVELOPED, (TransformParameterSpec) null)), 
	             null, null);

        // Create the SignedInfo
        SignedInfo si = this.signatureFactory.newSignedInfo
            (this.signatureFactory.newCanonicalizationMethod
             (CanonicalizationMethod.EXCLUSIVE, 
              (C14NMethodParameterSpec) null), 
              this.signatureFactory.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
             Collections.singletonList(ref));
        
        
        DOMSignContext dsc = new DOMSignContext
        	    (this.keyEntry.getPrivateKey(), dom.getDocumentElement());
        

    	// Create the XMLSignature, but don't sign it yet.
    	XMLSignature signature = this.signatureFactory.newXMLSignature(si, this.keyInfo);

    	
    	// Marshal, generate, and sign the enveloped signature.
    	signature.sign(dsc);
    	
    	
    	return dom;
	}
	
	public static String documentToString(Document dom) throws Exception {
		
		
    	DOMSource domSource = new DOMSource(dom);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(domSource, result);
        
        return writer.toString();
	}
}
