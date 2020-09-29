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

import org.apache.cocoon.ProcessingException;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Hidden;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

public class CatalogueUpdateResultTransformer extends AbstractDSpaceTransformer {

    private static final Logger log = Logger.getLogger(CatalogueUpdateResultTransformer.class);

    private static final Message T_message_update_success = message(
            "xmlui.administrative.catalogue.message.update.success");

    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException,
            AuthorizeException, ProcessingException {
        Division div = body.addDivision("result");
        Hidden resultMsg = div.addHidden("result_message");
        try {
            resultMsg.setValue(T_message_update_success);
        } catch (WingException e) {
            log.error("Could not get message.", e);
        }
    }

}
