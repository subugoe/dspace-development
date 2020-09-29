/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.catalogue;

/**
 * This exception is being thrown if a message could not be stored.
 * 
 * @author Julia Damerow
 *
 */
public class MessageStorageException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public MessageStorageException() {
        super();
    }

    public MessageStorageException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public MessageStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageStorageException(String message) {
        super(message);
    }

    public MessageStorageException(Throwable cause) {
        super(cause);
    }

}
