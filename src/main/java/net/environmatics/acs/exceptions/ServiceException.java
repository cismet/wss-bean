package net.environmatics.acs.exceptions;


/**
 * Exception, that is thrown when an error occured while
 * performing a non authorised operation or a service error
 * occures.
 * 
 * @author abonitz
 */
public class ServiceException extends Exception {

    /**
     * Creates a new AuthorisationFailedException
     * @param cause of the AuthenticationFailedException
     */
    public ServiceException(Exception ex) {
        super(ex);
    }

    /**
     * Creates a new AuthorisationFailedException
     * @param message an error message
     */
    public ServiceException(String message) {
        super(message);
    }
    
    /**
     * Creates a new AuthorisationFailedException
     */
    public ServiceException() {
        this("Authorisation failed!");
    }

}
