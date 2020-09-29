/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.catalogue;

import java.io.File;
import java.io.FilenameFilter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

/**
 * 
 * This class provides access to all message files. By default it will look for
 * messages*.xml files in dspace.dir/webapps/xmlui/i18n. If additional files
 * should be aded those can be added to the dspace config property
 * "message.catalogues.locations" as a comma separated list (e.g. for
 * development purposes).
 * 
 * @author Julia Damerow
 *
 */
public class MessageCatalogueProvider {
    private static final Logger log = Logger.getLogger(MessageCatalogueProvider.class);

    private static Map<String, String> catalogues;

    /**
     * Method to load all catalogues. Calling this method will recreate the map of
     * catalogues. This means any catalogue added before will not be available any
     * longer unless it is still in one of the configured locations.
     * 
     * This method is called internally to load catalogues. External classes should
     * only call this method if the configuration settings have changed, new
     * catalogues have been added, or old ones have been deleted while DSpace is
     * running.
     * 
     * Also, this method does not load any file contents. It's just loads a map that
     * maps unique ids (hashes) to the absolute paths of all messages files.
     */
    public static synchronized void loadCatalogues() {
        String dspaceDir = ConfigurationManager.getProperty("dspace.dir");

        catalogues = new HashMap<String, String>();
        StringBuffer catalogueLocation = new StringBuffer();
        catalogueLocation.append(dspaceDir);
        catalogueLocation.append(File.separator);
        catalogueLocation.append("webapps");
        catalogueLocation.append(File.separator);
        catalogueLocation.append("xmlui");
        catalogueLocation.append(File.separator);
        catalogueLocation.append("i18n");

        // load message files from the default location (dspace.dir +
        // /webapps/xmlui/i18n)
        File messagesLocationFolder = new File(catalogueLocation.toString());
        if (messagesLocationFolder.exists() && messagesLocationFolder.isDirectory()) {
            loadMessageFiles(messagesLocationFolder);
        }

        // add any additional files
        String additionalCatalogueLocationsProp = ConfigurationManager.getProperty("message.catalogues.locations");
        if (additionalCatalogueLocationsProp != null) {
            String[] additionalCatalogueLocations = additionalCatalogueLocationsProp.split(",");
            for (String addLoc : additionalCatalogueLocations) {
                File catalogueLocFile = new File(addLoc);
                if (!catalogueLocFile.exists()) {
                    continue;
                }
                if (catalogueLocFile.isFile()) {
                    try {
                        addLocation(addLoc);
                    } catch (NoSuchAlgorithmException e) {
                        log.error("Could not add: " + addLoc);
                        log.error("Could not add message file.", e);
                    }
                } else if (catalogueLocFile.isDirectory()) {
                    loadMessageFiles(catalogueLocFile);
                }
            }
        }
    }

    /**
     * Loads all message files in the given directory.
     * 
     * @param messagesLocationFolder {@link File} of the directory containing
     *                               message files. All files of the form
     *                               messages*.xml will be loaded. If the given file
     *                               is not a directory the methods returns.
     */
    private static void loadMessageFiles(File messagesLocationFolder) {
        if (!messagesLocationFolder.isDirectory()) {
            return;
        }

        File[] messageFiles = messagesLocationFolder.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                // we want to list all files of the form messages_*.xml
                return name.startsWith("messages") && name.endsWith(".xml");
            }
        });

        if (messageFiles != null) {
            for (File messageFile : messageFiles) {
                try {
                    addLocation(messageFile.getAbsolutePath());
                } catch (NoSuchAlgorithmException e) {
                    log.error("Could not add: " + messageFile.getAbsolutePath());
                    ;
                    log.error("Could not add message file.", e);
                }
            }
        }
    }

    /**
     * Returns the absolute path to a message file given its hash.
     * 
     * @param hash The hash used to identify a message file. Use
     *             {@link getCatalogues()} to get a map of all message files and
     *             their hashes.
     * 
     * @return The absolute path to a message file.
     */
    public static String getCatalogue(String hash) {
        if (catalogues == null) {
            loadCatalogues();
        }
        return catalogues.get(hash);
    }

    /**
     * Returns a maps a unique hashes (used as identifiers) and message files.
     * 
     * @return an unmodifiable map of the form { hash: path }
     */
    public static Map<String, String> getCatalogues() {
        if (catalogues == null) {
            loadCatalogues();
        }
        return Collections.unmodifiableMap(catalogues);
    }

    /**
     * Adss a new message file to the catalogues map.
     * 
     * @param file Messaeg file to be added to the map.
     * @throws NoSuchAlgorithmException if the MD5 algorithm is not available.
     */
    private static void addLocation(String file) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        catalogues.put(DatatypeConverter.printHexBinary(messageDigest.digest(file.getBytes())), file);

    }

}
