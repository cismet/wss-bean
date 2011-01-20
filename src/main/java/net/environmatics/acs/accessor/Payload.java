/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package net.environmatics.acs.accessor;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Represents the payload of a service response.
 *
 * @author   abonitz
 * @version  $Revision$, $Date$
 */
public class Payload {

    //~ Static fields/initializers ---------------------------------------------

    private static Logger logger = Logger.getLogger(Payload.class);

    //~ Instance fields --------------------------------------------------------

    // raw data
    private byte[] load;
    // charset used for text encoding
    private String charset;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Payload.
     *
     * @param   is  InputStream
     *
     * @throws  IOException  DOCUMENT ME!
     */
    public Payload(final InputStream is) throws IOException {
        this(is, "UTF-8"); // Standard: UTF-8
    }

    /**
     * Creates a new Payload.
     *
     * @param   is           InputStream
     * @param   charsetName  Is used for text decoding
     *
     * @throws  IOException  DOCUMENT ME!
     */
    public Payload(final InputStream is, final String charsetName) throws IOException {
        load = IOUtils.toByteArray(is);
        charset = charsetName;
    }

    /**
     * Creates a new Payload object.
     *
     * @param   load         DOCUMENT ME!
     * @param   charsetName  DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    public Payload(final byte[] load, final String charsetName) throws IOException {
        this.load = load;
        charset = charsetName;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Tries to return the payload as String.
     *
     * @return  textual representation of the payload
     */
    public String asText() {
        try {
            return new String(load, charset);
        } catch (UnsupportedEncodingException e) {
            return new String(load);
        }
    }

    /**
     * Returns the payload as byte array.
     *
     * @return  raw data
     */
    public byte[] asBytes() {
        if (logger.isDebugEnabled()) {
            logger.debug("asBytes()");
        }
        return load;
    }

    /**
     * If the Payload contains the String "ServiceExceptionReport" the method returns <b>true.</b>
     *
     * @return  If <b>true</b>: the Payload contains an exception message
     */
    public boolean containsException() {
        return (asText().contains("ServiceExceptionReport"));
    }
}
