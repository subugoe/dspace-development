/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.catalogue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

/**
 * Manager class to retrieve, add, edit, and delete messages in xml message
 * catalogues.
 * 
 * This class retrieves any catalogue location information from the
 * {@link MessageCatalogueProvider}.
 * 
 * @author Julia Damerow
 *
 */
public class CatalogueMessageManager {

    private static final Logger log = Logger.getLogger(CatalogueMessageManager.class);

    private static final String MESSAGE_NODE_NAME = "message";

    /**
     * Method to get all message keys and values from an XML message file.
     * 
     * @param catalogue The catalogue id as generated by the
     *                  {@link MessageCatalogueProvider}
     * @return A map of all message keys and values in a give catalogue.
     * @throws CatalogueDoesNotExistException Thrown if no catalogue can be found
     *                                        for the provided catalogue id.
     * @throws MessageLoadingException thrown if message could not be loaded due to
     *                  a JDOM parsing exception.
     */
    public static Map<String, String> getMessages(String catalogue) throws CatalogueDoesNotExistException, MessageLoadingException {
        String cataloguePath = MessageCatalogueProvider.getCatalogue(catalogue);
        log.debug("Getting messages for: " + cataloguePath);
        if (cataloguePath == null) {
            throw new CatalogueDoesNotExistException("Catalogue " + catalogue + " does not exist.");
        }
        Document doc;
        try {
            doc = getDocument(cataloguePath); 
        } catch (RuntimeException e) {
            throw new MessageLoadingException(e);
        }
        
        Element root = doc.getRootElement();
        Namespace ns = doc.getRootElement().getNamespace();
        List<?> messages;
        if (ns != null) {
            messages = root.getChildren(MESSAGE_NODE_NAME, ns);
        } else {
            messages = root.getChildren(MESSAGE_NODE_NAME);
        }

        // some files use a default namespace
        if (messages.isEmpty()) {

        }

        Map<String, String> messageMap = new HashMap<>();
        for (Object message : messages) {
            messageMap.put(((Element) message).getAttributeValue("key"), getValueAsString((Element) message));
        }

        return messageMap;
    }
    
    /**
     * Method that returns a string representation of a node and all
     * its children.
     * @param messageNode The node to turn into a string
     * @return a piece of XML representing the passed node
     */
    private static String getValueAsString(Element messageNode) {
        StringBuffer sb = new StringBuffer();
        
        Iterator<?> content = messageNode.getContent().iterator();
        while(content.hasNext()) {
            Object contentElem = content.next();
            // we can be sure we'll only have nodes and texts here
            if (contentElem instanceof Text) {
                sb.append(((Text)contentElem).getText());
            } else if (contentElem instanceof Element) {
                Element child = (Element)contentElem;
                
                String contentStr = getValueAsString(child);
                String startTag = getTagAsText(child, contentStr.isEmpty());
                
                sb.append(startTag);
                if (!contentStr.isEmpty()) {
                    sb.append(contentStr);
                    sb.append(getEndTagAsText(child));
                }
            }
            // everything else we ignore
        }
        String result = sb.toString();
        return result.trim();
    }
    
    /**
     * Returns a tag as string.
     * @param tag The tag to be turned into a string.
     * @param isEmpty a boolean indicating if the passed tag is empty or not
     * @return a string representation of a tag (e.g. <b>, <br/> or <a href="...">)
     */
    private static String getTagAsText(Element tag, boolean isEmpty) {
        StringBuffer tagString = new StringBuffer();
        tagString.append("<");
        tagString.append(tag.getName());
        List<?> attrs = tag.getAttributes();
        if (attrs != null) {
            for (Object attr : attrs) {
                tagString.append(" ");
                tagString.append(((Attribute)attr).getName());
                tagString.append("=\"");
                tagString.append(((Attribute)attr).getValue());
                tagString.append("\"");
            }
        }
        
        if (isEmpty) {
            tagString.append(" /");
        }
        tagString.append(">");
        return tagString.toString();
    }
    
    /**
     * Returns the end tag for a tag.
     * @param tag The tag an end tag is required for.
     * @return e.g. </a> or </i>
     */
    private static String getEndTagAsText(Element tag) {
        StringBuffer tagString = new StringBuffer();
        tagString.append("</");
        tagString.append(tag.getName());
        tagString.append(">");
        return tagString.toString();
    }

    /**
     * Method to store a message. This method assumes that the message already
     * exists and simply updates its value.
     * 
     * @param catalogue    The catalogue id as generated by the
     *                     {@link MessageCatalogueProvider}
     * @param messageKey   The string key of a message.
     * @param messageValue The message value
     * @throws CatalogueDoesNotExistException Thrown if no catalogue can be found
     *                                        for the provided catalogue id.
     * @throws MessageStorageException        Thrown if the message could not be
     *                                        found in the catalogue.
     */
    public static void storeMessage(String catalogue, String messageKey, String messageValue)
            throws CatalogueDoesNotExistException, MessageStorageException {
        String cataloguePath = MessageCatalogueProvider.getCatalogue(catalogue);
        if (cataloguePath == null) {
            throw new CatalogueDoesNotExistException("Catalogue " + catalogue + " does not exist.");
        }
        Document doc = getDocument(cataloguePath);
        Element messageNode = getMessageNode(messageKey, doc);
        
        if (messageNode != null) {
            Collection<Content> nodes = getMessageAsNodes(messageValue);
            if (nodes == null) {
                throw new MessageStorageException("Invalid XML.");
            }
            messageNode.setContent(nodes);
            writeMessageFile(cataloguePath, doc);
        } else {
            throw new MessageStorageException("Message node does not exist.");
        }
    }

    /**
     * Method to add a new message to the given catalogue. If the message key
     * already exists, its value is simply updated.
     * 
     * @param catalogue    The catalogue id as generated by the
     *                     {@link MessageCatalogueProvider}
     * @param messageKey   The string key of a message.
     * @param messageValue The message value
     * @throws CatalogueDoesNotExistException Thrown if no catalogue can be found
     *                                        for the provided catalogue id.
     */
    public static void addMessage(String catalogue, String messageKey, String messageValue)
            throws CatalogueDoesNotExistException {
        String cataloguePath = MessageCatalogueProvider.getCatalogue(catalogue);
        if (cataloguePath == null) {
            throw new CatalogueDoesNotExistException("Catalogue " + catalogue + " does not exist.");
        }
        Document doc = getDocument(cataloguePath);
        Element root = doc.getRootElement();

        Element messageNode = getMessageNode(messageKey, doc);
        // we'll just update the message in case the node already exists
        if (messageNode == null) {
            String nsUri = doc.getRootElement().getNamespaceURI();
            if (nsUri != null && !nsUri.trim().isEmpty()) {
                messageNode = new Element("message", nsUri);
            } else {
                messageNode = new Element("message");
            }
            messageNode.setAttribute("key", messageKey);

            root.addContent(messageNode);
        }
        messageNode.setContent(getMessageAsNodes(messageValue));
        writeMessageFile(cataloguePath, doc);
    }
    
    /**
     * This method creates JDOM nodes for a piece of XML. It will create a dummy
     * root node and returns all children of that root node as {@link Content} list.
     * @param messageText The piece of XML to be turned into Content objects (e.g.
     *      <code>Hello <i>World</i></code>.
     * @return A collection of {@link Content} objects (without a parent) so that they 
     *      can be added as children to another node.
     */
    private static Collection<Content> getMessageAsNodes(String messageText) {
        messageText = StringEscapeUtils.UNESCAPE_XML.translate(messageText);
        String validXML = "<dummy>" + messageText + "</dummy>";
        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(new StringReader(validXML));
        } catch (JDOMException | IOException e1) {
            log.error("Could not read messages file.", e1);
            return null;
        }

        return doc.getRootElement().removeContent();
    }

    /**
     * Method to delete a message from a given catalogue.
     * 
     * @param catalogue  The catalogue id as generated by the
     *                   {@link MessageCatalogueProvider}
     * @param messageKey The string key of a message.
     * @throws CatalogueDoesNotExistException Thrown if no catalogue can be found
     *                                        for the provided catalogue id.
     * @throws MessageDeletionException       thrown if a message could not be
     *                                        deleted
     */
    public static void deleteMessage(String catalogue, String messageKey)
            throws CatalogueDoesNotExistException, MessageDeletionException {
        String cataloguePath = MessageCatalogueProvider.getCatalogue(catalogue);
        if (cataloguePath == null) {
            throw new CatalogueDoesNotExistException("Catalogue " + catalogue + " does not exist.");
        }
        Document doc = getDocument(cataloguePath);
        Element messageNode = getMessageNode(messageKey, doc);
        log.debug("Removing node: " + messageNode);
        if (messageNode == null) {
            // nothing to do here since message key doesn't exist
            return;
        }

        boolean success = doc.getRootElement().removeContent(messageNode);
        if (!success) {
            throw new MessageDeletionException("Message could not be deleted.");
        }
        writeMessageFile(cataloguePath, doc);
    }

    /**
     * Writes the given XML document to a file with the provided path.
     * 
     * @param cataloguePath The path to the catalogue file.
     * @param doc           The XML document to write.
     */
    private static void writeMessageFile(String cataloguePath, Document doc) {
        XMLOutputter xmlOutput = new XMLOutputter();

        // Since we don't want white space before and after HTML tags, we can't pretty
        // print the XML. For existing message the existing format is used; new messages
        // are being added without pretty print.
        // Set unix-style newlines for line separators (the default is \r\n
        // carriage-return style)
        Format format = Format.getRawFormat();
        format.setLineSeparator("\n");
        xmlOutput.setFormat(format);
        try {
            xmlOutput.output(doc, new FileWriter(cataloguePath));
            log.info("Save XML file: " + cataloguePath);
        } catch (IOException e) {
            log.error("Could not write XML file.", e);
            return;
        }
    }

    /**
     * Returns the XML element with the provided message key.
     * 
     * @param messageKey The messageKey property of the desired message element.
     * @param doc        the XML document containing all messages.
     * @return The XML element with the provided message key or null.
     */
    private static Element getMessageNode(String messageKey, Document doc) {
        XPath xpath;
        Element messageNode = null;
        try {
            xpath = XPath.newInstance(String.format("message[@key=\"%s\"]", messageKey));
            String nsUri = doc.getRootElement().getNamespaceURI();
            
            List<?> results;
            // some XML files have a namespace
            if (nsUri != null && !nsUri.trim().isEmpty()) {
                xpath = XPath.newInstance(String.format("ns:message[@key=\"%s\"]", messageKey));
                xpath.addNamespace("ns", doc.getRootElement().getNamespaceURI());
            }

            results = xpath.selectNodes(doc.getRootElement());

            if (results != null && !results.isEmpty()) {
                messageNode = (Element) results.get(0);
            }
        } catch (JDOMException e) {
            log.error("Can't get message.", e);
            return null;
        }
        return messageNode;
    }

    /**
     * Returns the XML {@link Document} for the given catalogue.
     * 
     * @param cataloguePath path to calalogue file.
     * @return XML {@link Document} for the given catalogue
     */
    private static Document getDocument(String cataloguePath) {
        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(new File(cataloguePath));
        } catch (JDOMException | IOException e1) {
            log.error("Could not read messages file.", e1);
            return null;
        }

        return doc;
    }
}
