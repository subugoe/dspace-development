/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.step;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.doi.DOIIdentifierNotApplicableException;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.utils.DSpace;

/**
 * Submission step that adds a DOI to an element if the create doi button
 * is being pressed.
 * 
 * @author Julia Damerow
 *
 */
public class AddDoiStep extends AbstractProcessingStep {

    private static Logger log = Logger.getLogger(AddDoiStep.class);
    private DOIIdentifierProvider provider = new DSpace().getSingletonService(DOIIdentifierProvider.class);

    @Override
    public int doProcessing(Context context, HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo) throws ServletException, IOException, SQLException, AuthorizeException {
        
        String buttonPressed = Util.getSubmitButton(request, NEXT_BUTTON);

        if (buttonPressed.equals("submit-create-doi")) {
            // create doi
            Item item = subInfo.getSubmissionItem().getItem();
            
            try {
                String newDoi = provider.register(context, item, true);
                if (newDoi != null) {
                    request.setAttribute("registered-doi", newDoi);
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
        
        return 0;
    }

    @Override
    public int getNumberOfPages(HttpServletRequest request, SubmissionInfo subInfo) throws ServletException {
        // There is only one page to add a DOI
        return 1;
    }

}
