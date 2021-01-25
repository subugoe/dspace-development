/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.doi.DOIIdentifierNotApplicableException;
import org.dspace.utils.DSpace;

/**
 * Flow utils class to register a new DOI for an item.
 * 
 * @author Julia Damerow
 *
 */
public class FlowDoiUtils {
    
    private static Logger log = Logger.getLogger(FlowDoiUtils.class);


    public static FlowResult addDoi(Context context, String identifier) throws SQLException {
        FlowResult result = new FlowResult();
        result.setContinue(true);
        
        Item item = null;
        try {
            item = Item.find(context, Integer.valueOf(identifier));
        } catch (NumberFormatException e) {
            // ignoring the exception in case of a malformed input string
        }
        
        if (item != null) {
            try {
                DOIIdentifierProvider provider = new DSpace().getSingletonService(DOIIdentifierProvider.class);
                String newDoi = provider.register(context, item, true);
                if (newDoi != null) {
                    result.setOutcome(true);
                } else {
                    log.error("Got a null DOI after registering...");
                }
            } catch (DOIIdentifierNotApplicableException ex) {
                log.error(ex);
            } catch (IllegalArgumentException ex) {
                log.error(ex);
            } catch (IllegalStateException ex) {
                log.error(ex);
            } catch (IdentifierException ex) {
                log.error(ex);
            } 
        }
        
        return result;
    }
}
