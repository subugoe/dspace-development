/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.condition;

import java.util.HashMap;
import java.util.Map;

import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract class for conditions, to implement the basic getter and setter parameters
 *
 * @author Kim Shepherd
 * @version $Revision$
 */
public abstract class AbstractCondition implements Condition {
    private Map<String, Object> parameters = new HashMap<>();

    @Override
    public Map<String, Object> getParameters() throws LogicalStatementException {
        return this.parameters;
    }
    @Override
    public void setParameters(Map<String, Object> parameters) throws LogicalStatementException {
        this.parameters = parameters;
    }

    @Override
    public Boolean getResult(Context context, Item item) throws LogicalStatementException {
        if(item == null) {
            throw new LogicalStatementException("Item is null");
        }
        if(context == null) {
            throw new LogicalStatementException("Context is null");
        }
        if(this.parameters == null) {
            throw new LogicalStatementException("Parameters are null");
        }
        return true;
    }
}