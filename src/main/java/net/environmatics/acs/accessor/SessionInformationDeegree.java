/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package net.environmatics.acs.accessor;

import net.environmatics.acs.accessor.interfaces.SessionInformation;

/**
 * Implementation of the interface {@link net.environmatics.acs.accessor.interfaces.SessionInformation} for Deegree
 * WASS.
 *
 * @author   abonitz
 * @version  $Revision$, $Date$
 */
public class SessionInformationDeegree implements SessionInformation {

    //~ Instance fields --------------------------------------------------------

    private String sessionID;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SessionInformationDeegree Object.
     *
     * @param  payload  The response from an WSS
     */
    public SessionInformationDeegree(final Payload payload) {
        sessionID = payload.asText();
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * @see  net.environmatics.acs.accessor.interfaces.SessionInformation#getSessionID()
     */
    @Override
    public String getSessionID() {
        return sessionID;
    }
}
