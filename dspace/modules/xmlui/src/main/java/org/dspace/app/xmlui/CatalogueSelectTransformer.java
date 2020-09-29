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

import org.apache.cocoon.ProcessingException;
import org.apache.log4j.Logger;
import org.dspace.app.catalogue.MessageCatalogueProvider;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

public class CatalogueSelectTransformer extends AbstractDSpaceTransformer {

    private static final Logger log = Logger.getLogger(CatalogueSelectTransformer.class);

    private static final Message T_DSPACE_HOME = message("xmlui.general.dspace_home");
    private static final Message T_title = message("xmlui.administrative.select.catalogue.title");
    private static final Message T_trail = message("xmlui.administrative.select.catalogue.trail");
    private static final Message T_head = message("xmlui.administrative.select.catalogue.head");

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
        Division division = body.addDivision("select-catalogue");
        division.setHead(T_head);

        List list = division.addList("catalogue-list", List.TYPE_SIMPLE, "catalogue-list");

        Map<String, String> catalogues = MessageCatalogueProvider.getCatalogues();
        for (Entry<String, String> catalogue : catalogues.entrySet()) {
            list.addItemXref(contextPath + "/admin/catalogue/messages?catalogue=" + new String(catalogue.getKey()),
                    catalogue.getValue());
        }
    }

}
