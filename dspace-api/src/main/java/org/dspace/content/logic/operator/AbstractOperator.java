/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.operator;

import java.util.ArrayList;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatement;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;

/**
 * Abstract class for an operator
 *
 * @author Kim Shepherd
 * @version $Revision$
 */
public abstract class AbstractOperator implements LogicalStatement {

    private List<LogicalStatement> statements = new ArrayList<>();

    public List<LogicalStatement> getStatements() {
        return statements;
    }

    public void setStatements(List<LogicalStatement> statements) {
        this.statements = statements;
    }

    AbstractOperator() {}

    AbstractOperator(List<LogicalStatement> statements) {
        this.statements = statements;
    }

    @Override
    public Boolean getResult(Context context, Item item) throws LogicalStatementException {
        return false;
    }
}