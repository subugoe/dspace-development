/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * 
 * This class serves as base class for connectors to DOI providers. 
 * It offers a default implementation for checking if a resource has been already
 * registered, methods for getting username and password for a registry, and
 * a method to send HTTP requests. The following abstract methods need to be 
 * implemented for the before mentioned methods to function properly.
 * 
 * <ul>
 *  <li><code>getDsoUrl</code>: get the URL for a specific DSpaceObject</li>
 *  <li><code>sendHttpRequest</code>: sends a request to a DOI registry</li>
 *  <li><code>handleErrorCodes</code>: handles error codes returned from a HTTP request</li>
 *  <li><code>configureRequest</code>: for additional configurations of a request</li>
 *  <li><code>sendDOIGetRequest</code>: sends a GET request for a DOI</li>
 *  <li><code>getDoiGetSuccessStatusCode</code>: returns the HTTP code that is being returned if a DOI was found</li>
 *  <li><code>extractHandleFromResponse</code>: extract the handle for a registered DOI from the HTTP response of the registry.</li> 
 * </ul>
 * 
 * 
 * @author Julia Damerow, Pascal-Nicolas Becker
 *
 */
public abstract class AbstractDoiConnector implements DOIConnector {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected ConfigurationService configurationService;

    protected String USERNAME;
    protected String PASSWORD;

    /**
     * Name of crosswalk to convert metadata into DataCite Metadata Scheme. Set by
     * spring dependency injection.
     */
    protected String CROSSWALK_NAME;

    /**
     * Set the name of the dissemination crosswalk used to convert the metadata into
     * DOI Metadata Schema. Used by spring dependency injection.
     * 
     * @param CROSSWALK_NAME The name of the dissemination crosswalk to use. This
     *                       crosswalk must be configured in dspace.cfg.
     */
    @Required
    public void setDisseminationCrosswalkName(String CROSSWALK_NAME)
    {
        this.CROSSWALK_NAME = CROSSWALK_NAME;
    }

    @Autowired
    @Required
    public void setConfigurationService(ConfigurationService configurationService)
    {
        this.configurationService = configurationService;
    }

    /**
     * This method retrieves the property with the given name from the 
     * configuration and sets <code>this.USERNAME</code>. Typically, this should
     * be the username for the DOI service that should be contacted.
     * 
     * @param cfgUser The name of the property to load provided in dspace.cfg.
     * @return The value of the property.
     */
    protected String getUsername(String cfgUser)
    {
        if (null == this.USERNAME)
        {
            this.USERNAME = this.configurationService.getProperty(cfgUser);
            if (null == this.USERNAME)
            {
                throw new RuntimeException(
                        "Unable to load username from " + "configuration. Cannot find property " + cfgUser + ".");
            }
        }
        return this.USERNAME;
    }

    /**
     * This method retrieves the property with the given name from the 
     * configuration and sets <code>this.PASSWORD</code>. Typically, this should
     * be the password for the DOI service that should be contacted.
     * 
     * @param cfgPassword The name of the property to load provided in dspace.cfg.
     * @return The value of the property.
     */
    protected String getPassword(String cfgPassword)
    {
        if (null == this.PASSWORD)
        {
            this.PASSWORD = this.configurationService.getProperty(cfgPassword);
            if (null == this.PASSWORD)
            {
                throw new RuntimeException(
                        "Unable to load password from " + "configuration. Cannot find property " + cfgPassword + ".");
            }
        }
        return this.PASSWORD;
    }

    /**
     * Default implementation that checks if a given DOI is already registered.
     */
    @Override
    public boolean isDOIRegistered(Context context, String doi)
            throws DOIIdentifierException
    {
        return isDOIRegistered(context, null, doi);
    }
    
    /**
     * Default implementation that checks if a given DOI is already registered for 
     * the provided {@link DSpaceObject}. This method calls the following abstract methods:
     * <ul>
     *  <li><code>sendDOIGetRequest</code><li>
     *  <li><code>getDoiGetSuccessStatusCode</code></li>
     *  <li><code>getDsoUrl</code></li>
     *  
     * </ul>
     */
    @Override
    public boolean isDOIRegistered(Context context, DSpaceObject dso, String doi) throws DOIIdentifierException
    {
        DoiResponse response = sendDOIGetRequest(doi);

        if (response != null) {
            log.debug("Check reg'd DOI response: " + response);
        } else {
            log.error("Null or invalid response when checking for an existing / registered DOI " + doi);
            throw new DOIIdentifierException(DOIIdentifierException.BAD_ANSWER);
        }
        
        if (response.getStatusCode() == getDoiGetSuccessStatusCode()) {
            // Do we check if doi is reserved generally or for a specified dso?
            if (null == dso)
            {
                return true;
            }

            // DataCite returns the URL the DOI currently points to.
            // To ensure that the DOI is registered for a specified dso it
            // should be sufficient to compare the URL DataCite returns with
            // the URL of the dso.
            String doiUrl = response.getHandle();
            if (null == doiUrl)
            {
                log.error("Received a status code 200 without a response content. DOI: {}.", doi);
                throw new DOIIdentifierException("Received a http status code 200 without a response content.",
                        DOIIdentifierException.BAD_ANSWER);
            }

            String dsoUrl = getDsoUrl(dso, context);

            if (null == dsoUrl)
            {
                // the handle of the dso was not found in our db?!
                log.error("The HandleManager was unable to find the handle " + "of a DSpaceObject in the database!?! "
                        + "Type: {} ID: {}", dso.getTypeText(), dso.getID());
                throw new RuntimeException(
                        "The HandleManager was unable to " + "find the handle of a DSpaceObject in the database!");
            }

            return (dsoUrl.equals(doiUrl));
        }
        // Status Code 204 "No Content" stands for a known DOI without URL.
        // A DOI that is known but does not have any associated URL is
        // reserved but not registered yet.
        // The above is true for DataCite but might no be true when resolving a DOI?
        if (response.getStatusCode() == 204) {
            // we know it is reserved, but we do not know for which object.
            // won't add this to the cache.
            return false;
        }
        // 404 "Not Found" means DOI is neither reserved nor registered.
        if (response.getStatusCode() == 404) {
            return false;
        }
        // Catch all other http status code in case we forgot one.
        log.warn(
                    "While checking if the DOI {} is registered, we got a "
                            + "http status code {} and the message \"{}\".",
                    new String[] { doi, Integer.toString(response.getStatusCode()), response.getContent() });
            throw new DOIIdentifierException(
                    "Unable to parse an answer from " + "DataCite API. Please have a look into DSpace logs.",
                    DOIIdentifierException.BAD_ANSWER);
        
        
    }

    /**
     * This method should return the URL that would map to the URL registered
     * with a DOI. The <code>isDOIRegistered</code> method will call this method
     * to check if the URL returned in the response from the DOI service is equal
     * to this URL.
     * 
     * @param dso The object to get a URL for.
     * @param context The context for this object.
     * @return The URL of the provided DSpaceObject.
     */
    protected abstract String getDsoUrl(DSpaceObject dso, Context context);
    
    
    /**
     * Sends a request to a DOI registry.
     * This method calls the following abstract methods that need to be implemented in
     * subclasses:
     * <ul>
     *  <li><code>handleErrorCodes</code></li>
     *  <li><code>extractHandleFromResponse</code></li>
     * </ul>  
     * 
     * @param req The request to be sent.
     * @param doi The DOI for which a request is made.
     * @return A {@link DoiResponse} object containing information about the registry response.
     * 
     * @throws DOIIdentifierException if an error occurs during request submission.
     */
    protected DoiResponse sendHttpRequest(HttpUriRequest req, String doi) throws DOIIdentifierException
    {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        configureRequest(httpclient);

        HttpEntity entity = null;
        try
        {
            HttpResponse response = httpclient.execute(req);

            StatusLine status = response.getStatusLine();
            int statusCode = status.getStatusCode();
            String content = null;
            entity = response.getEntity();
            if (null != entity)
            {
                content = EntityUtils.toString(entity, "UTF-8");
            }

            // While debugging it can be useful to see whitch requests are send:

//             log.debug("Going to send HTTP request of type " + req.getMethod() + ".");
//             log.debug("Will be sent to " + req.getURI().toString() + ".");
//             if (req instanceof HttpEntityEnclosingRequestBase)
//             {
//                 log.debug("Request contains entity!");
//                 HttpEntityEnclosingRequestBase reqee = (HttpEntityEnclosingRequestBase) req;
//                 if (reqee.getEntity() instanceof StringEntity)
//                 {
//                     StringEntity se = (StringEntity) reqee.getEntity();
//                     try {
//                         BufferedReader br = new BufferedReader(new InputStreamReader(se.getContent()));
//                         String line = null;
//                         while ((line = br.readLine()) != null)
//                         {
//                             log.debug(line);
//                         }
//                         log.info("----");
//                     } catch (IOException ex) {
//                         
//                     }
//                 }
//             } else {
//                 log.debug("Request contains no entity!");
//             }
//             log.debug("The request got http status code {}.", Integer.toString(statusCode));
//             if (null == content)
//             {
//                 log.debug("The response did not contain any answer.");
//             } else {
//                 log.debug("DOI Registry says: {}", content);
//             }

            handleErrorCodes(statusCode, doi, content);
            
            DoiResponse doiResponse = new DoiResponse(statusCode, content);
            extractHandleFromResponse(doiResponse, response);
            return doiResponse;
        } catch (IOException e)
        {
            log.warn("Caught an IOException: " + e.getMessage());
            throw new RuntimeException(e);
        } finally
        {
            try
            {
                // Release any ressources used by HTTP-Request.
                if (null != entity)
                {
                    EntityUtils.consume(entity);
                }
            } catch (IOException e)
            {
                log.warn("Can't release HTTP-Entity: " + e.getMessage());
            }
        }
    }

    /**
     * This method should handle the specific error codes the API returns. E.g. 
     * DataCite returns 401, 403, and 500, while Crossref also returns 400 at times.
     * @param statusCode The received status code.
     * @param doi The DOI used in the request submission.
     * @param content The content of the response.
     * @throws DOIIdentifierException
     */
    protected abstract void handleErrorCodes(int statusCode, String doi, String content) throws DOIIdentifierException;

    /**
     * This method can configure the request to the DOI service to for example add
     * authentication.
     * 
     * @param httpclient
     */
    protected abstract void configureRequest(DefaultHttpClient httpclient);

    /**
     * This method should send a GET request to find out if a DOI is already reserved.
     * Depending on the used DOI registry, this method has different implementations. However,
     * handling of the response can be for the most part the same for different registries,
     * hence, this method is called in <code>isDOIRegistered</code>.
     * 
     * @param doi The DOI in question
     * @return A {@link DoiResponse} object that contains a status code, the content
     *      of the request, and the handle of the DOI if the DOI is already registered.
     * @throws DOIIdentifierException
     */
    protected abstract DoiResponse sendDOIGetRequest(String doi) throws DOIIdentifierException;

    /**
     * This method should return the expected HTTP code that will indicate if a DOI is
     * registered.
     * 
     * @return
     */
    protected abstract int getDoiGetSuccessStatusCode();
    
    /** 
     * This method should extract the handle for an object from the response of the DOI
     * registry. This method is called in the <code>sendHttpRequest</code> method. Different
     * registries will return the registred handle differently, hence this abstract method.
     * 
     * @param doiResponse The object that encapsulates the response from the DOI registry.
     * @param response The {@link HttpResponse} object from the registry.
     */
    protected abstract void extractHandleFromResponse( DoiResponse doiResponse, HttpResponse response);

    
    protected static class DoiResponse {
        private final int statusCode;
        private final String content;
        private String handle;

        public DoiResponse(int statusCode, String content)
        {
            this.statusCode = statusCode;
            this.content = content;
        }

        public int getStatusCode()
        {
            return this.statusCode;
        }

        public String getContent()
        {
            return this.content;
        }

        public String getHandle()
        {
            return this.handle;
        }
        public void setHandle(String handle)
        {
            this.handle = handle;
        }

        public String toString() {
            return "status=" + statusCode + ", handle=" +
                    (handle == null ? "null" : handle) + ", content=" + content;
        }
    }

}
