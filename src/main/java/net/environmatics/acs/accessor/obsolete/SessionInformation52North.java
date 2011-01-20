/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package net.environmatics.acs.accessor.obsolete;

import net.environmatics.acs.accessor.interfaces.SessionInformation;
import net.environmatics.acs.exceptions.AuthenticationFailedException;

import org.apache.log4j.Logger;

import org.dom4j.Document;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;

/**
 * Represents SessionInfo. Contains the SessionID and the expiration date of the session. <b>Cannot be used with
 * Deegree</b>
 *
 * @author   abonitz
 * @version  $Revision$, $Date$
 */
@Deprecated
public class SessionInformation52North implements SessionInformation {

    //~ Static fields/initializers ---------------------------------------------

    private static Logger logger = Logger.getLogger(SessionInformation52North.class);

    //~ Instance fields --------------------------------------------------------

    private String sessionID;
    private Date expirationDate;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SessionInformation52North Object from a getSession()-Response. This constructor parses the response
     * and throws an exception when the authorisation has failed or an error has occured.
     *
     * @param   getSessionResponse  DOCUMENT ME!
     *
     * @throws  AuthenticationFailedException  net.environmatics.acs.exceptions.AuthenticationFailedException
     */
    public SessionInformation52North(final Document getSessionResponse) throws AuthenticationFailedException {
        try {
            // Try to get session ID
            sessionID = getSessionResponse.valueOf("//Session/@id");
            sessionID = sessionID.trim();

            // Try to get expiration date
            final String date = getSessionResponse.valueOf("//Session/@expirationDate");

            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            expirationDate = dateFormat.parse(date.trim());
            if (logger.isDebugEnabled()) {
                logger.debug("Session ID: " + sessionID + " - Expiration Date: " + date);
            }
        } catch (ParseException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Parsing error:" + ex);
            }
            throw new AuthenticationFailedException(getSessionResponse.asXML());
        }
    }

    /**
     * Creates a new SessionInformation52North Object.
     *
     * @param  sessionID       DOCUMENT ME!
     * @param  expirationDate  DOCUMENT ME!
     */
    public SessionInformation52North(final String sessionID, final Date expirationDate) {
        this.sessionID = sessionID;
        this.expirationDate = expirationDate;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Returns the date when the session ID will expire.
     *
     * @return  expiration date
     */
    public Date getExpirationDate() {
        return expirationDate;
    }

    /**
     * Checks if the session ID is expired. Warning: Uses System Time!
     *
     * @return  <code>true</code> if expired
     */
    public boolean isExpired() {
        // TODO update?
        return new Date().after(expirationDate);
    }

    /**
     * Returns the stored session ID.
     *
     * @return  session ID
     */
    @Override
    public String getSessionID() {
        return sessionID;
    }
}
