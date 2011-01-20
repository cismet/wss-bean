/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package net.environmatics.acs.accessor.methods;

import net.environmatics.acs.accessor.interfaces.AuthenticationMethod;
import net.environmatics.acs.accessor.interfaces.SessionInformation;
import net.environmatics.acs.accessor.utils.DOMHelper;

import org.apache.commons.httpclient.NameValuePair;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.QName;

/**
 * Uses a SessionID for authentication.
 *
 * @author   abonitz
 * @version  $Revision$, $Date$
 */
public class SessionAuthenticationMethod implements AuthenticationMethod {

    //~ Static fields/initializers ---------------------------------------------

    // 52North public static final String METHOD_URN = "urn:opengeospatial:authNMethod:OWS:1.0:session";
    /** URN of the authentication method. */
    public static final String METHOD_URN = "urn:x-gdi-nrw:authnMethod:1.0:session";

    //~ Instance fields --------------------------------------------------------

    private String sessionID;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SessionAuthenticationMethod.
     *
     * @param  credentials  A String, containing session information
     */
    public SessionAuthenticationMethod(final String credentials) {
        sessionID = credentials;
    }

    /**
     * Creates a new SessionAuthenticationMethod.
     *
     * @param  sessionInfo  the SessionInformation
     */
    public SessionAuthenticationMethod(final SessionInformation sessionInfo) {
        sessionID = sessionInfo.getSessionID();
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getSessionID() {
        return sessionID;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  sessionID  DOCUMENT ME!
     */
    public void setSessionID(final String sessionID) {
        this.sessionID = sessionID;
    }

    /**
     * Returns the specific POST payload for session authentication to an WSS.
     *
     * @return  payload for POST transfer
     *
     * @see     net.environmatics.acs.accessor.interfaces.AuthenticationMethod#asNameValue()
     */
    @Override
    public NameValuePair[] asNameValue() {
        final NameValuePair[] data = {
                new NameValuePair("AUTHMETHOD", METHOD_URN),
                new NameValuePair("CREDENTIALS", sessionID)
            };

        return data;
    }

    /**
     * @see  net.environmatics.acs.accessor.interfaces.AuthenticationMethod#asText()
     */
    @Override
    public String asText() {
        return "Authentication Method: Session - Method URN: " + METHOD_URN;
    }

    /**
     * @see  net.environmatics.acs.accessor.interfaces.AuthenticationMethod#asDOM4jElement()
     */
    @Override
    public Element asDOM4jElement() {
        final DocumentFactory docFactory = DOMHelper.getDocumentFactory();
        final Element authnData = docFactory.createElement(new QName("AuthenticationData", DOMHelper.NAMESPACE_AUTHEN));

        final Element authnMethod = docFactory.createElement(new QName(
                    "AuthenticationMethod",
                    DOMHelper.NAMESPACE_AUTHEN));
        authnMethod.addAttribute("id", METHOD_URN);

        final Element authnCredentials = docFactory.createElement(new QName("Credentials", DOMHelper.NAMESPACE_AUTHEN));
        authnCredentials.addText(sessionID);

        authnData.add(authnMethod);
        authnData.add(authnCredentials);

        return authnData;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  the {@link AnonymousAuthenticationMethod#METHOD_URN URN} of the authentication method
     */
    @Override
    public String toString() {
        return METHOD_URN;
    }
}
