/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.operator;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatement;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;

/**
 * An operator that implements NIR by negating an OR operation
 *
 * @author Kim Shepherd
 * @version $Revision$
 */
public class Nor extends AbstractOperator {

    Nor() {
        super();
    }

    Nor(List<LogicalStatement> statements) {
        super(statements);
    }

    @Override
    public Boolean getResult(Context context, Item item) throws LogicalStatementException {
        return !(new Or(getStatements()).getResult(context, item));
    }
}