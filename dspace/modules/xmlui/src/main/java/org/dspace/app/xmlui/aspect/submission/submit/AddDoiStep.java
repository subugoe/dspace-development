/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission.submit;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.cocoon.ProcessingException;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.doi.DOIIdentifierNotApplicableException;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

/**
 * Submission step that lets the user add a DOI to an object.
 * 
 * @author Julia Damerow
 *
 */
public class AddDoiStep extends AbstractSubmissionStep {

    private static Logger log = Logger.getLogger(AddDoiStep.class);
    
    protected static final Message T_head = message("xmlui.Submission.submit.AddDoiStep.head");
    protected static final Message T_section_head = message("xmlui.Submission.submit.AddDoiStep.section_head");
    protected static final Message T_AddDoiBtn = message("xmlui.Submission.submit.AddDoiStep.btn");
    protected static final Message T_AddDoiBtn_done = message("xmlui.Submission.submit.AddDoiStep.done.btn");
    protected static final Message T_itemDoi = message("xmlui.Submission.submit.itemDOI.created");
    protected static final Message T_itemDoi_no_doi = message("xmlui.Submission.submit.itemDOI.no_doi");
    protected static final Message T_itemDoi_note = message("xmlui.Submission.submit.itemDOI.note");

    private DOIIdentifierProvider provider = new DSpace().getSingletonService(DOIIdentifierProvider.class);

    public AddDoiStep() {
        this.requireSubmission = true;
        this.requireStep = true;
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException,
            AuthorizeException, ProcessingException {

        Item item = submission.getItem();
        Collection collection = submission.getCollection();
        String actionURL = contextPath + "/handle/" + collection.getHandle() + "/submit/" + knot.getId() + ".continue";

        Division div = body.addInteractiveDivision("submit-doi", actionURL, Division.METHOD_POST, "primary submission");
        div.setHead(T_head);

        addSubmissionProgressList(div);

        List list = div.addList("submit-describe", List.TYPE_FORM);
        list.setHead(T_section_head);

        String itemDoi = DOIIdentifierProvider.getDOIByObject(context, item);
        if (itemDoi == null) {
            try {
                itemDoi = provider.mint(context, item);
            } catch (DOIIdentifierNotApplicableException e) {
                // this happens when a filter suppresses the creation of a DOI.
                // We'll ignor this.
                log.debug("Creation of DOI was suppressed by filter.", e);
            } catch (IdentifierException e) {
                // some other problem occured while minting the DOI.
                // log this and continue.
                log.error("Error minting DOI.", e);
            }
        }
        Button addDoiBtn = list.addItem().addButton("submit-create-doi");
        addDoiBtn.setValue(T_AddDoiBtn);
        if (itemDoi != null && !itemDoi.isEmpty()) {
            addDoiBtn.setDisabled();
            list.addItem().addContent(T_itemDoi.parameterize(itemDoi));
        } else {
            list.addItem().addContent(T_itemDoi_note);
        }

        addControlButtons(list);
    }

    @Override
    public List addReviewSection(List reviewList)
            throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        /* uncomment this code and remove the final "return null;" to show 
         * the addDoiStep as part of the verifyStep.
         * // Create a new list section for this step (and set its heading)
         * List doiSection = reviewList.addList("submit-review-" + this.stepAndPage, List.TYPE_FORM);
         * doiSection.setHead(T_head);
         * 
         * Item item = submission.getItem();
         * String itemDoi = DOIIdentifierProvider.getDOIByObject(context, item);
         * if (itemDoi != null && !itemDoi.isEmpty()) {
         *     doiSection.addItem().addContent(T_itemDoi.parameterize(itemDoi));
         * } else {
         *     doiSection.addItem().addContent(T_itemDoi_no_doi);
         * }
         * return doiSection;
        */
        return null;
    }

}
