/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.catalogue;

/**
 * This exception is being thrown when a message file can't be loaded.
 * 
 * @author Julia Damerow
 *
 */
public class MessageLoadingException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public MessageLoadingException() {
        super();
    }

    public MessageLoadingException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public MessageLoadingException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageLoadingException(String message) {
        super(message);
    }

    public MessageLoadingException(Throwable cause) {
        super(cause);
    }

}
