/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package net.environmatics.acs.exceptions;

/**
 * Exception, that is thrown when an error occured while performing a getSession() Operation.
 *
 * @author   abonitz
 * @version  $Revision$, $Date$
 */
public class AuthenticationFailedException extends Exception {

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AuthenticationFailedException.
     */
    public AuthenticationFailedException() {
        this("Authentication failed!");
    }

    /**
     * Creates a new AuthenticationFailedException.
     *
     * @param  exception  that causes the AuthenticationFailedException
     */
    public AuthenticationFailedException(final Exception exception) {
        super(exception);
    }

    /**
     * Creates a new AuthenticationFailedException.
     *
     * @param  message  an error message
     */
    public AuthenticationFailedException(final String message) {
        super(message);
    }
}
