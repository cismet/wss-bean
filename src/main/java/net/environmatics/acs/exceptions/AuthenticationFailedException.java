package net.environmatics.acs.exceptions;

/**
 * Exception, that is thrown when an error occured while
 * performing a getSession() Operation.
 * 
 * @author abonitz
 */
public class AuthenticationFailedException extends Exception {

    /**
     * Creates a new AuthenticationFailedException
     * @param exception that causes the AuthenticationFailedException
     */
    public AuthenticationFailedException(Exception exception) {
        super(exception);
    }

    /**
     * Creates a new AuthenticationFailedException
     * @param message an error message
     */
    public AuthenticationFailedException(String message) {
        super(message);
    }
    
    /**
     * Creates a new AuthenticationFailedException
     */
    public AuthenticationFailedException() {
        this("Authentication failed!");
    }

}
