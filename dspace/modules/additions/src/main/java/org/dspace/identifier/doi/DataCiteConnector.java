/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Iterator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.DOI;
import org.dspace.identifier.doi.DOIConnector;
import org.dspace.identifier.doi.DOIIdentifierException;
import org.dspace.services.ConfigurationService;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 * @author Pascal-Nicolas Becker
 */
public class DataCiteConnector extends AbstractDoiConnector
{

    private static final Logger log = LoggerFactory.getLogger(DataCiteConnector.class);
    
    // Configuration property names
    static final String CFG_USER = "identifier.doi.user";
    static final String CFG_PASSWORD = "identifier.doi.password";
    
    /**
     * Stores the scheme used to connect to the DataCite server. It will be set
     * by spring dependency injection.
     */
    protected String SCHEME;
    /**
     * Stores the hostname of the DataCite server. Set by spring dependency
     * injection.
     */
    protected String HOST;
    
    /**
     * Path on the DataCite server used to generate DOIs. Set by spring
     * dependency injection.
     */
    protected String DOI_PATH;
    /**
     * Path on the DataCite server used to register metadata. Set by spring
     * dependency injection.
     */
    protected String METADATA_PATH;
    /** 
     * DisseminationCrosswalk to map local metadata into DataCite metadata.
     * The name of the crosswalk is set by spring dependency injection using
     * {@link setDisseminationCrosswalk(String) setDisseminationCrosswalk} which
     * instantiates the crosswalk.
     */
    protected DisseminationCrosswalk xwalk;
    
    public DataCiteConnector()
    {
        this.xwalk = null;
        this.USERNAME = null;
        this.PASSWORD = null;
    }
    
    /**
     * Used to set the scheme to connect the DataCite server. Used by spring
     * dependency injection.
     * @param DATACITE_SCHEME Probably https or http.
     */
    @Required
    public void setDATACITE_SCHEME(String DATACITE_SCHEME)
    {
        this.SCHEME = DATACITE_SCHEME;
    }

    /**
     * Set the hostname of the DataCite server. Used by spring dependency
     * injection.
     * @param DATACITE_HOST Hostname to connect to register DOIs (f.e. test.datacite.org).
     */
    @Required
    public void setDATACITE_HOST(String DATACITE_HOST)
    {
        this.HOST = DATACITE_HOST;
    }
    
    /**
     * Set the path on the DataCite server to register DOIs. Used by spring
     * dependency injection.
     * @param DATACITE_DOI_PATH Path to register DOIs, f.e. /doi.
     */
    @Required
    public void setDATACITE_DOI_PATH(String DATACITE_DOI_PATH)
    {
        if (!DATACITE_DOI_PATH.startsWith("/"))
        {
            DATACITE_DOI_PATH = "/" + DATACITE_DOI_PATH;
        }
        if (!DATACITE_DOI_PATH.endsWith("/"))
        {
            DATACITE_DOI_PATH = DATACITE_DOI_PATH + "/";
        }
        
        this.DOI_PATH = DATACITE_DOI_PATH;
    }
    
    /**
     * Set the path to register metadata on DataCite server. Used by spring
     * dependency injection.
     * @param DATACITE_METADATA_PATH Path to register metadata, f.e. /mds.
     */
    @Required
    public void setDATACITE_METADATA_PATH(String DATACITE_METADATA_PATH)
    {
        if (!DATACITE_METADATA_PATH.startsWith("/"))
        {
            DATACITE_METADATA_PATH = "/" + DATACITE_METADATA_PATH;
        }
        if (!DATACITE_METADATA_PATH.endsWith("/"))
        {
            DATACITE_METADATA_PATH = DATACITE_METADATA_PATH + "/";
        }
        
        this.METADATA_PATH = DATACITE_METADATA_PATH;
    }
    
    
    
    protected void prepareXwalk()
    {
        if (null != this.xwalk)
            return;
        
        this.xwalk = (DisseminationCrosswalk) PluginManager.getNamedPlugin(
                DisseminationCrosswalk.class, this.CROSSWALK_NAME);
        
        if (this.xwalk == null)
        {
            throw new RuntimeException("Can't find crosswalk '"
                    + CROSSWALK_NAME + "'!");
        }
    }

    
    public boolean isDOIReserved(Context context, String doi)
            throws DOIIdentifierException
    {
        return isDOIReserved(context, null, doi);
    }
    
    @Override
    public boolean isDOIReserved(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException
    {
        // get mds/metadata/<doi>
        DoiResponse resp = this.sendMetadataGetRequest(doi);
    
        switch (resp.getStatusCode())
        {
            // 200 -> reserved
            // if (200 && dso != null) -> compare url (out of response-content) with dso
            case (200) :
            {
                // Do we check if doi is reserved generally or for a specified dso?
                if (null == dso)
                {
                    return true;
                }
                
                // check if doi belongs to dso
                String doiHandle = null;
                try
                {
                    doiHandle = extractAlternateIdentifier(context, resp.getContent());
                }
                catch (SQLException e)
                {
                    throw new RuntimeException(e);
                }
                
                if (null == doiHandle)
                {
                    // we were unable to find a handle belonging to our repository
                    return false;
                }
                
                String dsoHandle = dso.getHandle();
                if (null == dsoHandle)
                {
                    return false;
                }
                return dsoHandle.equals(doiHandle);
            }
                
            // 404 "Not Found" means DOI is neither reserved nor registered.
            case (404) :
            {
                return false;
            }
                
            // 410 GONE -> DOI is set inactive
            // that means metadata has been deleted
            // it is unclear if the doi has been registered before or only reserved.
            // it is unclear for which item it has been reserved
            // we will handle this as if it reserved for an unknown object.
            case (410) :
            {
                if (null == dso)
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }
                
            // Catch all other http status code in case we forgot one.
            default :
            {
                log.warn("While checking if the DOI {} is registered, we got a "
                        + "http status code {} and the message \"{}\".",
                        new String[]
                        {
                            doi, Integer.toString(resp.getStatusCode()),
                            resp.getContent()
                        });
                throw new DOIIdentifierException("Unable to parse an answer from "
                        + "DataCite API. Please have a look into DSpace logs.",
                        DOIIdentifierException.BAD_ANSWER);
            }
        }
    }
    
    @Override
    public void deleteDOI(Context context, String doi)
            throws DOIIdentifierException
    {
        if (!isDOIReserved(context, doi))
            return;
        
        // delete mds/metadata/<doi>
        DoiResponse resp = this.sendMetadataDeleteRequest(doi);
        switch(resp.getStatusCode())
        {
            //ok
            case (200) :
            {
                return;
            }
            // 404 "Not Found" means DOI is neither reserved nor registered.
            case (404) :
            {
                log.error("DOI {} is at least reserved, but a delete request "
                        + "told us that it is unknown!", doi);
                return;
            }
            // Catch all other http status code in case we forgot one.
            default :
            {
                log.warn("While deleting metadata of DOI {}, we got a "
                        + "http status code {} and the message \"{}\".",
                        new String[] {doi, Integer.toString(resp.getStatusCode()), resp.getContent()});
                throw new DOIIdentifierException("Unable to parse an answer from "
                        + "DataCite API. Please have a look into DSpace logs.",
                        DOIIdentifierException.BAD_ANSWER);
            }
        }
    }

    @Override
    public void reserveDOI(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException
    {   
        // check if DOI is reserved at the registration agency
        if (this.isDOIReserved(context, doi))
        {
            // if doi is registered for this object we still should check its
            // status in our database (see below).
            // if it is registered for another object we should notify an admin
            if (!this.isDOIReserved(context, dso, doi))
            {
                log.warn("DOI {} is reserved for another object already.", doi);
                throw new DOIIdentifierException(DOIIdentifierException.DOI_ALREADY_EXISTS);
            }
            // the DOI is reserved for this Object. We use {@code reserveDOI} to
            // send metadata updates, so don't return here!
        }

        this.prepareXwalk();
        
        if (!this.xwalk.canDisseminate(dso))
        {
            log.error("Crosswalk " + this.CROSSWALK_NAME 
                    + " cannot disseminate DSO with type " + dso.getType() 
                    + " and ID " + dso.getID() + ". Giving up reserving the DOI "
                    + doi + ".");
            throw new DOIIdentifierException("Cannot disseminate "
                    + dso.getTypeText() + "/" + dso.getID()
                    + " using crosswalk " + this.CROSSWALK_NAME + ".",
                    DOIIdentifierException.CONVERSION_ERROR);
        }
        
        Element root = null;
        try
        {
            root = xwalk.disseminateElement(dso);
        }
        catch (AuthorizeException ae)
        {
            log.error("Caught an AuthorizeException while disseminating DSO "
                    + "with type " + dso.getType() + " and ID " + dso.getID()
                    + ". Giving up to reserve DOI " + doi + ".", ae);
            throw new DOIIdentifierException("AuthorizeException occurred while "
                    + "converting " + dso.getTypeText() + "/" + dso.getID()
                    + " using crosswalk " + this.CROSSWALK_NAME + ".", ae,
                    DOIIdentifierException.CONVERSION_ERROR);
        }
        catch (CrosswalkException ce)
        {
            log.error("Caught an CrosswalkException while reserving a DOI ("
                    + doi + ") for DSO with type " + dso.getType() + " and ID " 
                    + dso.getID() + ". Won't reserve the doi.", ce);
            throw new DOIIdentifierException("CrosswalkException occurred while "
                    + "converting " + dso.getTypeText() + "/" + dso.getID()
                    + " using crosswalk " + this.CROSSWALK_NAME + ".", ce,
                    DOIIdentifierException.CONVERSION_ERROR);
        }
        catch (IOException ioe)
        {
            throw new RuntimeException(ioe);
        }
        catch (SQLException se)
        {
            throw new RuntimeException(se);
        }
        
        String metadataDOI = extractDOI(root);
        if (null == metadataDOI)
        {
            // The DOI will be saved as metadata of dso after successful
            // registration. To register a doi it has to be part of the metadata
            // sent to DataCite. So we add it to the XML we'll send to DataCite
            // and we'll add it to the DSO after successful registration.
            root = addDOI(doi, root);
        }
        else if (!metadataDOI.equals(doi.substring(DOI.SCHEME.length())))
        {
            // FIXME: that's not an error. If at all, it is worth logging it.
            throw new DOIIdentifierException("DSO with type " + dso.getTypeText()
                    + " and id " + dso.getID() + " already has DOI "
                    + metadataDOI + ". Won't reserve DOI " + doi + " for it.");
        }
        
        // send metadata as post to mds/metadata
        DoiResponse resp = this.sendMetadataPostRequest(doi, root);
        
        switch (resp.getStatusCode())
        {
            // 201 -> created / ok
            case (201) :
            {
                return;
            }
            // 400 -> invalid XML
            case (400) :
            {
                log.warn("DataCite was unable to understand the XML we send.");
                log.warn("DataCite Metadata API returned a http status code "
                        +"400: " + resp.getContent());
                Format format = Format.getCompactFormat();
                format.setEncoding("UTF-8");
                XMLOutputter xout = new XMLOutputter(format);
                log.info("We send the following XML:\n" + xout.outputString(root));
                throw new DOIIdentifierException("Unable to reserve DOI " + doi 
                        + ". Please inform your administrator or take a look "
                        +" into the log files.", DOIIdentifierException.BAD_REQUEST);
            }
            // Catch all other http status code in case we forgot one.
            default :
            {
                log.warn("While reserving the DOI {}, we got a http status code "
                        + "{} and the message \"{}\".", new String[]
                        {doi, Integer.toString(resp.getStatusCode()), resp.getContent()});
                throw new DOIIdentifierException("Unable to parse an answer from "
                        + "DataCite API. Please have a look into DSpace logs.",
                        DOIIdentifierException.BAD_ANSWER);
            }
        }
    }
    
    @Override
    public void registerDOI(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException
    {
        // check if the DOI is already registered online
        if (this.isDOIRegistered(context, doi))
        {
            // if it is registered for another object we should notify an admin
            if (!this.isDOIRegistered(context, dso, doi))
            {
                // DOI is reserved for another object
                log.warn("DOI {} is registered for another object already.", doi);
                throw new DOIIdentifierException(DOIIdentifierException.DOI_ALREADY_EXISTS);
            }
            // doi is registered for this object, we're done
            return;
        }
        else
        {
            // DataCite wants us to reserve a DOI before we can register it
            if (!this.isDOIReserved(context, dso, doi))
            {
                // check if doi is already reserved for another dso
                if (this.isDOIReserved(context, doi))
                {
                    log.warn("Trying to register DOI {}, that is reserved for "
                            + "another dso.", doi);
                    throw new DOIIdentifierException("Trying to register a DOI "
                            + "that is reserved for another object.",
                            DOIIdentifierException.DOI_ALREADY_EXISTS);
                }
                
                // the DOIIdentifierProvider should catch and handle this
                throw new DOIIdentifierException("You need to reserve a DOI "
                        + "before you can register it.",
                        DOIIdentifierException.RESERVE_FIRST);
            }
        }

        // send doi=<doi>\nurl=<url> to mds/doi
        DoiResponse resp = null;
        try
        {
            resp = this.sendDOIPostRequest(doi, 
                    HandleManager.resolveToURL(context, dso.getHandle()));
        }
        catch (SQLException e)
        {
            log.error("Caught SQL-Exception while resolving handle to URL: "
                    + e.getMessage());
            throw new RuntimeException(e);
        }
        
        switch(resp.getStatusCode())
        {
            // 201 -> created/updated -> okay
            case (201) :
            {
                return;
            }
            // 400 -> wrong domain, wrong prefix, wrong request body
            case (400) :
            {
                log.warn("We send an irregular request to DataCite. While "
                        + "registering a DOI they told us: " + resp.getContent());
                throw new DOIIdentifierException("Currently we cannot register "
                        + "DOIs. Please inform the administrator or take a look "
                        + " in the DSpace log file.",
                        DOIIdentifierException.BAD_REQUEST);
            }
            // 412 Precondition failed: DOI was not reserved before registration!
            case (412) :
            {
                log.error("We tried to register a DOI {} that was not reserved "
                        + "before! The registration agency told us: {}.", doi,
                        resp.getContent());
                throw new DOIIdentifierException("There was an error in handling "
                        + "of DOIs. The DOI we wanted to register had not been "
                        + "reserved in advance. Please contact the administrator "
                        + "or take a look in DSpace log file.",
                        DOIIdentifierException.RESERVE_FIRST);
            }
            // Catch all other http status code in case we forgot one.
            default :
            {
                log.warn("While registration of DOI {}, we got a http status code "
                        + "{} and the message \"{}\".", new String[]
                        {doi, Integer.toString(resp.getStatusCode()), resp.getContent()});
                throw new DOIIdentifierException("Unable to parse an answer from "
                        + "DataCite API. Please have a look into DSpace logs.",
                        DOIIdentifierException.BAD_ANSWER);
            }
        }
    }
    
    @Override
    public void updateMetadata(Context context, DSpaceObject dso, String doi) 
            throws DOIIdentifierException
    { 
        // check if doi is reserved for another object
        if (!this.isDOIReserved(context, dso, doi) && this.isDOIReserved(context, doi))
        {
            log.warn("Trying to update metadata for DOI {}, that is reserved"
                    + " for another dso.", doi);
            throw new DOIIdentifierException("Trying to update metadta for "
                    + "a DOI that is reserved for another object.",
                    DOIIdentifierException.DOI_ALREADY_EXISTS);
        }
        // We can use reserveDOI to update metadata. Datacite API uses the same
        // request for reservartion as for updating metadata.
        this.reserveDOI(context, dso, doi);
    }
    
    protected DoiResponse sendDOIPostRequest(String doi, String url)
            throws DOIIdentifierException
    {
        // post mds/doi/
        // body must contaion "doi=<doi>\nurl=<url>}n"
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme(SCHEME).setHost(HOST).setPath(DOI_PATH);
        
        HttpPost httppost = null;
        try
        {
            httppost = new HttpPost(uribuilder.build());
        }
        catch (URISyntaxException e)
        {
            log.error("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!");
            log.error("The URL was {}.", SCHEME + "://" + HOST +
                    DOI_PATH + "/" + doi.substring(DOI.SCHEME.length()));
            throw new RuntimeException("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!", e);
        }
        
        // assemble request content:
        HttpEntity reqEntity = null;
        try
        {
            String req = "doi=" + doi.substring(DOI.SCHEME.length()) + "\n" + "url=" + url + "\n";
            ContentType contentType = ContentType.create("text/plain", "UTF-8");
            reqEntity = new StringEntity(req, contentType);
            httppost.setEntity(reqEntity);
            
            return sendHttpRequest(httppost, doi);
        }
        finally
        {
            // release ressources
            try
            {
                EntityUtils.consume(reqEntity);
            }
            catch (IOException ioe)
            {
               log.info("Caught an IOException while releasing a HTTPEntity:"
                       + ioe.getMessage());
            }
        }
    }
    
    
    protected DoiResponse sendMetadataDeleteRequest(String doi)
            throws DOIIdentifierException
    {
        // delete mds/metadata/<doi>
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme(SCHEME).setHost(HOST).setPath(METADATA_PATH
                + doi.substring(DOI.SCHEME.length()));
        
        HttpDelete httpdelete = null;
        try
        {
            httpdelete = new HttpDelete(uribuilder.build());
        }
        catch (URISyntaxException e)
        {
            log.error("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!");
            log.error("The URL was {}.", SCHEME + "://" + HOST +
                    DOI_PATH + "/" + doi.substring(DOI.SCHEME.length()));
            throw new RuntimeException("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!", e);
        }
        return sendHttpRequest(httpdelete, doi);
    }
    
    protected DoiResponse sendDOIGetRequest(String doi)
            throws DOIIdentifierException
    {
        return sendGetRequest(doi, DOI_PATH);
    }
    
    protected DoiResponse sendMetadataGetRequest(String doi)
            throws DOIIdentifierException
    {
        return sendGetRequest(doi, METADATA_PATH);
    }
    
    protected DoiResponse sendGetRequest(String doi, String path)
            throws DOIIdentifierException
    {
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme(SCHEME).setHost(HOST).setPath(path
                + doi.substring(DOI.SCHEME.length()));
        
        HttpGet httpget = null;
        try
        {
            httpget = new HttpGet(uribuilder.build());
        }
        catch (URISyntaxException e)
        {
            log.error("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!");
            log.error("The URL was {}.", SCHEME + "://" + HOST +
                    DOI_PATH + "/" + doi.substring(DOI.SCHEME.length()));
            throw new RuntimeException("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!", e);
        }
        return sendHttpRequest(httpget, doi);
        
    }
    
    protected DoiResponse sendMetadataPostRequest(String doi, Element metadataRoot)
            throws DOIIdentifierException
    {
        Format format = Format.getCompactFormat();
        format.setEncoding("UTF-8");
        XMLOutputter xout = new XMLOutputter(format);
        return sendMetadataPostRequest(doi, xout.outputString(new Document(metadataRoot)));
    }
    
    protected DoiResponse sendMetadataPostRequest(String doi, String metadata)
            throws DOIIdentifierException
    {
        // post mds/metadata/
        // body must contain metadata in DataCite-XML.
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme(SCHEME).setHost(HOST).setPath(METADATA_PATH);
        
        HttpPost httppost = null;
        try
        {
            httppost = new HttpPost(uribuilder.build());
        }
        catch (URISyntaxException e)
        {
            log.error("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!");
            log.error("The URL was {}.", SCHEME + "://" + HOST +
                    DOI_PATH + "/" + doi.substring(DOI.SCHEME.length()));
            throw new RuntimeException("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!", e);
        }
        
        // assemble request content:
        HttpEntity reqEntity = null;
        try
        {
            ContentType contentType = ContentType.create("application/xml", "UTF-8");
            reqEntity = new StringEntity(metadata, contentType);
            httppost.setEntity(reqEntity);
            
            return sendHttpRequest(httppost, doi);
        }
        finally
        {
            // release ressources
            try
            {
                EntityUtils.consume(reqEntity);
            }
            catch (IOException ioe)
            {
               log.info("Caught an IOException while releasing an HTTPEntity:"
                       + ioe.getMessage());
            }
        }
    }
    
    protected void handleErrorCodes(int statusCode, String doi, String content) throws DOIIdentifierException
    {
        switch (statusCode)
        {
            // we get a 401 if we forgot to send credentials or if the username
            // and password did not match.
            case (401) :
            {
                log.info("We were unable to authenticate against the DOI registry agency.");
                log.info("The response was: {}", content);
                throw new DOIIdentifierException("Cannot authenticate at the "
                        + "DOI registry agency. Please check if username "
                        + "and password are set correctly.",
                        DOIIdentifierException.AUTHENTICATION_ERROR);
            }

            // We get a 403 Forbidden if we are managing a DOI that belongs to
            // another party or if there is a login problem.
            case (403) :
            {
                log.info("Managing a DOI ({}) was prohibited by the DOI "
                        + "registration agency: {}", doi, content);
                throw new DOIIdentifierException("We can check, register or "
                        + "reserve DOIs that belong to us only.",
                        DOIIdentifierException.FOREIGN_DOI);
            }


            // 500 is documented and signals an internal server error
            case (500) :
            {
                log.warn("Caught an http status code 500 while managing DOI "
                        +"{}. Message was: " + content);
                throw new DOIIdentifierException("DataCite API has an internal error. "
                        + "It is temporarily impossible to manage DOIs. "
                        + "Further information can be found in DSpace log file.",
                        DOIIdentifierException.INTERNAL_ERROR);
            }
        }
        
    }
    
    protected void configureRequest(DefaultHttpClient httpclient) {
        httpclient.getCredentialsProvider().setCredentials(
                new AuthScope(HOST, 443),
                new UsernamePasswordCredentials(this.getUsername(CFG_USER), this.getPassword(CFG_PASSWORD)));
    }
    
    protected int getDoiGetSuccessStatusCode() {
        return 200;
    }

    // returns null or handle
    protected String extractAlternateIdentifier(Context context, String content)
    throws SQLException, DOIIdentifierException
    {
        if (content == null)
        {
            return null;
        }
        
        // parse the XML
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = null;
        try
        {
            doc = saxBuilder.build(new ByteArrayInputStream(content.getBytes("UTF-8")));
        }
        catch (IOException ioe)
        {
            throw new RuntimeException("Got an IOException while reading from a string?!", ioe);
        }
        catch (JDOMException jde)
        {
            throw new DOIIdentifierException("Got a JDOMException while parsing "
                    + "a response from the DataCite API.", jde,
                    DOIIdentifierException.BAD_ANSWER);
        }
        
        String handle = null;
        
        Iterator<Element> it = doc.getDescendants(new ElementFilter("alternateIdentifier"));
        while (handle == null && it.hasNext())
        {
            Element alternateIdentifier = it.next();
            try
            {
                handle = HandleManager.resolveUrlToHandle(context,
                        alternateIdentifier.getText());
            }
            catch (SQLException e)
            {
                throw e;
            }
        }

        return handle;
    }
    
    protected String extractDOI(Element root) {
        Element doi = root.getChild("identifier", root.getNamespace());
        return (null == doi) ? null : doi.getTextTrim();
    }

    protected Element addDOI(String doi, Element root) {
        if (null != extractDOI(root))
        {
            return root;
        }
        Element identifier = new Element("identifier", "http://datacite.org/schema/kernel-2.2");
        identifier.setAttribute("identifierType", "DOI");
        identifier.addContent(doi.substring(DOI.SCHEME.length()));
        return root.addContent(0, identifier);
    }

    @Override
    protected void extractHandleFromResponse(DoiResponse doiResponse, HttpResponse response)
    {
        // DataCite returns the handle in the body (which is in the content now)
        doiResponse.setHandle(doiResponse.getContent());
    }
    
    protected String getDsoUrl(DSpaceObject dso, Context context) {
        try
        {
            return HandleManager.resolveToURL(context, dso.getHandle());
        } catch (SQLException e)
        {
            log.error("Error in database connection: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
