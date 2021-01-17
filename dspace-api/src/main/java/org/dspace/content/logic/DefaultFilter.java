/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * The default filter, a very simple implementation of Filter / LogicalStatement
 * The idea is to have this as a wrapper / root class for all logical operations, so it takes a single
 * statement as a property (unlike an operator) and takes no parameters (unlike a condition)
 *
 * @author Kim Shepherd
 * @version $Revision$
 */
public class DefaultFilter implements Filter {
    private LogicalStatement statement;

    // be aware that this is singular not plural. A filter can have one substatement only.
    public void setStatement(LogicalStatement statement) {
        this.statement = statement;
    }

    private static Logger log = Logger.getLogger(Filter.class);

    public Boolean getResult(Context context, Item item) throws LogicalStatementException {
        return this.statement.getResult(context, item);
    }
}