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
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * A condition that accepts a group and action parameter and returns true if the group
 * can perform the action on a given item
 *
 * @author Kim Shepherd
 * @version $Revision$
 */
public class ReadableByGroupCondition extends AbstractCondition {
    private static Logger log = Logger.getLogger(ReadableByGroupCondition.class);

    @Override
    public Boolean getResult(Context context, Item item) throws LogicalStatementException {

        String group = (String)getParameters().get("group");
        String action = (String)getParameters().get("action");

        try {
            List<ResourcePolicy> policies =
                AuthorizeManager.getPoliciesActionFilter(context, item, Constants.getActionID(action));
            for (ResourcePolicy policy : policies) {
                if (policy.getGroup().getName().equals(group)) {
                    return true;
                }
            }
        } catch(SQLException e) {
            log.error("Error trying to read policies for " + item.getHandle() + ": " + e.getMessage());
            throw new LogicalStatementException(e);
        }
        log.debug("item " + item.getHandle() + " not readable by anonymous group");

        return false;
    }
}