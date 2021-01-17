/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.condition;

import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;

/**
 * A condition that accepts a list of collection handles and returns true
 * if the item belongs to any of them.
 *
 * @author Kim Shepherd
 * @version $Revision$
 */
public class InCollectionCondition extends AbstractCondition {
    private static Logger log = Logger.getLogger(InCollectionCondition.class);

    @Override
    public Boolean getResult(Context context, Item item) throws LogicalStatementException {

        try {
            List<String> collectionHandles = (List<String>) getParameters().get("collections");
            Collection[] itemCollections = item.getCollections();

            for (Collection collection : itemCollections) {
                if (collectionHandles.contains(collection.getHandle())) {
                    log.debug("item " + item.getHandle() + " is in collection "
                        + collection.getHandle() + ", returning true");
                    return true;
                }
            }
        } catch(SQLException e) {
            log.error("Encountered SQL error inspecting item collections: " + e.getMessage());
            throw new LogicalStatementException(e);
        }

        log.debug("item " + item.getHandle() + " not found in the passed collection handle list");

        return false;
    }
}