/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.condition;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;

/**
 * A condition to evaluate an item based on how many bitstreams it has in a particular bundle
 *
 * @author Kim Shepherd
 * @version $Revision$
 */
public class BitstreamCountCondition extends AbstractCondition {

    private static Logger log = Logger.getLogger(BitstreamCountCondition.class);

    @Override
    public Boolean getResult(Context context, Item item) throws LogicalStatementException {

        // This super call just throws some useful exceptions if required objects are null
        super.getResult(context, item);

        Integer min = -1;
        if (getParameters().get("min") != null) {
            min = Integer.parseInt((String)getParameters().get("min"));
        }
        
        Integer max = -1;
        if (getParameters().get("max") != null)
        {
            max = Integer.parseInt((String)getParameters().get("max"));
        }
        
        String bundleName = (String)getParameters().get("bundle");
        
        if (min < 0 && max < 0) {
            throw new LogicalStatementException("Either min or max parameter must be 0 or bigger.");
        }

        Bundle[] bundles;
        int count = 0;

        try {
            if (bundleName != null) {
                bundles = item.getBundles(bundleName);
            } else {
                bundles = item.getBundles();
            }

            for (Bundle bundle : bundles) {
                count += bundle.getBitstreams().length;
            }
        } catch(SQLException e) {
            log.error("Failed to inspect item bundles due to SQL error: " + e.getMessage());
            throw new LogicalStatementException(e);
        }

        log.debug("logic for " + item.getHandle() + ": min: " + min + ", max: " + max
            + ", bundle: " + bundleName + ", final count: " + count);

        if (min < 0)
        {
            return (count <= max);
        }
        
        if (max < 0)
        {
            return (count >= min);
        }
        
        return (count <= max && count >= min);
    }
}