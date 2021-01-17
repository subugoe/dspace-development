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
 * An operator that implements NAND by negating an AND operation
 *
 * @author Kim Shepherd
 * @version $Revision$
 */
public class Nand extends AbstractOperator {

    Nand() {
        super();
    }

    Nand(List<LogicalStatement> statements) {
        super(statements);
    }

    @Override
    public Boolean getResult(Context context, Item item) throws LogicalStatementException {
        return !(new And(getStatements()).getResult(context, item));
    }
}