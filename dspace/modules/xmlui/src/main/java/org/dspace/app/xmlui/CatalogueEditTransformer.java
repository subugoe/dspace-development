/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.cocoon.ProcessingException;
import org.apache.log4j.Logger;
import org.dspace.app.catalogue.CatalogueDoesNotExistException;
import org.dspace.app.catalogue.CatalogueMessageManager;
import org.dspace.app.catalogue.MessageLoadingException;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

public class CatalogueEditTransformer extends AbstractDSpaceTransformer {

    private static final Logger log = Logger.getLogger(CatalogueEditTransformer.class);

    private static final Message T_DSPACE_HOME = message("xmlui.general.dspace_home");
    private static final Message T_title = message("xmlui.administrative.catalogue.title");
    private static final Message T_trail = message("xmlui.administrative.catalogue.trail");
    private static final Message T_head = message("xmlui.administrative.catalogue.head");
    private static final Message T_no_catalogue = message("xmlui.administrative.catalogue.none.provided");
    private static final Message T_catalogue_not_exist = message("xmlui.administrative.catalogue.does.not.exist");
    private static final Message T_catalogue_not_valid = message("xmlui.administrative.catalogue.not.valid");
    private static final Message T_message_could_not_load = message("xmlui.administrative.catalogue.message.not.loading");

    @Override
    public void addPageMeta(PageMeta pageMeta)
            throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_DSPACE_HOME);
        pageMeta.addTrailLink(null, T_trail);
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException,
            AuthorizeException, ProcessingException {

        Division division = body.addDivision("catalogue", "catalogue-messages");
        division.setHead(T_head);

        String catalogue = null;
        try {
            catalogue = parameters.getParameter("catalogue");
            log.info(catalogue);
        } catch (ParameterException e) {
            log.error("Could not get catalogue.", e);
            division.addPara(T_no_catalogue);
        }

        if (catalogue != null) {

            Map<String, String> messages;
            try {
                messages = CatalogueMessageManager.getMessages(catalogue);
            } catch (CatalogueDoesNotExistException e) {
                division.addPara(T_catalogue_not_exist);
                log.info("Catalogue does not exist.");
                return;
            } catch (MessageLoadingException e) {
                division.addPara(T_message_could_not_load);
                division.addPara(e.getLocalizedMessage());
                log.error("Could not load catalogue.", e);
                return;
            }

            if (messages == null) {
                division.addPara(T_catalogue_not_valid);
                return;
            }

            if (!messages.isEmpty()) {
                Table table = division.addTable("messages-table", messages.size(), 3, "catalogue-messages");

                for (Entry<String, String> message : messages.entrySet()) {
                    String key = message.getKey();
                    if (key == null || key.trim().isEmpty()) {
                        // if key is empty, it's an invalid key, so we'll ignore it
                        continue;
                    }
                    Row row = table.addRow();
                    Cell nameCell = row.addCell(null, null, "catalogue-message-key");
                    nameCell.addContent(key);

                    Cell valueCell = row.addCell(null, null, "catalogue-message-value");
                    TextArea text = valueCell.addTextArea("text_" + key);
                    text.setDisabled();
                    text.setSize(3, 18);
                    text.setValue(message.getValue());

                    Cell btnCell = row.addCell();
                    btnCell.addHidden(key.replace(".", "_")).setValue(key);
                    btnCell.addButton("EditButtons").setValue("Edit");
                }
            }
        }
    }

}
