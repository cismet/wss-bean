/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * $HeadURL$
 * $Revision: 1.1.1.1 $
 * $Date: 2009-08-14 11:22:48 $
 *
 * ====================================================================
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.commons.httpclient.contrib.ssl;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClientError;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

/**
 * <p>EasySSLProtocolSocketFactory can be used to creats SSL {@link Socket}s that accept self-signed certificates.</p>
 *
 * <p>This socket factory SHOULD NOT be used for productive systems due to security reasons, unless it is a concious
 * decision and you are perfectly aware of security implications of accepting self-signed certificates</p>
 *
 * <p>Example of using custom protocol socket factory for a specific host:</p>
 *
 * <pre>
       Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);

       HttpClient client = new HttpClient();
       client.getHostConfiguration().setHost("localhost", 443, easyhttps);
       // use relative url only
       GetMethod httpget = new GetMethod("/");
       client.executeMethod(httpget);
 *     </pre>
 *
 * <p>Example of using custom protocol socket factory per default instead of the standard one:</p>
 *
 * <pre>
       Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
       Protocol.registerProtocol("https", easyhttps);

       HttpClient client = new HttpClient();
       GetMethod httpget = new GetMethod("https://localhost/");
       client.executeMethod(httpget);
 *     </pre>
 *
 * @author   <a href="mailto:oleg -at- ural.ru">Oleg Kalnichevski</a>
 *
 *           <p>DISCLAIMER: HttpClient developers DO NOT actively support this component. The component is provided as a
 *           reference material, which may be inappropriate for use without additional customization.</p>
 * @version  $Revision$, $Date$
 */

public class EasySSLProtocolSocketFactory implements SecureProtocolSocketFactory {

    //~ Static fields/initializers ---------------------------------------------

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(EasySSLProtocolSocketFactory.class);

    //~ Instance fields --------------------------------------------------------

    private SSLContext sslcontext = null;

    //~ Constructors -----------------------------------------------------------

    /**
     * Constructor for EasySSLProtocolSocketFactory.
     */
    public EasySSLProtocolSocketFactory() {
        super();
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  HttpClientError  DOCUMENT ME!
     */
    private static SSLContext createEasySSLContext() {
        try {
            final SSLContext context = SSLContext.getInstance("SSL");
            context.init(
                null,
                new TrustManager[] { new EasyX509TrustManager(null) },
                null);
            return context;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new HttpClientError(e.toString());
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private SSLContext getSSLContext() {
        if (this.sslcontext == null) {
            this.sslcontext = createEasySSLContext();
        }
        return this.sslcontext;
    }

    /**
     * @see  SecureProtocolSocketFactory#createSocket(java.lang.String,int,java.net.InetAddress,int)
     */
    @Override
    public Socket createSocket(final String host, final int port, final InetAddress clientHost, final int clientPort)
            throws IOException, UnknownHostException {
        return getSSLContext().getSocketFactory().createSocket(
                host,
                port,
                clientHost,
                clientPort);
    }

    /**
     * Attempts to get a new socket connection to the given host within the given time limit.
     *
     * <p>To circumvent the limitations of older JREs that do not support connect timeout a controller thread is
     * executed. The controller thread attempts to create a new socket within the given limit of time. If socket
     * constructor does not return until the timeout expires, the controller terminates and throws an
     * {@link ConnectTimeoutException}</p>
     *
     * @param   host          the host name/IP
     * @param   port          the port on the host
     * @param   localAddress  clientHost the local host name/IP to bind the socket to
     * @param   localPort     clientPort the port on the local machine
     * @param   params {@link HttpConnectionParams Http connection parameters}
     *
     * @return  Socket a new socket
     *
     * @throws  IOException               if an I/O error occurs while creating the socket
     * @throws  UnknownHostException      if the IP address of the host cannot be determined
     * @throws  ConnectTimeoutException   DOCUMENT ME!
     * @throws  IllegalArgumentException  DOCUMENT ME!
     */
    @Override
    public Socket createSocket(
            final String host,
            final int port,
            final InetAddress localAddress,
            final int localPort,
            final HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        final int timeout = params.getConnectionTimeout();
        final SocketFactory socketfactory = getSSLContext().getSocketFactory();
        if (timeout == 0) {
            return socketfactory.createSocket(host, port, localAddress, localPort);
        } else {
            final Socket socket = socketfactory.createSocket();
            final SocketAddress localaddr = new InetSocketAddress(localAddress, localPort);
            final SocketAddress remoteaddr = new InetSocketAddress(host, port);
            socket.bind(localaddr);
            socket.connect(remoteaddr, timeout);
            return socket;
        }
    }

    /**
     * @see  SecureProtocolSocketFactory#createSocket(java.lang.String,int)
     */
    @Override
    public Socket createSocket(final String host, final int port) throws IOException, UnknownHostException {
        return getSSLContext().getSocketFactory().createSocket(
                host,
                port);
    }

    /**
     * @see  SecureProtocolSocketFactory#createSocket(java.net.Socket,java.lang.String,int,boolean)
     */
    @Override
    public Socket createSocket(final Socket socket, final String host, final int port, final boolean autoClose)
            throws IOException, UnknownHostException {
        return getSSLContext().getSocketFactory().createSocket(
                socket,
                host,
                port,
                autoClose);
    }

    @Override
    public boolean equals(final Object obj) {
        return ((obj != null) && obj.getClass().equals(EasySSLProtocolSocketFactory.class));
    }

    @Override
    public int hashCode() {
        return EasySSLProtocolSocketFactory.class.hashCode();
    }
}
