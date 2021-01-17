/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.operator;

import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatement;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;

/**
 * An operator that implements NOT by simply negating a statement
 * Note that this operator doesn't actually implement the 'AbstractOperator' interface because
 * we only want one sub-statement. So it's actually just a simple implementation of LogicalStatement.
 * Not can have one sub-statement only, while and, or, nor, ... can have multiple sub-statements.
 *
 * @author Kim Shepherd
 * @version $Revision$
 */
public class Not implements LogicalStatement {

    private LogicalStatement statement;

    public LogicalStatement getStatements() {
        return statement;
    }

    public void setStatements(LogicalStatement statement) {
        this.statement = statement;
    }

    Not() {}

    Not(LogicalStatement statement) {
        this.statement = statement;
    }

    @Override
    public Boolean getResult(Context context, Item item) throws LogicalStatementException {
        return !statement.getResult(context, item);
    }
}