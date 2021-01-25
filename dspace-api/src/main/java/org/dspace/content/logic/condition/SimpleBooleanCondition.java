/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.condition;

import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;

/**
 * A condition that always evaluates to true or false depending on the configuration.
 *
 * @author Julia Damerow
 * @version $Revision$
 */
public class SimpleBooleanCondition extends AbstractCondition {

    public Boolean getResult(Context context, Item item) throws LogicalStatementException {
     
        // This super call just throws some useful exceptions if required objects are null
        super.getResult(context, item);

        Boolean bool = false;
        if (getParameters().get("condition") != null) {
            bool = Boolean.parseBoolean((String)getParameters().get("condition"));
        }
        
        return bool;
    }
}
