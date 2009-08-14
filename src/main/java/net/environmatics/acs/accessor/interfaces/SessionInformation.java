package net.environmatics.acs.accessor.interfaces;

/**
 * Defines what a SessionInformation container should look like.
 * 
 * @author abonitz
 */
public interface SessionInformation {

    /**
     * Returns the Session ID of a WAS / WSS Session
     * 
     * @return Session ID
     */
    public String getSessionID();
    
}
