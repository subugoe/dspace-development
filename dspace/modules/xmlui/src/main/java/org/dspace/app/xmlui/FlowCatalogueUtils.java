/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui;

import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.catalogue.CatalogueDoesNotExistException;
import org.dspace.app.catalogue.CatalogueMessageManager;
import org.dspace.app.catalogue.MessageDeletionException;
import org.dspace.app.catalogue.MessageStorageException;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;

public class FlowCatalogueUtils {

    private static final Logger log = Logger.getLogger(FlowCatalogueUtils.class);

    public static FlowResult saveMessage(Request request)
            throws MessageStorageException, CatalogueDoesNotExistException {
        FlowResult result = new FlowResult();

        try {
            CatalogueMessageManager.storeMessage(request.get("message-catalogue") + "", request.get("message-key") + "",
                    request.get("message-value") + "");
        } catch (CatalogueDoesNotExistException e) {
            log.error("Catalogue does not exist.", e);
            throw e;
        }

        result.setContinue(true);
        result.setOutcome(true);
        result.setParameter("result", "success");
        return result;
    }

    public static FlowResult deleteMessage(Request request)
            throws CatalogueDoesNotExistException, MessageDeletionException {
        FlowResult result = new FlowResult();
        try {
            CatalogueMessageManager.deleteMessage(request.get("message-catalogue") + "",
                    request.get("message-key") + "");
        } catch (CatalogueDoesNotExistException e) {
            log.error("Catalogue does not exist.", e);
            throw e;
        }

        result.setContinue(true);
        result.setOutcome(true);
        return result;
    }

    public static FlowResult addMessage(Request request) throws CatalogueDoesNotExistException {
        FlowResult result = new FlowResult();
        try {
            CatalogueMessageManager.addMessage(request.get("message-catalogue") + "", request.get("message-key") + "",
                    request.get("message-value") + "");
        } catch (CatalogueDoesNotExistException e) {
            log.error("Catalogue does not exist.", e);
            throw e;
        }

        result.setContinue(true);
        result.setOutcome(true);
        return result;
    }
}
