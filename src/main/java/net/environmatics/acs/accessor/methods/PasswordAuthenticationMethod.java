/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.environmatics.acs.accessor.methods;

import net.environmatics.acs.accessor.interfaces.AuthenticationMethod;
import net.environmatics.acs.accessor.utils.DOMHelper;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.log4j.Logger;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.QName;

/**
 * A password authentication method. Uses a an username + password for authentication.
 *
 * @author   abonitz
 * @version  $Revision$, $Date$
 */
public class PasswordAuthenticationMethod implements AuthenticationMethod {

    //~ Static fields/initializers ---------------------------------------------

    // 52North public static final String METHOD_URN = "urn:opengeospatial:authNMethod:OWS:1.0:password";
    /** URN of the authentication method. */
    public static final String METHOD_URN = "urn:x-gdi-nrw:authnMethod:1.0:password";

    //~ Instance fields --------------------------------------------------------

    private String credentials;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new PasswordAuthenticationMethod.
     *
     * @param  credentials  - Must contain username and password, seperated by a comma, e.g. "myUser,myPass"
     */
    public PasswordAuthenticationMethod(final String credentials) {
        this.credentials = credentials;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Returns the specific POST payload for password authentication to an WSS.
     *
     * @return  payload for POST transfer
     *
     * @see     net.environmatics.acs.accessor.interfaces.AuthenticationMethod#asNameValue()
     */
    @Override
    public NameValuePair[] asNameValue() {
        final NameValuePair[] data = {
                new NameValuePair("AUTHMETHOD", METHOD_URN),
                new NameValuePair("CREDENTIALS", credentials)
            };

        return data;
    }

    /**
     * @see  net.environmatics.acs.accessor.interfaces.AuthenticationMethod#asText()
     */
    @Override
    public String asText() {
        return "Authentication Method: Password - Method URN: " + METHOD_URN;
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
        authnCredentials.addText(credentials);

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
