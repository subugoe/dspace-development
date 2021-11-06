/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.DOI;
import org.dspace.identifier.IdentifierException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.springframework.beans.factory.annotation.Required;

/**
 * Class that handles DOI registrations with Crossref.
 * 
 * @author Julia Damerow
 *
 */
public class CrossRefDoiConnector extends AbstractDoiConnector {

    // Configuration property names
    static final String CFG_USER = "identifier.doi.crossref.user";
    static final String CFG_PASSWORD = "identifier.doi.crossref.password";

    static final String CFG_DEPOSITOR = "identifier.doi.crossref.depositor";
    static final String CFG_DEPOSITOR_EMAIL = "identifier.doi.crossref.depositor.email";
    static final String CFG_REGISTRANT = "identifier.doi.crossref.registrant";

    private static final String MD_SCHEMA = "dc";
    private static final String DOI_ELEMENT = "identifier";
    private static final String DOI_QUALIFIER = "uri";

    private static final String CROSSREF_SCHEMA = "http://www.crossref.org/schema/4.4.2";
    
    private static final String FORM_DATA_FILE = "mdFile";
    private static final String FORM_DATA_PASSWORD = "pwd";
    private static final String FORM_DATA_USER = "usr";

    private static final String DOI_RESOLVER_SCHEME = "https";
    private static final String DOI_RESOLVER_HOST = "dx.doi.org";
    private static final String REDIRECT_HEADER = "Location";
    private static final String DOI_PREFIX = DOI_RESOLVER_SCHEME + "://" + DOI_RESOLVER_HOST + "/";
    private static final String DOI_PREFIX_ALT = "http://" + DOI_RESOLVER_HOST + "/";

    private String DEPOSITOR;
    private String DEPOSITOR_EMAIL;
    private String REGISTRANT;

    /**
     * Stores the scheme used to connect to the Crossref server. It will be set by
     * spring dependency injection.
     */
    protected String SCHEME;
    /**
     * Stores the hostname of the Crossref server. Set by spring dependency
     * injection.
     */
    protected String HOST;

    /**
     * Path on the Crossref server deposit endpoint. Set by spring dependency
     * injection.
     */
    protected String DEPOSIT_PATH;

    /**
     * Timeout for API responses. Defaults to 5 seconds as processing can be slow.
     */
    protected int TIMEOUT = 5000;

    /**
     * Metadata field used for journal ISSN (used for pre-register validation)
     */
    protected String parentIssnField;
    /**
     * Metadata field used for parent publication DOI (used for pre-register validation)
     */
    protected String parentDoiField;
    
    /**
     * Method that sets the scheme for the POST request. Used by Spring's dependency
     * injection
     * 
     * @param CROSSREF_SCHEME The scheme to use, 'https' or 'http'.
     */
    @Required
    public void setCROSSREF_SCHEME(String CROSSREF_SCHEME)
    {
        this.SCHEME = CROSSREF_SCHEME;
    }

    /**
     * Method to set the host for the POST request. Used by Spring's dependency
     * injection.
     * 
     * @param CROSSREF_HOST The host for the request, probably 'doi.crossref.org'.
     */
    @Required
    public void setCROSSREF_HOST(String CROSSREF_HOST)
    {
        this.HOST = CROSSREF_HOST;
    }

    /**
     * Method to set the path for the POST request. Used by Spring's dependency
     * injection.
     * 
     * @param CROSSREF_DEPOSIT_PATH The path for the deposit request, probably
     *                              '/servlet/deposit'.
     */
    @Required
    public void setCROSSREF_DEPOSIT_PATH(String CROSSREF_DEPOSIT_PATH)
    {
        CROSSREF_DEPOSIT_PATH = !CROSSREF_DEPOSIT_PATH.startsWith("/") ? "/" + CROSSREF_DEPOSIT_PATH
                : CROSSREF_DEPOSIT_PATH;
        CROSSREF_DEPOSIT_PATH = !CROSSREF_DEPOSIT_PATH.endsWith("/") ? CROSSREF_DEPOSIT_PATH + "/"
                : CROSSREF_DEPOSIT_PATH;

        this.DEPOSIT_PATH = CROSSREF_DEPOSIT_PATH;
    }

    @Required
    public void setCROSSREF_TIMEOUT(int CROSSREF_TIMEOUT) {
        this.TIMEOUT = CROSSREF_TIMEOUT;
    }
    
    @Required
    public void setParentIssnField(String parentIssnField) {
        this.parentIssnField = parentIssnField;
    }
    
    @Required
    public void setParentDoiField(String parentDoiField) {
        this.parentDoiField = parentDoiField;
    }

    protected DisseminationCrosswalk prepareXwalk(String type)
    {
        DisseminationCrosswalk xwalk = (DisseminationCrosswalk) PluginManager
                .getNamedPlugin(DisseminationCrosswalk.class, this.CROSSWALK_NAME + "_" + type.replace(" ", "_").toLowerCase());

        // default crosswalk
        if (xwalk == null) {
            xwalk = (DisseminationCrosswalk) PluginManager
                    .getNamedPlugin(DisseminationCrosswalk.class, this.CROSSWALK_NAME + "_other");
        }
        if (xwalk == null)
        {
            throw new RuntimeException("Can't find crosswalk '" + CROSSWALK_NAME  + "_" + type.replace(" ", "_").toLowerCase() + "' or " + CROSSWALK_NAME  + "_other!");
        }

        return xwalk;
    }

    protected String getDepositorName()
    {
        if (null == this.DEPOSITOR)
        {
            this.DEPOSITOR = this.configurationService.getProperty(CFG_DEPOSITOR);
            if (null == this.DEPOSITOR)
            {
                throw new RuntimeException("Unable to load depositor from " + "configuration. Cannot find property "
                        + CFG_DEPOSITOR + ".");
            }
        }
        return this.DEPOSITOR;
    }

    protected String getDepositorEmail()
    {
        if (null == this.DEPOSITOR_EMAIL)
        {
            this.DEPOSITOR_EMAIL = this.configurationService.getProperty(CFG_DEPOSITOR_EMAIL);
            if (null == this.DEPOSITOR_EMAIL)
            {
                throw new RuntimeException("Unable to load depositor from " + "configuration. Cannot find property "
                        + CFG_DEPOSITOR_EMAIL + ".");
            }
        }
        return this.DEPOSITOR_EMAIL;
    }

    protected String getRegistrant()
    {
        if (null == this.REGISTRANT)
        {
            this.REGISTRANT = this.configurationService.getProperty(CFG_REGISTRANT);
            if (null == this.REGISTRANT)
            {
                throw new RuntimeException("Unable to load depositor from " + "configuration. Cannot find property "
                        + CFG_REGISTRANT + ".");
            }
        }
        return this.REGISTRANT;
    }

    @Override
    public boolean isDOIReserved(Context context, String doi) throws DOIIdentifierException
    {
        // Crossref does not require reservation of DOIs, we just return true
        return true;
    }

    @Override
    public boolean isDOIReserved(Context context, DSpaceObject dso, String doi) throws DOIIdentifierException
    {
        // Crossref does not require reservation of DOIs, we just return true
        return true;
    }

    @Override
    public void deleteDOI(Context context, String doi) throws DOIIdentifierException
    {
        log.warn("Skipping deletion of DOI as Crossref does not allow deactivation of DOIs.");
    }

    @Override
    public void reserveDOI(Context context, DSpaceObject dso, String doi) throws DOIIdentifierException
    {
        log.warn("Skipping reserving DOI as Crossref does not require this step.");
    }

    @Override
    public void registerDOI(Context context, DSpaceObject dso, String doi) throws DOIIdentifierException
    {
        // check if the DOI is already registered online
        if (this.isDOIRegistered(context, doi))
        {
            // if it is registered for another object we should notify an admin
            if (!ConfigurationManager.getBooleanProperty("identifier.doi.crossref.override", false)
                    && !this.isDOIRegistered(context, dso, doi))
            {
                // DOI is reserved for another object
                log.warn(String.format("DOI {} is registered for another object already.", doi));
                throw new DOIIdentifierException(DOIIdentifierException.DOI_ALREADY_EXISTS);
            }
        }

        Metadatum[] typeMd = dso.getDC("type", Item.ANY, Item.ANY);
        if (typeMd == null || typeMd.length == 0)
        {
            throw new DOIIdentifierException("Type of record is missing.");
        }

        // Validate item metadata required by Crossref before proceeding with registration
        // (or throw a CONVERSION_ERROR exception)
        validateItemMetadata(dso, typeMd[0].value);
        
        DisseminationCrosswalk xwalk = this.prepareXwalk(typeMd[0].value);
        if (!xwalk.canDisseminate(dso))
        {
            log.error("Crosswalk " + this.CROSSWALK_NAME + " cannot disseminate DSO with type " + dso.getType()
                    + " and ID " + dso.getID() + ". Giving up reserving the DOI " + doi + ".");
            throw new DOIIdentifierException("Cannot disseminate " + dso.getTypeText() + "/" + dso.getID()
                    + " using crosswalk " + this.CROSSWALK_NAME + ".", DOIIdentifierException.CONVERSION_ERROR);
        }

        // we'll set a DOI to have it available in XSLT
        String metadataDOI = extractDOI(dso);
        boolean hasBeenRegisteredBefore = true;
        if (metadataDOI == null)
        {
            hasBeenRegisteredBefore = false;
            try
            {
                dso.addMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null, DOI.DOIToExternalForm(doi));
            } catch (IdentifierException e)
            {
                throw new DOIIdentifierException("Could not add DOI to object.", e);
            }
        } else
        {
            String existingDoi = metadataDOI.startsWith("http:") ? metadataDOI.substring(DOI_PREFIX_ALT.length())
                    : metadataDOI.substring(DOI_PREFIX.length());
            
            // Are we trying to update the wrong object?
            if (!existingDoi.equals(doi.substring(DOI.SCHEME.length())))
            {
                log.info("DSO with type " + dso.getTypeText() + " and id " + dso.getID() + " already has DOI "
                        + metadataDOI + " which doesn't match " + doi + " . Won't register object.");
                return;
            }
        }

        // Let's create the XML to send
        Element root = null;
        try
        {
            root = xwalk.disseminateElement(dso);
        } catch (AuthorizeException ae)
        {
            log.error("Caught an AuthorizeException while disseminating DSO " + "with type " + dso.getType()
                    + " and ID " + dso.getID() + ". Giving up to reserve DOI " + doi + ".", ae);
            throw new DOIIdentifierException(
                    "AuthorizeException occurred while " + "converting " + dso.getTypeText() + "/" + dso.getID()
                            + " using crosswalk " + this.CROSSWALK_NAME + ".",
                    ae, DOIIdentifierException.CONVERSION_ERROR);
        } catch (CrosswalkException ce)
        {
            log.error("Caught an CrosswalkException while reserving a DOI (" + doi + ") for DSO with type "
                    + dso.getType() + " and ID " + dso.getID() + ". Won't reserve the doi.", ce);
            throw new DOIIdentifierException(
                    "CrosswalkException occurred while " + "converting " + dso.getTypeText() + "/" + dso.getID()
                            + " using crosswalk " + this.CROSSWALK_NAME + ".",
                    ce, DOIIdentifierException.CONVERSION_ERROR);
        } catch (IOException ioe)
        {
            throw new RuntimeException(ioe);
        } catch (SQLException se)
        {
            throw new RuntimeException(se);
        }

        addHeadInfo(root, dso);
        try {
            addDoi(context, root, doi, dso);
        } catch (SQLException ex) {
            log.debug("Error while ingesting doi into crossref xml", ex);
            throw new RuntimeException(ex);
        }

        // if this is the first time we are registering a resource,
        // we have to make sure we remove the DOI we've added for the XML generation
        // or we'll end up with two DOI metadata entries (as after successful
        // registration a DOI metadata field is added.
        if (!hasBeenRegisteredBefore)
        {
            Metadatum[] doiMd = dso.getDC("identifier", Item.ANY, Item.ANY);
            dso.clearDC("identifier", Item.ANY, Item.ANY);
            if (doiMd != null && doiMd.length > 0)
            {
                for (Metadatum md : doiMd)
                {
                    if (md.value != null && !md.value.startsWith(DOI.RESOLVER))
                    {
                        dso.addDC(md.element, md.qualifier, md.language, md.value);
                    }
                }
            }
        }

        DoiResponse resp = this.sendDepositRequest(doi, root);
        log.debug("Deposit DOI response: " + resp);
    }

    /**
     * Crossref requires any request to have an integer version of date and time
     * upon which it decides if a record needs to be updated if it already exits.
     * 
     * @param root
     * @return
     */
    protected Element addTimestamp(Element root)
    {
        Element identifier = new Element("timestamp", CROSSREF_SCHEMA);
        identifier.addContent(new Date().getTime() + "");
        return root.getChild("head", Namespace.getNamespace(CROSSREF_SCHEMA)).addContent(identifier);
    }

    /**
     * Crossref requires a batch id. This method adds a batch id created out of the handle
     * of a DSpaceObject to the head element.
     * 
     * @param root The root of the XML.
     * @return The parent of the new created element (<code>head</code>)
     */
    protected Element addBatchId(Element root, DSpaceObject dso)
    {
        Element batchId = new Element("doi_batch_id", CROSSREF_SCHEMA);
        batchId.addContent(dso.getHandle().replaceAll("/", "_"));
        log.info("Set id: " + dso.getHandle().replaceAll("/", "_"));
        return root.getChild("head", Namespace.getNamespace(CROSSREF_SCHEMA)).addContent(0, batchId);
    }

    /**
     * Adds the <code>head</code> information to the XML document to be submitted
     * to Crossref. Specifically, this method adds timestamp, batch id, depositor,
     * and registrant information. Depositor and registrant values should be configured
     * in dspace.cfg.
     * 
     * @param root The root of the XML.
     * @param dso The DSpaceObject for which the XML is being created.
     * @return The parent of the new created element (<code>head</code>)
     */
    protected Element addHeadInfo(Element root, DSpaceObject dso)
    {
        // add batch id
        addBatchId(root, dso);

        // add timestamp
        addTimestamp(root);

        // add depositor element
        Element depositor = new Element("depositor", CROSSREF_SCHEMA);
        Element depositorName = new Element("depositor_name", CROSSREF_SCHEMA);
        depositorName.addContent(getDepositorName());
        Element depositorEmail = new Element("email_address", CROSSREF_SCHEMA);
        depositorEmail.addContent(getDepositorEmail());

        depositor.addContent(depositorName);
        depositor.addContent(depositorEmail);

        root.getChild("head", Namespace.getNamespace(CROSSREF_SCHEMA)).addContent(depositor);

        // add registrant
        Element registrant = new Element("registrant", CROSSREF_SCHEMA);
        registrant.addContent(getRegistrant());

        return root.getChild("head", Namespace.getNamespace(CROSSREF_SCHEMA)).addContent(registrant);
    }
    
    /**
     * Add the doi to the xml that will be send to CrossRef. We expect the XML to contain exactly one node doi_data
     * into which we will ingest the doi information.
     * @param c org.dspace.core.Context
     * @param root The XML into which the doi information shall be ingested.
     * @param doi The doi to ingest.
     * @param dso The DSpaceObject for which the DOI is registered, to create the URL to which the DOI shall point to.
     * @return The XML with the ingested DOI information.
     * @throws SQLException
     */
    protected Element addDoi(Context c, Element root, String doi, DSpaceObject dso) throws SQLException {
        // create the information to ingest (doi and url)
        Element doiInformation = new Element("doi", CROSSREF_SCHEMA);
        doiInformation.addContent(doi.substring(SCHEME.length() -1));
        Element resource = new Element("resource", CROSSREF_SCHEMA);
        resource.addContent(HandleManager.resolveToURL(c, dso.getHandle()));
    
        // find the node into which the information shall be ingested
        List<Element> nodes = null;
        try {
            // JDOM seem to need the heading dot. Without it the expression doesn't finde the node, even if that is surprising.
            XPath path = XPath.newInstance(".//x:doi_data");
            // jdom 1 does not support a default namespace, add it explicitly.
            path.addNamespace("x", root.getNamespaceURI());
            nodes = path.selectNodes(root);
        } catch (JDOMException ex) {
            log.error("JDOMException: ", ex);
            throw new RuntimeException(ex);
        }
    
        if (nodes.size() != 1 ) {
            log.error("Trying to create invalid XML for Crossref. There should be exactly one node 'doi_data'.");
            throw new IllegalStateException("Trying to create invalid XML for Crossref. There should be one node 'doi_data' only.");
        }
        
        Element doiData = nodes.get(0);
        doiData.addContent(doiInformation);
        doiData.addContent(resource);
        
        if (log.isDebugEnabled()) {
            Format format = Format.getCompactFormat();
            format.setEncoding("UTF-8");
            XMLOutputter xout = new XMLOutputter(format);
            log.debug("Ingested " + doi + ":");
            log.debug(xout.outputString(root));
        }
        return root;
    }

    /**
     * This method gets the doi from the passed object if it already has one.
     * 
     * @param dso the object to extract the doi from
     * @return the doi of the object or null
     */
    protected String extractDOI(DSpaceObject dso)
    {
        Metadatum[] doiMd = dso.getDC("identifier", Item.ANY, Item.ANY);
        if (doiMd != null && doiMd.length > 0)
        {
            for (Metadatum md : doiMd)
            {
                if (md.value != null && md.value.startsWith(DOI.RESOLVER))
                {
                    return md.value;
                }
            }
        }
        return null;
    }

    @Override
    public void updateMetadata(Context context, DSpaceObject dso, String doi) throws DOIIdentifierException
    {
        // check if doi is registered for another object
        if (!ConfigurationManager.getBooleanProperty("identifier.doi.crossref.override", false)
                && !this.isDOIRegistered(context, dso, doi) && this.isDOIRegistered(context, doi))
        {
            log.warn("Trying to update metadata for DOI {}, that is reserved for another dso.", doi);
            throw new DOIIdentifierException("Trying to update metadta for a DOI that is reserved for another object.",
                    DOIIdentifierException.DOI_ALREADY_EXISTS);
        }

        // We can simply make another deposit request to Crossref for updating metadata
        this.registerDOI(context, dso, doi);

    }

    protected DoiResponse sendDepositRequest(String doi, Element metadataRoot) throws DOIIdentifierException
    {
        Format format = Format.getCompactFormat();
        format.setEncoding("UTF-8");
        XMLOutputter xout = new XMLOutputter(format);
        return sendDepositRequest(doi, xout.outputString(new Document(metadataRoot)));
    }

    protected DoiResponse sendDepositRequest(String doi, String metadata) throws DOIIdentifierException
    {
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme(SCHEME).setHost(HOST).setPath(DEPOSIT_PATH);

        HttpPost httppost = null;
        try
        {
            httppost = new HttpPost(uribuilder.build());
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(TIMEOUT).setSocketTimeout(TIMEOUT).build();
            httppost.setConfig(requestConfig);
        } catch (URISyntaxException e)
        {
            log.error("The URL we constructed to deposit a new DOI"
                    + "produced a URISyntaxException. Please check the configuration parameters!");
            log.error(String.format("The URL was {}.", SCHEME + "://" + HOST + DEPOSIT_PATH));
            throw new RuntimeException("The URL we constructed to deposti a new DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!", e);
        }

        File requestXml = null;
        try
        {
            requestXml = File.createTempFile("doiRequest", "xml");
            FileUtils.write(requestXml, metadata);
        } catch (IOException e)
        {
            throw new RuntimeException("Could not create temporary file to be send to Crossref.", e);
        }

        log.debug("Sending the following xml: \n" + metadata);

        // assemble request content:
        HttpEntity reqEntity = null;
        try
        {
            reqEntity = MultipartEntityBuilder.create()
                    .addBinaryBody(FORM_DATA_FILE, metadata.getBytes(), ContentType.TEXT_XML, "requestData.xml")
                    .addTextBody(FORM_DATA_USER, this.getUsername(CFG_USER))
                    .addTextBody(FORM_DATA_PASSWORD, this.getPassword(CFG_PASSWORD)).build();
            httppost.setEntity(reqEntity);

            return sendHttpRequest(httppost, doi);
        } finally
        {
            // release resources
            try
            {
                EntityUtils.consume(reqEntity);
            } catch (IOException ioe)
            {
                log.error("Caught an IOException while releasing an HTTPEntity:" + ioe.getMessage(), ioe);
            }
        }
    }

    /**
     * Handle status code responses / errors from the API
     * @param statusCode
     * @param doi
     * @param content
     * @return
     * @throws DOIIdentifierException
     */
    protected void handleErrorCodes(int statusCode, String doi, String content) throws DOIIdentifierException
    {
        switch (statusCode) {
            case (200):
            case (201):
            case (204):
            case (301):
            case (302): {
                // No error, return (this check is necessary since we include a 'default' case
                return;
            }
            // we get a 401 if we forgot to send credentials or if the username
            // and password did not match.
            case (400): {
                log.info("Crossref did not accept the sent request.");
                log.info(String.format("The response was: %s", content));
                throw new DOIIdentifierException("Crossref did not accept the sent request.",
                        DOIIdentifierException.BAD_REQUEST);
            }
            case (401): {
                log.info("We were unable to authenticate against the DOI registry agency.");
                log.info(String.format("The response was: %s", content));
                throw new DOIIdentifierException("Cannot authenticate at the "
                        + "DOI registry agency. Please check if username " + "and password are set correctly.",
                        DOIIdentifierException.AUTHENTICATION_ERROR);
            }

            // We get a 403 Forbidden if we are managing a DOI that belongs to
            // another party or if there is a login problem.
            case (403): {
                log.info(String.format("Managing a DOI (%s) was forbidden by the DOI registration agency: %s", doi,
                        content));
                throw new DOIIdentifierException("There was an error during submission. DOI could not be registered.",
                        DOIIdentifierException.REGISTRATION_ERROR);
            }

            // 404 Not Found is returned for a DOI that doesn't exist so we must not throw an exception
            case (404): {
                log.info("No DOI found: " + doi);
                return;
            }

            // 500 is documented and signals an internal server error
            case (500): {
                log.warn("Caught an http status code 500 while managing DOI " + "{}. Message was: " + content);
                throw new DOIIdentifierException(
                        "Crossref API has an internal error. " + "It is temporarily impossible to manage DOIs. "
                                + "Further information can be found in DSpace log file.",
                        DOIIdentifierException.INTERNAL_ERROR);
            }

            // 504 is gateway timeout and means we should increase our client-side timeout
            case (504): {
                log.warn("Caught an http status code 504 (gateway timeout) while managing DOI " + "{}. Message was: " + content);
                throw new DOIIdentifierException(
                        "Crossref API took too long responding to our request. " +
                                "Increase the CROSSREF_TIMEOUT in the identifier services spring configuration. " +
                                "Further information can be found in DSpace log file.",
                        DOIIdentifierException.INTERNAL_ERROR);
            }
            // Catch all other http status code in case we forgot one.
            default: {
                log.warn(String.format("While registering the DOI %s, we got a http status code %s and the message \"%s\".",
                        doi, statusCode, content));
                throw new DOIIdentifierException(
                        "Unable to parse an answer from Crossref API. Please have a look into the DSpace logs.",
                        DOIIdentifierException.BAD_ANSWER);
            }
        }
    }

    protected void configureRequest(DefaultHttpClient httpclient)
    {
        // nothing to do here, authentication is sent in body
    }

    /**
     * This method chewcks if a DOI is already registered. CrossRef does not
     * automatically update their DOI endpoint "https://api.crossref.org/works",
     * hence we can't trust a negative response.
     * 
     * Instead we try to resolve the DOI. If a 302 Found is returned, we know the
     * DOI is registered. We have to make sure to use https://dx.doi.org though or a
     * 304 will be returned.
     */
    @Override
    protected DoiResponse sendDOIGetRequest(String doi) throws DOIIdentifierException
    {
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme(DOI_RESOLVER_SCHEME).setHost(DOI_RESOLVER_HOST)
                .setPath("/" + doi.substring(DOI.SCHEME.length()));

        HttpGet httpget = null;
        try
        {
            // we don't want to follow the redirect, or we don't know if just the handle
            // doesn't resolve or the DOI
            HttpParams params = new BasicHttpParams();
            HttpClientParams.setRedirecting(params, false);

            httpget = new HttpGet(uribuilder.build());
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(TIMEOUT).setSocketTimeout(TIMEOUT).build();
            httpget.setConfig(requestConfig);

            httpget.setParams(params);

        } catch (URISyntaxException e)
        {
            log.error("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!");
            log.error("The URL was {}.",
                    DOI_RESOLVER_SCHEME + "://" + DOI_RESOLVER_HOST + "/" + doi.substring(DOI.SCHEME.length()));
            throw new RuntimeException("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!", e);
        }

        DoiResponse doiResponse = null;

        try {
            doiResponse = sendHttpRequest(httpget, doi);
        } catch(DOIIdentifierException e) {
            // 404s are actually OK for this check since it just means "not found" but we should log an info line
            // anyway just to help diagnose problems in case there's a bad URL or network issue going on
            if (e.getCode() == DOIIdentifierException.DOI_DOES_NOT_EXIST) {
                log.info("404 NOT FOUND for a 'get DOI' request, which probably means the DOI isn't registered," +
                        "but may indicate a bad DOI scheme, host, or path or network error.");
            } else {
                // If it's not a 404, rethrow
                throw new DOIIdentifierException(e);
            }
        }

        return doiResponse;
    }

    private void validateItemMetadata(DSpaceObject dso, String type) throws DOIIdentifierException {
        if ("article".equals(type)) {
            // Journal articles must have a journal ISSN or DOI to provide as journal metadata
            String[] issnFieldParts = parentIssnField.split("\\.");
            String[] doiFieldParts = parentDoiField.split("\\.");
            if (issnFieldParts.length < 2) {
                log.error("Invalid ISSN field configured in spring. Should be eg. dc.identifier.issn");
                throw new DOIIdentifierException(DOIIdentifierException.CONVERSION_ERROR);
            }
            if (doiFieldParts.length < 2) {
                log.error("Invalid parent (journal) DOI field configured in spring. Should be eg. local.identifier.doi");
                throw new DOIIdentifierException(DOIIdentifierException.CONVERSION_ERROR);
            }
            // Get ISSNs
            Metadatum[] issns = dso.getMetadata(issnFieldParts[0], issnFieldParts[1],
                    (issnFieldParts.length == 3 ? issnFieldParts[2] : null), Item.ANY);
            // Get DOIs
            Metadatum[] dois = dso.getMetadata(doiFieldParts[0], doiFieldParts[1],
                    (doiFieldParts.length == 3 ? doiFieldParts[2] : null), Item.ANY);
            if (issns.length == 0 && dois.length == 0) {
                log.error("article type must supply at least one ISSN or DOI to identify the parent publication.");
                throw new DOIIdentifierException(DOIIdentifierException.CONVERSION_ERROR);
            }
        }
    }

    @Override
    protected int getDoiGetSuccessStatusCode()
    {
        // We are resolving the DOI and if that is successful, we'll get a 302 Found
        // back
        return 302;
    }

    @Override
    protected void extractUrlFromResponse(DoiResponse doiResponse, HttpResponse response)
    {
        log.debug("Getting handle");
        if (response != null && response.containsHeader(REDIRECT_HEADER))
        {
            doiResponse.setUrl(response.getFirstHeader(REDIRECT_HEADER).getValue());
            log.debug("set to: " + doiResponse.getUrl());
        }
    }
}
