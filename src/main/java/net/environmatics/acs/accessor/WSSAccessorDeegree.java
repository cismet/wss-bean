package net.environmatics.acs.accessor;

import java.net.MalformedURLException;
import java.util.concurrent.locks.Lock;
import net.environmatics.acs.accessor.utils.DOMHelper;
import net.environmatics.acs.accessor.interfaces.WSSAccessor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.environmatics.acs.accessor.interfaces.AuthenticationMethod;
import net.environmatics.acs.accessor.interfaces.SessionInformation;
import net.environmatics.acs.accessor.methods.AnonymousAuthenticationMethod;
import net.environmatics.acs.accessor.methods.SessionAuthenticationMethod;
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

/**
 * Main Class for Interaction with an deegree WebSecurityService. <br>
 * <br>
 * <i>Note:</i> Uses the anonymous authentication as default<br><br>
 * <b>Example:</b><code><br>
 *  // Creating new accessor instance
 *  WSSAccessorDeegree accessor = new WSSAccessorDeegree("http://myservices.com/wss");<br>
 *  <br>    
 *  // Choosing a authentication method<br>
 *  PasswordAuthenticationMethod authMethod = new PasswordAuthenticationMethod("user","pass");<br>
 *  <br>
 *  // Use the auth. method with the accessor<br>
 *  accessor.setAuthenticationMethod(authMethod);<br>
 *  <br>
 *  // Perform a doService Operation on the WSS, in this case with a GetCapabilities <br>
 *  // operation on a secured WMS<br>
 *  Payload response = accessor.doService("HTTP_GET", "SERVICE=WMS&REQUEST=GetCapabilities", "http://localhost:8080/facadeURL");<br>
 * </code>
 * 
 * @author abonitz
 */
public class WSSAccessorDeegree implements WSSAccessor {

    private static Logger logger = Logger.getLogger(WSSAccessorDeegree.class);
    private String wss_url;
    private HttpClient client;
    private SessionInformation sessionInfo;
    private AuthenticationMethod authnMethod;
    private String[] supportedAuthnMethods;
    private Document wssCapabilities;
    private boolean isCredentialProviderAvailable = false;
    ReadWriteLock lock = new ReentrantReadWriteLock();
    private SessionAuthenticationMethod currentAuth = null;

    /**
     * Creates a new instance of the WSSAccessorDeegree
     * 
     * @param wssURL URL to a WSS
     */
    public WSSAccessorDeegree() {
        logger.debug("WSS AccessorDeegree()");
        client = new HttpClient(new MultiThreadedHttpConnectionManager());

        this.wss_url = null;
        this.supportedAuthnMethods = null;
        this.sessionInfo = null;
        this.authnMethod = new AnonymousAuthenticationMethod();
    }

    /**
     * Creates a new instance of the WSSAccessorDeegree
     * 
     * @param wssURL URL to a WSS
     */
    public WSSAccessorDeegree(String wssURL) {
        this();
        logger.debug("WSS AccessorDeegree(wssUrl)");
        setWSS(wssURL);
    }

    /**
     * Creates a new instance of the WSSAccessorDeegree
     * 
     * @param wssURL URL to a WSS
     * @param proxyURL URL to a proxy server
     * @param port The port of the proxy server 
     */
    public WSSAccessorDeegree(String wssURL, String proxyURL, int port) {
        this(wssURL);

        setProxy(proxyURL, port);
    }

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
    public Payload doService(String dcp_type, String serviceRequest, NameValuePair[] requestParams, String facadeURL) throws ServiceException {
        logger.debug("service request: " + serviceRequest + " facade url: " + facadeURL);
        if (wss_url == null) {
            throw new NullPointerException("wss_url is not initialized");
        }

        try {
            //TODO Achtung schauen ob get überhaupt funktioniert 
            String postMethod = wss_url;
            if (dcp_type.equals(DCP_HTTP_GET)) {
                postMethod = postMethod + "?";
            }
            PostMethod post = new PostMethod(postMethod);
            if (isCredentialProviderAvailable) {
                post.setDoAuthentication(true);
            }
            // TODO: This shalt be changed by session if applicable 
            //compability change for Wuppertal at the moment there is no username/password access therefore every request 
            //gets a new session id
            //first close the
            logger.fatal("Wuppertal Kompatibilitätsmodus --> Immer neue Session bei jedem Request");


            if (lock.writeLock().tryLock()) {
                try {
                    closeSession();
                    SessionInformation sInfo = null;
                    try {
                        sInfo = getSession(authnMethod);
                    } catch (AuthenticationFailedException ex) {
                        logger.error("Authentication failed couldn't aquire session id: ", ex);
                        lock.readLock().unlock();
                        throw new ServiceException(ex);
                    }
                    currentAuth = new SessionAuthenticationMethod(sInfo);
                } catch (Exception ex) {
                    logger.warn("Failure while closing session", ex);

                } finally {
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            } else {
                lock.readLock().lock();
                logger.debug("couldn't aquire writelock --> somebody is using the session aquire readlock (session not closed)");
            }
            //ToDo            
            serviceRequest+="&sessionID="+currentAuth.getSessionID();
            logger.debug("WSS request: "+serviceRequest.toString());
            Document request = DOMHelper.generateDoService(dcp_type, serviceRequest, currentAuth, requestParams, facadeURL);

            post.setRequestEntity(new StringRequestEntity(request.asXML(), "text/xml", "UTF-8"));
            logger.debug("sending WSS request: " + request.asXML());    
            client.executeMethod(post);

            //post.getResponseBodyAsString();
            logger.debug("ResponseCharset" + post.getResponseCharSet());
            logger.debug("ContentLength" + post.getResponseContentLength());

            Header[] header = post.getResponseHeaders();
            if (header != null) {
                for (Header current : header) {
                    logger.debug("Response Header: " + current.getName() + " value: " + current.getValue());
                }
            }
            Header[] footer = post.getResponseFooters();
            if (footer != null) {
                for (Header current : footer) {
                    logger.debug("Response footer: " + current.getName() + " value: " + current.getValue());
                }
            }

            //post.getResponseBody();
            logger.debug("Befor Payload object creation");
            Payload doServiceResponse = new Payload(post.getResponseBody(), post.getResponseCharSet());
            logger.debug("After Payload object creation");
            lock.readLock().unlock();
            if (doServiceResponse.containsException()) {
                throw new ServiceException(doServiceResponse.asText());
            }

            return doServiceResponse;

        } catch (IOException ex) {
            logger.error("Could not perform doService(). Exception: " + ex);
            lock.readLock().unlock();
            throw new ServiceException(ex);
        }
    }

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
    public Payload doService(String dcp_type, String serviceRequest, String facadeURL) throws ServiceException {

        return doService(
                dcp_type,
                serviceRequest,
                new NameValuePair[]{new NameValuePair("HTTP_Header", "Mime-Type: text/xml")},
                facadeURL);
    }

    /**
     * Establishes a session between the WSSAccessor and the remote WSS service.
     * 
     * @param authnMethod The authentication method which should be used
     * @throws AuthenticationFailedException Is thrown, when the authentication fails
     */
    public SessionInformation getSession(AuthenticationMethod authnMethod)
            throws AuthenticationFailedException {
        logger.debug("getSession()");
        if (wss_url == null) {
            throw new NullPointerException("wss_url is not initialized");
        }

        this.authnMethod = authnMethod;

        try {
            logger.info("getSession() with " + authnMethod.asText());

            PostMethod post = new PostMethod(wss_url + "?");
            if (isCredentialProviderAvailable) {
                post.setDoAuthentication(true);
            }
            NameValuePair[] body = arrayMerge(
                    authnMethod.asNameValue(),
                    new NameValuePair[]{
                        new NameValuePair("SERVICE", "WSS"),
                        new NameValuePair("VERSION", "1.0"),
                        new NameValuePair("REQUEST", "GetSession")
                    });

            post.setRequestBody(body);
            client.executeMethod(post);

            Payload getSessionResponse = new Payload(post.getResponseBodyAsStream(), post.getResponseCharSet());

            if (getSessionResponse.containsException()) {
                throw new AuthenticationFailedException(getSessionResponse.asText());
            }

            sessionInfo = new SessionInformationDeegree(getSessionResponse);
            logger.info("New Session with SessionID=" + sessionInfo.getSessionID() +" length: "+sessionInfo.getSessionID().length());
            
            if(sessionInfo.getSessionID() == null || sessionInfo.getSessionID().length() == 0 || sessionInfo.getSessionID().length() == 2){                
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
     * @param authnMethod The authentication method which should be used
     * @throws AuthenticationFailedException Is thrown, when the authentication fails
     */
    public SessionInformation getSession()
            throws AuthenticationFailedException {
        logger.debug("getSession() ");
        if (wss_url == null) {
            throw new NullPointerException("wss_url is not initialized");
        }

        //AuthenticationMethod tmpAuth = authnMethod;

        try {
            logger.info("getSession() with " + authnMethod.asText());

            PostMethod post = new PostMethod(wss_url + "?");
            if (isCredentialProviderAvailable) {
                post.setDoAuthentication(true);
            }
            NameValuePair[] body = arrayMerge(
                    authnMethod.asNameValue(),
                    new NameValuePair[]{
                        new NameValuePair("SERVICE", "WSS"),
                        new NameValuePair("VERSION", "1.0"),
                        new NameValuePair("REQUEST", "GetSession")
                    });

            post.setRequestBody(body);
            HttpClient tmpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
            tmpClient.executeMethod(post);

            Payload getSessionResponse = new Payload(post.getResponseBodyAsStream(), post.getResponseCharSet());

            if (getSessionResponse.containsException()) {
                throw new AuthenticationFailedException(getSessionResponse.asText());
            }

            SessionInformation tmpSI = new SessionInformationDeegree(getSessionResponse);
            logger.info("New Session with SessionID=" + tmpSI.getSessionID() +" length: "+tmpSI.getSessionID().length());
            
            if(tmpSI.getSessionID() == null || tmpSI.getSessionID().length() == 0 || tmpSI.getSessionID().length() == 2){                
                throw new AuthenticationFailedException("SessionID is null or equals  \"\"");
            }
            return tmpSI;

        } catch (IOException ioex) {
            logger.info("Could not perform getSession(). Exception: " + ioex);
            throw new AuthenticationFailedException(ioex);
        }
    }
    

    /**
     * Closes the WSS session
     * 
     * @throws ServiceException Is thrown, when closeSession() fails on the WSS
     */
    public void closeSession() throws ServiceException {

        if (wss_url == null) {
            throw new NullPointerException("wss_url is not initialized");
        }

        try {
            PostMethod post = new PostMethod(wss_url + "?");
            if (isCredentialProviderAvailable) {
                post.setDoAuthentication(true);
            }
            NameValuePair[] data = {
                new NameValuePair("SERVICE", "WSS"),
                new NameValuePair("REQUEST", "CloseSession"),
                new NameValuePair("SESSIONID", sessionInfo.getSessionID())
            };

            post.setRequestBody(data);
            client.executeMethod(post);

            Payload closeSessionResponse = new Payload(post.getResponseBodyAsStream(), post.getResponseCharSet());

            if (closeSessionResponse.containsException()) {
                logger.error(closeSessionResponse.asText());
                throw new ServiceException(closeSessionResponse.asText());
            }

        } catch (IOException ex) {
            logger.info("Could not perform closeSession(). Exception: " + ex);
        }

        logger.debug("closeSession() called successfully");

    }
    
     /**
     * Closes the WSS session
     * 
     * @throws ServiceException Is thrown, when closeSession() fails on the WSS
     */
    public void closeSession(SessionInformation si) throws ServiceException {

        if (wss_url == null) {
            throw new NullPointerException("wss_url is not initialized");
        }

        try {
            PostMethod post = new PostMethod(wss_url + "?");
            if (isCredentialProviderAvailable) {
                post.setDoAuthentication(true);
            }
            NameValuePair[] data = {
                new NameValuePair("SERVICE", "WSS"),
                new NameValuePair("REQUEST", "CloseSession"),
                new NameValuePair("SESSIONID", si.getSessionID())
            };

            post.setRequestBody(data);
            client.executeMethod(post);

            Payload closeSessionResponse = new Payload(post.getResponseBodyAsStream(), post.getResponseCharSet());

            if (closeSessionResponse.containsException()) {
                logger.error(closeSessionResponse.asText());
                throw new ServiceException(closeSessionResponse.asText());
            }

        } catch (IOException ex) {
            logger.info("Could not perform closeSession(). Exception: " + ex);
        }

        logger.debug("closeSession() called successfully");

    }

    /**
     * Returns the URL to the used WSS.
     * 
     * @return URL as String
     */
    public String getWSS() {
        return wss_url.toString();
    }

    public URL getWSSURL() {
        try {
            return new URL(wss_url);
        } catch (MalformedURLException ex) {
            return null;
        }

    }

    /**
     * Sets a proxy for indirect HTTP communication
     * 
     * @param proxy_url URL of the proxy to use.
     * @param port The port of the proxy server.
     */
    public void setProxy(String proxy_url, int port) {
        client.getHostConfiguration().setProxy(proxy_url, port);
    }

    public void setCredentialProvider(CredentialsProvider credentialProvider) {
        client.getParams().setParameter(CredentialsProvider.PROVIDER, credentialProvider);
        if (credentialProvider != null) {
            isCredentialProviderAvailable = true;
        } else {
            isCredentialProviderAvailable = false;
        }
    }

    /**
     * Sets the URL of WSS to use
     * 
     * @param wss_url URL of an WSS
     */
    public void setWSS(String wssUrl) {

        logger.debug("Using WSS: " + wssUrl);
        this.wss_url = wssUrl;

        try {
            URL wssURL = new URL(wssUrl);

            // Cancel if HTTPS is not used
            if (!wssURL.getProtocol().equals("https")) {
                return;
            }
            Protocol easyHTTPS = new Protocol("https", (ProtocolSocketFactory) new EasySSLProtocolSocketFactory(), wssURL.getPort());
            Protocol.registerProtocol("https", easyHTTPS);

            this.supportedAuthnMethods = null;
            this.sessionInfo = null;
        } catch (MalformedURLException ex) {
            logger.error("URL " + wss_url + " is malformed");
        }
    }

    /**
     * Sets the authentication method for WSS interaction
     * 
     * @param authnMethod The authentication method that shall be used.
     */
    public void setAuthenticationMethod(AuthenticationMethod authnMethod) {
        this.authnMethod = authnMethod;
    }

    /**
     * Retrieves the capabilities document of a WSS, parses it and returns 
     * the supported auhtentication methods. If the WSS does not respond, 
     * the method returns an empty List.
     * 
     * @return a List with the IDs of all supported auhtentication methods
     */
    public List<String> getSupportedAuthenticationMethods() {

        if (wss_url == null) {
            throw new NullPointerException("wss_url is not initialized");
        }

        List<String> authMethodsList = new ArrayList<String>();
        Document capabilities = getWSSCapabilities();

        // if there's no valid capabilities document, return an empty list
        if (capabilities == null) {
            return authMethodsList;
        }
        //Problem Arndt 
        ListIterator<Element> it = capabilities.selectNodes("//authn:SupportedAuthenticationMethod").listIterator();

        while (it.hasNext()) {
            Element e = it.next();
            authMethodsList.add(e.valueOf("//authn:AuthenticationMethod/@id"));
            e.detach();
        }

        return authMethodsList;
    }

    /**
     * Tries to retrieve the capabillities document from the specific WSS
     * 
     * @return the capabillities document as Dom4J Document, or in case of an 
     *      error <code>null</code>
     */
    public Document getWSSCapabilities() {
        logger.debug("Initial retrieval of WSS Capabilites");
        if (wss_url == null) {
            throw new NullPointerException("wss_url is not initialized");
        }

        try {
            GetMethod get = new GetMethod(wss_url + "?SERVICE=WSS&REQUEST=GetCapabilities");
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

    public String getSecuredServiceType() {
        if (wssCapabilities == null) {
            wssCapabilities = getWSSCapabilities();
        }
        if (wssCapabilities != null) {
            logger.debug("wssCaps != null");
            Element root = wssCapabilities.getRootElement();
            if (root != null) {
                logger.debug("root Element != null");
                Element caps = root.element("Capability");
                if (caps != null) {
                    logger.debug("Caps Element != null");
                    Element securedServiceType = caps.element("SecuredServiceType");
                    if (securedServiceType != null) {
                        logger.debug("SecuredServiceType Element != null");
                        return securedServiceType.getText();
                    }
                }
            }
        }
        logger.warn("It was not possible to determine the secured service type");
        return null;
    }

    /**
     * Tries to create a Dom4J Document from a InputStream
     * 
     * @param is 
     * @return a XML Document
     * @throws org.dom4j.DocumentException when parsing went wrong
     */
    private Document getXMLDocumentFromStream(InputStream is) throws DocumentException {
        SAXReader reader = new SAXReader();
        Document doc = reader.read(is);
        return doc;
    }

    /**
     * Merges two arrays into a new one
     * 
     * @param arrayA The first array.
     * @param arrayB The Second array
     * @return a new Array
     */
    private static <TYPE> TYPE[] arrayMerge(TYPE[] arrayA, TYPE[] arrayB) {
        int size = arrayA.length + arrayB.length;

        // creating a new array with generics ...
        TYPE[] mergedArray = (TYPE[]) Array.newInstance(arrayA.getClass().getComponentType(), size);

        // Copy the two arrays into a new one
        System.arraycopy(arrayA, 0, mergedArray, 0, arrayA.length);
        System.arraycopy(arrayB, 0, mergedArray, arrayA.length, arrayB.length);

        return (TYPE[]) mergedArray;
    }

    public boolean isSessionAvailable() {
        if (sessionInfo != null) {
            return true;
        } else {
            return false;
        }
    }
}
