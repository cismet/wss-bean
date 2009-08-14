package net.environmatics.acs.accessor;

import net.environmatics.acs.accessor.interfaces.SessionInformation;

/**
 * Implementation of the interface 
 * {@link net.environmatics.acs.accessor.interfaces.SessionInformation} for 
 * Deegree WASS
 * 
 * @author abonitz
 */
public class SessionInformationDeegree implements SessionInformation {

    
    private String sessionID;
    
     /**
     * Creates a new SessionInformationDeegree Object.
     * 
     * @param payload The response from an WSS
     */
    public SessionInformationDeegree(Payload payload) {
       sessionID = payload.asText();
    }
    
    /**
     * @see net.environmatics.acs.accessor.interfaces.SessionInformation#getSessionID()
     */
    public String getSessionID() {
        return sessionID;
    }
    

}
