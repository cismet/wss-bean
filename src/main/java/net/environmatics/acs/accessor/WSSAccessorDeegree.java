/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package net.environmatics.acs.accessor;

import net.environmatics.acs.accessor.interfaces.AuthenticationMethod;
import net.environmatics.acs.accessor.interfaces.SessionInformation;
import net.environmatics.acs.accessor.interfaces.WSSAccessor;
import net.environmatics.acs.accessor.methods.AnonymousAuthenticationMethod;
import net.environmatics.acs.accessor.methods.SessionAuthenticationMethod;
import net.environmatics.acs.accessor.utils.DOMHelper;
import net.environmatics.acs.exceptions.AuthenticationFailedException;
import net.environmatics.acs.exceptions.ServiceException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.log4j.Logger;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Array;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.Lock;
//import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Main Class for Interaction with an deegree WebSecurityService.<br>
 * <br>
 * <i>Note:</i> Uses the anonymous authentication as default<br>
 * <br>
 * <b>Example:</b><code><br>
 * // Creating new accessor instance WSSAccessorDeegree accessor = new WSSAccessorDeegree("http://myservices.com/wss");
 * <br>
 * <br>
 * // Choosing a authentication method<br>
 * PasswordAuthenticationMethod authMethod = new PasswordAuthenticationMethod("user","pass");<br>
 * <br>
 * // Use the auth. method with the accessor<br>
 * accessor.setAuthenticationMethod(authMethod);<br>
 * <br>
 * // Perform a doService Operation on the WSS, in this case with a GetCapabilities<br>
 * // operation on a secured WMS<br>
 * Payload response = accessor.doService("HTTP_GET", "SERVICE=WMS&REQUEST=GetCapabilities",
 * "http://localhost:8080/facadeURL");<br>
 * </code>
 *
 * @author   abonitz
 * @version  $Revision$, $Date$
 */
public class WSSAccessorDeegree implements WSSAccessor {

    //~ Static fields/initializers ---------------------------------------------

    private static Logger logger = Logger.getLogger(WSSAccessorDeegree.class);

    //~ Instance fields --------------------------------------------------------

    ReadWriteLock lock = new ReentrantReadWriteLock();
    private String wss_url;
    private HttpClient client;
    private SessionInformation sessionInfo;
    private AuthenticationMethod authnMethod;
    private String[] supportedAuthnMethods;
    private Document wssCapabilities;
    private boolean isCredentialProviderAvailable = false;
    // private int readLockCounter = 0;
    private SessionAuthenticationMethod currentAuth = null;
    private boolean lastDoServiceFailed = false;
    // TODO entfernen!!! nur zum testen
    // private int testSessionTimeoutCounter = 0;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new instance of the WSSAccessorDeegree.
     */
    public WSSAccessorDeegree() {
        if (logger.isDebugEnabled()) {
            logger.debug("WSS AccessorDeegree()");
        }
        client = new HttpClient(new MultiThreadedHttpConnectionManager());

        this.wss_url = null;
        this.supportedAuthnMethods = null;
        this.sessionInfo = null;
        this.authnMethod = new AnonymousAuthenticationMethod();
    }

    /**
     * Creates a new instance of the WSSAccessorDeegree.
     *
     * @param  wssURL  URL to a WSS
     */
    public WSSAccessorDeegree(final String wssURL) {
        this();
        if (logger.isDebugEnabled()) {
            logger.debug("WSS AccessorDeegree(wssUrl)");
        }
        setWSS(wssURL);
    }

    /**
     * Creates a new instance of the WSSAccessorDeegree.
     *
     * @param  wssURL    URL to a WSS
     * @param  proxyURL  URL to a proxy server
     * @param  port      The port of the proxy server
     */
    public WSSAccessorDeegree(final String wssURL, final String proxyURL, final int port) {
        this(wssURL);

        setProxy(proxyURL, port);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Performs a doService request on the selected WSS.
     *
     * @param   dcp_type        <b>Must</b> be
     *                          {@link net.environmatics.acs.accessor.interfaces.WSSAccessor#DCP_HTTP_GET HTTP GET} or
     *                          {@link net.environmatics.acs.accessor.interfaces.WSSAccessor#DCP_HTTP_POST HTTP POST}
     * @param   serviceRequest  Request to a secured service
     * @param   requestParams   An array with request parameters, i.e. HTTP_Header / Mime-Type: text/xml
     * @param   facadeURL       String with an URL to facade
     *
     * @return  Payload, containing the doService response.
     *
     * @throws  ServiceException      Thrown in case of an error.
     * @throws  NullPointerException  DOCUMENT ME!
     */
    @Override
    public synchronized Payload doService(final String dcp_type,
            final String serviceRequest,
            final NameValuePair[] requestParams,
            final String facadeURL) throws ServiceException {
        if (logger.isDebugEnabled()) {
            logger.debug("service request: " + serviceRequest + " facade url: " + facadeURL);
        }
        if (wss_url == null) {
            throw new NullPointerException("wss_url is not initialized");
        }

        try {
            if (currentAuth == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("not yet authed => call newSession()");
                }
                newSession();
            }

            // TODO Achtung schauen ob get überhaupt funktioniert
            String postMethod = wss_url;
            if (dcp_type.equals(DCP_HTTP_GET)) {
                postMethod = postMethod + "?";
            }
            final PostMethod post = new PostMethod(postMethod);
            if (isCredentialProviderAvailable) {
                post.setDoAuthentication(true);
            }

            Payload doServiceResponse = null;
            // int readlockID = new Random().nextInt();
            try {
//                logger.debug("deadlock:read lock: "+readlockID+" counter: " + readLockCounter);
//                lock.readLock().lock();
//                readLockCounter++;
//                logger.debug("deadlock:read lock done"+readlockID+" counter: " +readLockCounter);

                // TODO entfernen!!! nur zum testen!
// if (testSessionTimeoutCounter++ >= 5) {
// testSessionTimeoutCounter = 0;
// SessionInformation sesInfo = new SessionInformation() {
//
// @Override
// public String getSessionID() {
// return "ID0000-0.000000000000000";
// }
// };
// currentAuth = new SessionAuthenticationMethod(sesInfo);
// } //TODO entfernen!!! nur zum testen!

                final String wssRequest = serviceRequest + "&sessionID=" + currentAuth.getSessionID();
                if (logger.isDebugEnabled()) {
                    logger.debug("WSS request: " + wssRequest.toString());
                }
                final Document request = DOMHelper.generateDoService(
                        dcp_type,
                        wssRequest,
                        currentAuth,
                        requestParams,
                        facadeURL);

                post.setRequestEntity(new StringRequestEntity(request.asXML(), "text/xml", "UTF-8"));
                if (logger.isDebugEnabled()) {
                    logger.debug("sending WSS request: " + request.asXML());
                }
                client.executeMethod(post);
                if (logger.isDebugEnabled()) {
                    logger.debug("getResponseBody: " + post.getResponseBody().toString());
                }
                if (logger.isDebugEnabled()) {
                    // post.getResponseBodyAsString();
                    logger.debug("ResponseCharset: " + post.getResponseCharSet());
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("ContentLength: " + post.getResponseContentLength());
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("getStatusCode: " + post.getStatusCode());
                }

                final Header[] header = post.getResponseHeaders();
                if (header != null) {
                    for (final Header current : header) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Response Header: " + current.getName() + " value: " + current.getValue());
                        }
                    }
                }
                final Header[] footer = post.getResponseFooters();
                if (footer != null) {
                    for (final Header current : footer) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Response footer: " + current.getName() + " value: " + current.getValue());
                        }
                    }
                }
                if (logger.isDebugEnabled()) {
                    // post.getResponseBody();
                    logger.debug("Befor Payload object creation");
                }
                doServiceResponse = new Payload(post.getResponseBody(), post.getResponseCharSet());
                if (logger.isDebugEnabled()) {
                    logger.debug("After Payload object creation");
                }

                if (doServiceResponse.containsException()) {
                    if (!lastDoServiceFailed) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("doRequest failed first time => call newSession() and try again.");
                        }
                        newSession();
                        lastDoServiceFailed = true;
                        return doService(dcp_type, serviceRequest, requestParams, facadeURL);
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("doRequest failed second time in a row => give up (throw Exception).");
                        }
                        lastDoServiceFailed = false;
                        throw new ServiceException(doServiceResponse.asText());
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("doRequest not failed.");
                    }
                    lastDoServiceFailed = false;
                }
                if (logger.isDebugEnabled()) {
                    logger.debug(doServiceResponse.asText());
                }
                return doServiceResponse;
            } catch (IOException rethrow) {
                logger.error("Error in doService(). Exception: " + rethrow);
                throw rethrow;
//            } finally {
//                logger.debug("deadlock:read unlock: "+readlockID+" counter: " + readLockCounter);
//                readLockCounter--;
//                lock.readLock().unlock();
//                logger.debug("deadlock:read unlock done"+readlockID+" counter: " +readLockCounter);
            }
        } catch (IOException ex) {
            logger.error("Could not perform doService(). Exception: " + ex);
            throw new ServiceException(ex);
//        } finally {
//            logger.debug("deadlock:read unlock" + readLockCounter);
//            readLockCounter--;
//            lock.readLock().unlock();
//            logger.debug("deadlock:read unlock done" + readLockCounter);
        }
    }

    /**
     * Starts a new session with the authentication metod that was set before. This method is first trying to close the
     * current session
     *
     * @throws  ServiceException  Thrown in case of an error.
     */
    private synchronized void newSession() throws ServiceException {
        if (logger.isDebugEnabled()) {
            logger.debug("newSession()");
        }

//        try {
//            logger.debug("deadlock:write lock" + readLockCounter);
//            lock.writeLock().lock();
//            logger.debug("deadlock:write lock done" + readLockCounter);
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("first close current session");
            }
            closeSession();
        } catch (ServiceException ex) {
            logger.warn("Failure while closing session", ex);
        }
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("authenticate => calling getSession(authnMethod)");
            }
            final SessionInformation sInfo = getSession(authnMethod);
            currentAuth = new SessionAuthenticationMethod(sInfo);
        } catch (AuthenticationFailedException ex) {
            currentAuth = null;
            logger.error("Authentication failed couldn't aquire session id: ", ex);
            throw new ServiceException(ex);
//            } finally {
//            logger.debug("deadlock:write unlock" + readLockCounter);
//            lock.writeLock().unlock();
//            logger.debug("deadlock:write unlock done" + readLockCounter);
        }
//        } finally {
//            logger.debug("deadlock:write unlock" + readLockCounter);
//            lock.writeLock().unlock();
//            logger.debug("deadlock:write unlock done" + readLockCounter);
//        }
    }

    /**
     * Performs a doService request on the selected WSS. Note that this method sets the WSS doService request parameters
     * to "HTTP_Header" with "Mime-Type: text/xml"
     *
     * @param   dcp_type        <b>Must</b> be
     *                          {@link net.environmatics.acs.accessor.interfaces.WSSAccessor#DCP_HTTP_GET HTTP GET} or
     *                          {@link net.environmatics.acs.accessor.interfaces.WSSAccessor#DCP_HTTP_POST HTTP POST}
     * @param   serviceRequest  Request to a secured service
     * @param   facadeURL       String with an URL to facade
     *
     * @return  Payload, containing the doService response.
     *
     * @throws  ServiceException  Thrown in case of an error.
     */
    @Override
    public Payload doService(final String dcp_type, final String serviceRequest, final String facadeURL)
            throws ServiceException {
        return doService(
                dcp_type,
                serviceRequest,
                new NameValuePair[] { new NameValuePair("HTTP_Header", "Mime-Type: text/xml") },
                facadeURL);
    }

    /**
     * Establishes a session between the WSSAccessor and the remote WSS service.
     *
     * @param   authnMethod  The authentication method which should be used
     *
     * @return  DOCUMENT ME!
     *
     * @throws  AuthenticationFailedException  Is thrown, when the authentication fails
     * @throws  NullPointerException           DOCUMENT ME!
     */
    @Override
    public SessionInformation getSession(final AuthenticationMethod authnMethod) throws AuthenticationFailedException {
        if (logger.isDebugEnabled()) {
            logger.debug("getSession()");
        }
        if (wss_url == null) {
            throw new NullPointerException("wss_url is not initialized");
        }

        this.authnMethod = authnMethod;

        try {
            logger.info("getSession() with " + authnMethod.asText());

            final PostMethod post = new PostMethod(wss_url + "?");
            if (isCredentialProviderAvailable) {
                post.setDoAuthentication(true);
            }
            final NameValuePair[] body = arrayMerge(
                    authnMethod.asNameValue(),
                    new NameValuePair[] {
                        new NameValuePair("SERVICE", "WSS"),
                        new NameValuePair("VERSION", "1.0"),
                        new NameValuePair("REQUEST", "GetSession")
                    });

            post.setRequestBody(body);
            client.executeMethod(post);

            final Payload getSessionResponse = new Payload(post.getResponseBodyAsStream(), post.getResponseCharSet());

            if (getSessionResponse.containsException()) {
                throw new AuthenticationFailedException(getSessionResponse.asText());
            }

            sessionInfo = new SessionInformationDeegree(getSessionResponse);
            logger.info("New Session with SessionID=" + sessionInfo.getSessionID() + " length: "
                        + sessionInfo.getSessionID().length());

            if ((sessionInfo.getSessionID() == null) || (sessionInfo.getSessionID().length() == 0)
                        || (sessionInfo.getSessionID().length() == 2)) {
                throw new AuthenticationFailedException("SessionID is null or equals  \"\"");
            }
            return sessionInfo;
        } catch (IOException ioex) {
            logger.info("Could not perform getSession(). Exception: " + ioex);
            throw new AuthenticationFailedException(ioex);
        }
    }

    /**
     * Establishes a session between the WSSAccessor and the remote WSS service.
     *
     * @return  DOCUMENT ME!
     *
     * @throws  AuthenticationFailedException  Is thrown, when the authentication fails
     * @throws  NullPointerException           DOCUMENT ME!
     */
    public SessionInformation getSession() throws AuthenticationFailedException {
        if (logger.isDebugEnabled()) {
            logger.debug("getSession() ");
        }
        if (wss_url == null) {
            throw new NullPointerException("wss_url is not initialized");
        }

        // AuthenticationMethod tmpAuth = authnMethod;

        try {
            logger.info("getSession() with " + authnMethod.asText());

            final PostMethod post = new PostMethod(wss_url + "?");
            if (isCredentialProviderAvailable) {
                post.setDoAuthentication(true);
            }
            final NameValuePair[] body = arrayMerge(
                    authnMethod.asNameValue(),
                    new NameValuePair[] {
                        new NameValuePair("SERVICE", "WSS"),
                        new NameValuePair("VERSION", "1.0"),
                        new NameValuePair("REQUEST", "GetSession")
                    });

            post.setRequestBody(body);
            final HttpClient tmpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
            tmpClient.executeMethod(post);

            final Payload getSessionResponse = new Payload(post.getResponseBodyAsStream(), post.getResponseCharSet());

            if (getSessionResponse.containsException()) {
                throw new AuthenticationFailedException(getSessionResponse.asText());
            }

            final SessionInformation tmpSI = new SessionInformationDeegree(getSessionResponse);
            logger.info("New Session with SessionID=" + tmpSI.getSessionID() + " length: "
                        + tmpSI.getSessionID().length());

            if ((tmpSI.getSessionID() == null) || (tmpSI.getSessionID().length() == 0)
                        || (tmpSI.getSessionID().length() == 2)) {
                throw new AuthenticationFailedException("SessionID is null or equals  \"\"");
            }
            return tmpSI;
        } catch (IOException ioex) {
            logger.info("Could not perform getSession(). Exception: " + ioex);
            throw new AuthenticationFailedException(ioex);
        }
    }

    /**
     * Closes the WSS session.
     *
     * @throws  ServiceException      Is thrown, when closeSession() fails on the WSS
     * @throws  NullPointerException  DOCUMENT ME!
     */
    @Override
    public void closeSession() throws ServiceException {
        if (wss_url == null) {
            throw new NullPointerException("wss_url is not initialized");
        }

        try {
            final PostMethod post = new PostMethod(wss_url + "?");
            if (isCredentialProviderAvailable) {
                post.setDoAuthentication(true);
            }
            final NameValuePair[] data = {
                    new NameValuePair("SERVICE", "WSS"),
                    new NameValuePair("REQUEST", "CloseSession"),
                    new NameValuePair("SESSIONID", sessionInfo.getSessionID())
                };

            post.setRequestBody(data);
            client.executeMethod(post);

            final Payload closeSessionResponse = new Payload(post.getResponseBodyAsStream(), post.getResponseCharSet());

            if (closeSessionResponse.containsException()) {
                logger.error(closeSessionResponse.asText());
                throw new ServiceException(closeSessionResponse.asText());
            }
        } catch (IOException ex) {
            logger.info("Could not perform closeSession(). Exception: " + ex);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("closeSession() called successfully");
        }
    }

    /**
     * Closes the WSS session.
     *
     * @param   si  DOCUMENT ME!
     *
     * @throws  ServiceException      Is thrown, when closeSession() fails on the WSS
     * @throws  NullPointerException  DOCUMENT ME!
     */
    public void closeSession(final SessionInformation si) throws ServiceException {
        if (wss_url == null) {
            throw new NullPointerException("wss_url is not initialized");
        }

        try {
            final PostMethod post = new PostMethod(wss_url + "?");
            if (isCredentialProviderAvailable) {
                post.setDoAuthentication(true);
            }
            final NameValuePair[] data = {
                    new NameValuePair("SERVICE", "WSS"),
                    new NameValuePair("REQUEST", "CloseSession"),
                    new NameValuePair("SESSIONID", si.getSessionID())
                };

            post.setRequestBody(data);
            client.executeMethod(post);

            final Payload closeSessionResponse = new Payload(post.getResponseBodyAsStream(), post.getResponseCharSet());

            if (closeSessionResponse.containsException()) {
                logger.error(closeSessionResponse.asText());
                throw new ServiceException(closeSessionResponse.asText());
            }
        } catch (IOException ex) {
            logger.info("Could not perform closeSession(). Exception: " + ex);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("closeSession() called successfully");
        }
    }

    /**
     * Returns the URL to the used WSS.
     *
     * @return  URL as String
     */
    @Override
    public String getWSS() {
        return wss_url.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public URL getWSSURL() {
        try {
            return new URL(wss_url);
        } catch (MalformedURLException ex) {
            return null;
        }
    }

    /**
     * Sets a proxy for indirect HTTP communication.
     *
     * @param  proxy_url  URL of the proxy to use.
     * @param  port       The port of the proxy server.
     */
    @Override
    public void setProxy(final String proxy_url, final int port) {
        if (proxy_url == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("make new httpclient without proxy");
            }
            client = new HttpClient(new MultiThreadedHttpConnectionManager());
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("set accessor proxy: " + proxy_url + ":" + port);
            }
            client.getHostConfiguration().setProxy(proxy_url, port);
        }
    }

    @Override
    public void setCredentialProvider(final CredentialsProvider credentialProvider) {
        client.getParams().setParameter(CredentialsProvider.PROVIDER, credentialProvider);
        if (credentialProvider != null) {
            isCredentialProviderAvailable = true;
        } else {
            isCredentialProviderAvailable = false;
        }
    }

    /**
     * Sets the URL of WSS to use.
     *
     * @param  wssUrl  URL of an WSS
     */
    @Override
    public void setWSS(final String wssUrl) {
        if (logger.isDebugEnabled()) {
            logger.debug("Using WSS: " + wssUrl);
        }
        this.wss_url = wssUrl;

        try {
            final URL wssURL = new URL(wssUrl);

            // Cancel if HTTPS is not used
            if (!wssURL.getProtocol().equals("https")) {
                return;
            }
            final Protocol easyHTTPS = new Protocol(
                    "https",
                    (ProtocolSocketFactory)new EasySSLProtocolSocketFactory(),
                    wssURL.getPort());
            Protocol.registerProtocol("https", easyHTTPS);

            this.supportedAuthnMethods = null;
            this.sessionInfo = null;
        } catch (MalformedURLException ex) {
            logger.error("URL " + wss_url + " is malformed");
        }
    }

    /**
     * Sets the authentication method for WSS interaction.
     *
     * @param  authnMethod  The authentication method that shall be used.
     */
    @Override
    public void setAuthenticationMethod(final AuthenticationMethod authnMethod) {
        this.authnMethod = authnMethod;
    }

    /**
     * Retrieves the capabilities document of a WSS, parses it and returns the supported auhtentication methods. If the
     * WSS does not respond, the method returns an empty List.
     *
     * @return  a List with the IDs of all supported auhtentication methods
     *
     * @throws  NullPointerException  DOCUMENT ME!
     */
    @Override
    public List<String> getSupportedAuthenticationMethods() {
        if (wss_url == null) {
            throw new NullPointerException("wss_url is not initialized");
        }

        final List<String> authMethodsList = new ArrayList<String>();
        final Document capabilities = getWSSCapabilities();

        // if there's no valid capabilities document, return an empty list
        if (capabilities == null) {
            return authMethodsList;
        }
        // Problem Arndt
        final ListIterator<Element> it = capabilities.selectNodes("//authn:SupportedAuthenticationMethod")
                    .listIterator();

        while (it.hasNext()) {
            final Element e = it.next();
            authMethodsList.add(e.valueOf("//authn:AuthenticationMethod/@id"));
            e.detach();
        }

        return authMethodsList;
    }

    /**
     * Tries to retrieve the capabillities document from the specific WSS.
     *
     * @return  the capabillities document as Dom4J Document, or in case of an error <code>null</code>
     *
     * @throws  NullPointerException  DOCUMENT ME!
     */
    @Override
    public Document getWSSCapabilities() {
        if (logger.isDebugEnabled()) {
            logger.debug("Initial retrieval of WSS Capabilites");
        }
        if (wss_url == null) {
            throw new NullPointerException("wss_url is not initialized");
        }

        try {
            final GetMethod get = new GetMethod(wss_url + "?SERVICE=WSS&REQUEST=GetCapabilities");
            if (isCredentialProviderAvailable) {
                get.setDoAuthentication(true);
            }
            client.executeMethod(get);
            return getXMLDocumentFromStream(get.getResponseBodyAsStream());
        } catch (DocumentException dex) {
            logger.info("Could not parse capabilities document. Exception: " + dex);
        } catch (IOException ex) {
            logger.info("Could not perform getCapabilities(). Exception: " + ex.getClass());
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getSecuredServiceType() {
        if (wssCapabilities == null) {
            wssCapabilities = getWSSCapabilities();
        }
        if (wssCapabilities != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("wssCaps != null");
            }
            final Element root = wssCapabilities.getRootElement();
            if (root != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("root Element != null");
                }
                final Element caps = root.element("Capability");
                if (caps != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Caps Element != null");
                    }
                    final Element securedServiceType = caps.element("SecuredServiceType");
                    if (securedServiceType != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("SecuredServiceType Element != null");
                        }
                        return securedServiceType.getText();
                    }
                }
            }
        }
        logger.warn("It was not possible to determine the secured service type");
        return null;
    }

    /**
     * Tries to create a Dom4J Document from a InputStream.
     *
     * @param   is  DOCUMENT ME!
     *
     * @return  a XML Document
     *
     * @throws  DocumentException  when parsing went wrong
     */
    private Document getXMLDocumentFromStream(final InputStream is) throws DocumentException {
        final SAXReader reader = new SAXReader();
        final Document doc = reader.read(is);
        return doc;
    }

    /**
     * Merges two arrays into a new one.
     *
     * @param   <TYPE>  DOCUMENT ME!
     * @param   arrayA  The first array.
     * @param   arrayB  The Second array
     *
     * @return  a new Array
     */
    private static <TYPE> TYPE[] arrayMerge(final TYPE[] arrayA, final TYPE[] arrayB) {
        final int size = arrayA.length + arrayB.length;

        // creating a new array with generics ...
        final TYPE[] mergedArray = (TYPE[])Array.newInstance(arrayA.getClass().getComponentType(), size);

        // Copy the two arrays into a new one
        System.arraycopy(arrayA, 0, mergedArray, 0, arrayA.length);
        System.arraycopy(arrayB, 0, mergedArray, arrayA.length, arrayB.length);

        return (TYPE[])mergedArray;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isSessionAvailable() {
        if (sessionInfo != null) {
            return true;
        } else {
            return false;
        }
    }
}
