/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.environmatics.acs.accessor.interfaces;

import net.environmatics.acs.accessor.*;
import java.util.List;
import net.environmatics.acs.exceptions.AuthenticationFailedException;
import net.environmatics.acs.exceptions.ServiceException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.dom4j.Document;

/**
 * Generic interface for WSS interaction
 *
 * @author abonitz
 */
public interface WSSAccessor {

    /** DCP Method: GET */
    public static final String DCP_HTTP_GET = "HTTP_GET";
    /** DCP Method: POST */
    public static final String DCP_HTTP_POST = "HTTP_POST";
    
    /**
     * Closes the WSS session
     *
     * @throws ServiceException Is thrown, when closeSession() fails on the WSS
     */
    public void closeSession() throws ServiceException;

    /**
     * Performs a doService request on the selected WSS.
     *
     * @param dcp_type <b>Must</b> be 
     *      {@link net.environmatics.acs.accessor.interfaces.WSSAccessor#DCP_HTTP_GET HTTP GET} or
     *      {@link net.environmatics.acs.accessor.interfaces.WSSAccessor#DCP_HTTP_POST HTTP POST}
     * @param serviceRequest Request to a secured service
     * @param requestParams An array with request parameters, i.e. HTTP_Header / Mime-Type: text/xml
     * @param facadeURL String with an URL to facade
     * @return Payload, containing the doService response.
     * @throws ServiceException Thrown in case of an error.
     */
    public Payload doService(String dcp_type, String serviceRequest, NameValuePair[] requestParams, String facadeURL) throws ServiceException;

    /**
     * Performs a doService request on the selected WSS. Note that this method
     * sets the WSS doService request parameters to "HTTP_Header" with "Mime-Type: text/xml"
     *
     * @param dcp_type <b>Must</b> be 
     *      {@link net.environmatics.acs.accessor.interfaces.WSSAccessor#DCP_HTTP_GET HTTP GET} or
     *      {@link net.environmatics.acs.accessor.interfaces.WSSAccessor#DCP_HTTP_POST HTTP POST}
     * @param serviceRequest Request to a secured service
     * @param facadeURL String with an URL to facade
     * @return Payload, containing the doService response.
     * @throws ServiceException Thrown in case of an error.
     */
    public Payload doService(String dcp_type, String serviceRequest, String facadeURL) throws ServiceException;

    /**
     * Establishes a session between the WSSAccessor and the remote WSS service.
     *
     * @param authnMethod The authentication method which should be used
     * @throws AuthenticationFailedException Is thrown, when the authentication fails
     */
    public SessionInformation getSession(AuthenticationMethod authnMethod) throws AuthenticationFailedException;

    /**
     * Retrieves the capabilities document of a WSS, parses it and returns
     * the supported auhtentication methods. If the WSS does not respond,
     * the method returns an empty List.
     *
     * @return a List with the IDs of all supported auhtentication methods
     */
    public List<String> getSupportedAuthenticationMethods();

    /**
     * Returns the URL to the used WSS.
     *
     * @return URL as String
     */
    public String getWSS();

    /**
     * Tries to retrieve the capabillities document from the specific WSS
     *
     * @return the capabillities document as Dom4J Document, or in case of an
     * error <code>null</code>
     */
    public Document getWSSCapabilities();

    /**
     * Sets the authentication method for WSS interaction
     *
     * @param authnMethod The authentication method that shall be used.
     */
    public void setAuthenticationMethod(AuthenticationMethod authnMethod);

    /**
     * Sets a proxy for indirect HTTP communication
     *
     * @param proxy_url URL of the proxy to use.
     * @param port The port of the proxy server.
     */
    public void setProxy(String proxy_url, int port);

    public void setCredentialProvider(CredentialsProvider credentialProvider);
    /**
     * Sets the URL of WSS to use
     *
     * @param wss_url URL of an WSS
     */
    public void setWSS(String wssUrl);

}
