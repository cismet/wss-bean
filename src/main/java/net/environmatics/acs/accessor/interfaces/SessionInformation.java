/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package net.environmatics.acs.accessor.interfaces;

/**
 * Defines what a SessionInformation container should look like.
 *
 * @author   abonitz
 * @version  $Revision$, $Date$
 */
public interface SessionInformation {

    //~ Methods ----------------------------------------------------------------

    /**
     * Returns the Session ID of a WAS / WSS Session.
     *
     * @return  Session ID
     */
    String getSessionID();
}
