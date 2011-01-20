/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package net.environmatics.acs.exceptions;

/**
 * Exception, that is thrown when an error occured while performing a non authorised operation or a service error
 * occures.
 *
 * @author   abonitz
 * @version  $Revision$, $Date$
 */
public class ServiceException extends Exception {

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AuthorisationFailedException.
     */
    public ServiceException() {
        this("Authorisation failed!");
    }

    /**
     * Creates a new AuthorisationFailedException.
     *
     * @param  ex  cause of the AuthenticationFailedException
     */
    public ServiceException(final Exception ex) {
        super(ex);
    }

    /**
     * Creates a new AuthorisationFailedException.
     *
     * @param  message  an error message
     */
    public ServiceException(final String message) {
        super(message);
    }
}
