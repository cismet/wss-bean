package net.environmatics.acs.accessor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * Represents the payload of a service response
 * 
 * @author abonitz
 */
public class Payload {
    
    private static Logger logger = Logger.getLogger(Payload.class);
    // raw data
    private byte[] load;
    // charset used for text encoding
    private String charset;
    
    
    /**
     * Creates a new Payload
     * 
     * @param is InputStream
     * @throws java.io.IOException
     */
    public Payload(InputStream is) throws IOException {
        this(is, "UTF-8"); // Standard: UTF-8
    }
    
    /**
     * Creates a new Payload
     * 
     * @param is InputStream 
     * @param charsetName Is used for text decoding 
     * @throws java.io.IOException
     */
    public Payload(InputStream is, String charsetName) throws IOException {
        load = IOUtils.toByteArray(is);        
        charset = charsetName;
    }
    
    public Payload(byte[] load, String charsetName) throws IOException {
        this.load =load;        
        charset = charsetName;
    }
    
    
    /**
     * Tries to return the payload as String
     * 
     * @return textual representation of the payload
     */
    public String asText() {
        try {
            return new String(load, charset); 
        } catch (UnsupportedEncodingException e) {
            return new String(load);
        }
    }
    
    /**
     * Returns the payload as byte array
     * 
     * @return raw data
     */
    public byte[] asBytes() {
        logger.debug("asBytes()");
        return load;
    }
    
    /**
     * If the Payload contains the String "ServiceExceptionReport"
     * the method returns <b>true</b>
     * 
     * @return If <b>true</b>: the Payload contains an exception message
     */
    public boolean containsException() {
        return (asText().contains("ServiceExceptionReport"));
    }
}
