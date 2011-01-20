/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package net.environmatics.acs.accessor.interfaces;

import org.apache.commons.httpclient.NameValuePair;

import org.dom4j.Element;

/**
 * Represents a authentication method for authentication on a WSS. This interface defines the basic methods that all
 * AuthenticationMethods have in common.
 *
 * @author   abonitz
 * @version  $Revision$, $Date$
 */
public interface AuthenticationMethod {

    //~ Methods ----------------------------------------------------------------

    /**
     * Returns the specific POST payload for authentication to an WSS.
     *
     * @return  payload for POST transfer
     */
    NameValuePair[] asNameValue();

    /**
     * Textual representation of the authentication method.
     *
     * @return  String: "Authentication Method: $method - Method URN: $URN"
     */
    String asText();

    /**
     * XML representation of the authentication method as DOM4j Element.
     *
     * @return  Element containing all authentication data
     */
    Element asDOM4jElement();
}
