/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.catalogue;

/**
 * Exception that is thrown if a specific catalogue does not exist.
 * 
 * @author Julia Damerow
 *
 */
public class CatalogueDoesNotExistException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public CatalogueDoesNotExistException() {
        super();
    }

    public CatalogueDoesNotExistException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public CatalogueDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

    public CatalogueDoesNotExistException(String message) {
        super(message);
    }

    public CatalogueDoesNotExistException(Throwable cause) {
        super(cause);
    }

}
