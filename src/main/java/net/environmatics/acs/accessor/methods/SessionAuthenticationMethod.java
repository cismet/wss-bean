package net.environmatics.acs.accessor.methods;

import net.environmatics.acs.accessor.interfaces.AuthenticationMethod;
import net.environmatics.acs.accessor.utils.DOMHelper;
import net.environmatics.acs.accessor.interfaces.SessionInformation;
import org.apache.commons.httpclient.NameValuePair;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.QName;

/**
 * Uses a SessionID for authentication.
 * 
 * @author abonitz
 */
public class SessionAuthenticationMethod implements AuthenticationMethod {

    //52North public static final String METHOD_URN = "urn:opengeospatial:authNMethod:OWS:1.0:session";
    /** URN of the authentication method */
    public static final String METHOD_URN = "urn:x-gdi-nrw:authnMethod:1.0:session";
    
    private String sessionID;

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    /**
     * Creates a new SessionAuthenticationMethod 
     * 
     * @param credentials A String, containing session information
     */
    public SessionAuthenticationMethod(String credentials) {
        sessionID = credentials;
    }
    
    
    /**
     * Creates a new SessionAuthenticationMethod 
     * 
     * @param the SessionInformation
     */
    public SessionAuthenticationMethod(SessionInformation sessionInfo) {
        sessionID = sessionInfo.getSessionID();
    }
    

    /**
     * Returns the specific POST payload for session authentication
     * to an WSS.
     * 
     * @return payload for POST transfer
     * @see net.environmatics.acs.accessor.interfaces.AuthenticationMethod#asNameValue()
     */
    public NameValuePair[] asNameValue() {
        
        NameValuePair[] data = {
          new NameValuePair("AUTHMETHOD", METHOD_URN),
          new NameValuePair("CREDENTIALS", sessionID)
        };
                
        return data;
    }

    /**
     * @see net.environmatics.acs.accessor.interfaces.AuthenticationMethod#asText()
     */
    public String asText() {
        return "Authentication Method: Session - Method URN: " + METHOD_URN;
    }
    
    /**
     * @see net.environmatics.acs.accessor.interfaces.AuthenticationMethod#asDOM4jElement()
     */
    public Element asDOM4jElement() {
        
        DocumentFactory docFactory = DOMHelper.getDocumentFactory();
        Element authnData = docFactory.createElement(new QName("AuthenticationData", DOMHelper.NAMESPACE_AUTHEN));

        Element authnMethod = docFactory.createElement(new QName("AuthenticationMethod", DOMHelper.NAMESPACE_AUTHEN));
        authnMethod.addAttribute("id", METHOD_URN);

        Element authnCredentials = docFactory.createElement(new QName("Credentials", DOMHelper.NAMESPACE_AUTHEN));
        authnCredentials.addText(sessionID);

        authnData.add(authnMethod);
        authnData.add(authnCredentials);
        
        return authnData;
    }
    
     /**
     * @return the {@link AnonymousAuthenticationMethod#METHOD_URN URN} of the 
     *      authentication method
     */
    @Override
    public String toString() {
        return METHOD_URN;
    }
}
