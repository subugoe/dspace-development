/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.condition;

import java.util.Map;

import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatement;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;

/**
 * The Condition interface
 *
 * A condition is one logical statement testing an item for any idea. A condition is always a logical statements. An
 * operator is not a condition but also a logical statement.
 *
 * @author Kim Shepherd
 * @version $Revision$
 */
public interface Condition extends LogicalStatement {
    void setParameters(Map<String, Object> parameters) throws LogicalStatementException;
    Map<String, Object> getParameters() throws LogicalStatementException;
    Boolean getResult(Context context, Item item) throws LogicalStatementException;
}