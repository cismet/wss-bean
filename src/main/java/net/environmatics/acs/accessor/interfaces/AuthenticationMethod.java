package net.environmatics.acs.accessor.interfaces;

import org.apache.commons.httpclient.NameValuePair;
import org.dom4j.Element;

/**
 * Represents a authentication method for authentication
 * on a WSS. This interface defines the basic methods that
 * all AuthenticationMethods have in common.
 * 
 * @author abonitz
 */
public interface AuthenticationMethod {
        
    /**
     * Returns the specific POST payload for authentication
     * to an WSS.
     * 
     * @return payload for POST transfer
     */
    public NameValuePair[] asNameValue();
    
    /**
     * Textual representation of the authentication method
     * 
     * @return String: "Authentication Method: $method - Method URN: $URN"
     */
    public String asText();
    
    /**
     * XML representation of the authentication method as DOM4j Element
     * 
     * @return Element containing all authentication data
     */
    public Element asDOM4jElement();
}
