# Caveats

## Deleting records
Crossref does not support deactivating or deleting DOIs that have been registered already. Therefore, it is important to only withdraw items in DSpace and not to delete any DSpace items as the DOI will always forward to the registered resource URL.

## Changing record types

Similarly, Crossref currently does not support changing the type of a previously registered item. While other information such as title, author, or year can be updated, the type of a item cannot be changed once an item has been registered.

## Registering items
When registering new items, a couple of things should be kept in mind:

* Author Accepted Manuscript versions or copies of Versions of Record of items that have already been published through a journal and have an assigned DOI cannot be registered with Crossref if the depositor is not the owner of the journal ISSN as Crossref will not allow registration of articles within that journal.
* In the case of preprints that should be assigned a DOI, those should be submitted as `posted content` with `type="preprint"`. That means that applications and services that reuses the metadata will not show this as an article, but as a preprint, even if it is a peer-reviewed version of the article's content.

## Complex metadata schema
To achieve good metadata quality within Crossref's metadata registration, you should customize the XSLTs we use to transform metadata stored in DSpace into XML send to Crossref. Crossref's metadata schema differentiates heavily depending on the content's type. When customizing the XSLT, you will notice that the there are almost multiple schemas depending on the content's type. We documented the metadata mapping in more detail below.

## DataCite or Crossref
While it definitely is possible, we haven't developped support to register some DOIs with DataCite and others with Crossref from a single installation of DSpace. Unless you know what you're doing, you should register DOIs either with Crossref or with DataCite.


# Configuration

DOI Registration with Crossref can be enabled by adding/commenting in the following DOI Connector in identifier-service.xml (located in dspace/config/spring/api/identifier-service.xml):

```xml
<bean id="org.dspace.identifier.doi.DOIConnector"
        class="org.dspace.identifier.doi.CrossRefDoiConnector"
        scope="singleton">
    <property name='CROSSREF_SCHEME' value='https'/>
    <property name='CROSSREF_HOST' value='api.crossref.org'/>
    <property name='CROSSREF_DEPOSIT_PATH' value='/v2/deposits' />
    <property name='disseminationCrosswalkName' value="Crossref" />
</bean>
```

Additionally the following configuration properties have to be set in dspace.cfg:

* `identifier.doi.user`: the username to be used with the Crossref API.
* `identifier.doi.crossref.password`: the password to be used with the Crossref API.
* `identifier.doi.prefix`: the DOI prefix assigned by Crossref.
* `identifier.doi.crossref.depositor`: the name of the depositor (e.g. name of university depositing new records); this information will be used when making deposit requests; from crossref documentation: “Information about the organization submitting DOI metadata to Crossref.”
* `identifier.doi.crossref.depositor.email`: email address to which notifications can be send; while the API endpoint that is currently used does not send notifications this might change in the future, and the email address should therefore be a monitored one.
* `identifier.doi.crossref.registrant`: the owner of the registered entry; from Crossref documentation: “The organization that owns the information being registered.”

## Metadata-Mapping
There are default metadata mappings for the default DSpace item types. These can be adjusted and replaced as described below. The submitted XML files have to adhere to [Crossref’s metadata scheme](https://data.crossref.org/reports/help/schema_doc/4.4.2/index.html). Example XMLs can be found [here](https://www.crossref.org/xml-samples/).

A couple of things to note for the XSLTs that map DSpace metadata to Crossref metadata:

* If you are using different metadata schemas than the default DSpace ones, you will need to adjust the XSLTs that are provided.
* To map DSpace metadata to Crossref metadata, you need to provide XSLTs. There can be at most one XSLT per DSpace type. All XSLTs have to be located in the `config/crosswalks/crossref` folder. They have to adhere to the following naming convention `DIM2Crossref_<type_lowercase>`, e.g. `DIM2Crossref_book.xsl`. If the type contains spaces, those should be replaced with `_`, e.g. the XSLT for book chapters should be named `DIM2Crossref_book_chapter.xsl`. There are a couple of default XSLTs for certain types, which are described below.
* Whenever there is no XSLT named `DIM2Crossref_<type_lowercase>` for a type, the XLST `DIM2Crossref_other.xsl` is used. This creates a very basic metadata XML that submits contributors, title, publication year, and DOI information as type posted_content type="other".
* All XSLTs should have the same basic format:

```xml
<doi_batch version="4.4.2" xmlns="http://www.crossref.org/schema/4.4.2"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.crossref.org/schema/4.4.2 http://www.crossref.org/schema/deposit/crossref4.4.2.xsd">

    <head>
      <!-- This section will be filled programmatically. Do not remove! -->
    </head>

    <body>
    <!-- type specific content -->
    </bod>
</doi_batch>
```

* The metadata to submit to Crossref has two parts: a `head` and a `body`. The head contains some metadata about the request and is the same for all types. The body is different depending on the type. Every XSLT should have an empty `head` element, directly under `doi_batch`. The CrossrefConnector will fill the `head` with the required information (taken from the `dspace.cfg` file and automatically generated data). When the XML to be sent to Crossref is being generated, a timestamp, depositor information, and a batch id are programmatically added to this head. Do not add depositor, timestamp, or doi_batch_id in the XSLT.

```xml
<head>
  <!-- This section will be filled programmatically. Do not remove! -->
</head>
```

* The DOI part is the same for all types (just with different parent nodes) and can be generate using this template:

```xml
<xsl:template match="dspace:field[@mdschema='dc' and @element='identifier' and starts-with(., 'http://dx.doi.org/')]">
  <doi_data>
     <doi>
        <xsl:value-of select="substring(., 19)"/>
     </doi>
     <resource>
        <xsl:value-of select="//dspace:field[@mdschema='dc' and @element='identifier' and @qualifier='uri' and not(starts-with(., 'http://dx.doi.org/'))]"/>
     </resource>
  </doi_data>
</xsl:template>
```

* All XSLTs assume that you are using the prefix http://dx.doi.org/ for all DOIs. If this is not the case, the XSLTs need to be adjusted.


### Books
Example: [https://www.crossref.org/xml-samples/monograph.xml](https://www.crossref.org/xml-samples/monograph.xml)


| Node | Attributes | | Notes|
|----|----|----|----|
| book | book_type (required) | Possible values: edited_book, monograph, reference, other| Current mappings: Book => monograph|
| book/book_metadata | language | Language attribute should be only a two letter code (e.g. en not en_US). | Currently the language attribute is not mapped due to mismatch of language codes in DSpace and Crossref (e.g. no en_US in Crossref and no other option). |
| book/book\_metadata/titles/title | | Children: title, subtitle, original\_language\_title (with attribute language), subtitle | |
| book/book\_metadata/publication\_date/year | | Year of publication | |
| book/book\_metadata/isbn | media_type (optional): printed, electronic | "In very limited circumstances a book may be deposited without an ISBN, in which case the noisbn element must be supplied to explicitly declare that an ISBN is not accidentily omitted. Great care should be taken when choossing to use noisbn since it may adversely effect matching. This provision is primarily being made to allow for the deposit of DOIs for historical volumes that are difficult to obtain ISBNs." | |
| book/book\_metadata/publisher/publisher\_name | | Name of publisher | |
| book/book\_metadata/doi\_data | Two children: doi, resource | | |
| book/book\_metadata/contributors/person\_name | Children: given\_name, surname, alt\_name Attribute contributor_role with one of the following values: author, editor, chair, reviewer, reviewer-assistant, stats-reviewer, reviewer-external, reader, translator | Author information; only `surname` is required. | |

### Book Chapters

| Node | Attributes | | Notes|
|----|----|----|----|
| book | book\_type (required) | Possible values: edited_book, monograph, reference, other | Current mappings: Book => monograph |
| book/book\_metadata | language | Language attribute should be only a two letter code (e.g. en not `en_US`). | Currently the language attribute is not mapped due to mismatch of language codes in DSpace and Crossref (e.g. no `en_US` in Crossref and no `other` option). |
| book/book\_metadata/titles/title | | Children: title, subtitle, original\_language\_title (with attribute language), subtitle | Currently, if there is an `isPartOf` metadata relationship, its value will be used, otherwise it’ll will be set to “(:unas) unassigned”. |
| book/book\_metadata/publication_date/year | | Year of publication | |
| book/book\_metadata/isbn | media\_type (optional): printed, electronic | "In very limited circumstances a book may be deposited without an ISBN, in which case the noisbn element must be supplied to explicitly declare that an ISBN is not accidentily omitted. Great care should be taken when choossing to use noisbn since it may adversely effect matching. This provision is primarily being made to allow for the deposit of DOIs for historical volumes that are difficult to obtain ISBNs." | Currently, if there is an ISBN number, it will be used. Otherwise, `<noisbn reason="monograph" />` will be submitted. |
| book/book\_metadata/publisher/publisher\_name | | Name of publisher | |
| book/book\_metadata/doi\_data | Two children: doi, resource | | |
| book/book\_metadata/contributors/person\_name | Children: given\_name, surname, alt\_name Attribute: contributor\_role, author, editor, chair, reviewer, reviewer-assistant, stats-reviewer, reviewer-external, reader, translator | Author information; only surname is required. | While this is listed in the schema, Crossref doesn’t seem to like it when it is part of the book metadata and content item. So, it is omitted in the XSL. |
| book/content\_item | | | |
| book/content\_item/contributors/person\_name | Children: given\_name, surname, alt\_name Attribute: contributor\_role with one of the following values: author, editor, chair, reviewer, reviewer-assistant, stats-reviewer, reviewer-external, reader, translator. Attribute: sequence with one of the values: first, additional | There needs to be a first authors (`sequence=first`) or Crossref is unhappy. | Currently, the first author in the list is being used as first author. All others are additional. |
| book/content\_item/titles/title | | Children of titles: title, subtitle, original\_language\_title (with attribute language), subtitle | |
| book/content\_item/publication\_date/year | | Year of publication | |
| book/content\_item/doi\_data | Two children: doi, resource | | |

### Article
Journal articles have to be submitted as part of a journal for which the depositor can assign new DOIs.

| Node | Attributes | | Notes|
|----|----|----|----|
| journal | | | Parent for new journal articles |
| journal/journal\_metadata | language | Language attribute should be only a two letter code (e.g. en not en_US). | Currently the language attribute is not mapped due to mismatch of language codes in DSpace and Crossref (e.g. no en_US in Crossref and no other option). |
| journal/journal\_metadata/full\_title | | required; title of journal an article belongs to. | Currently, if there is an `isPartOf` metadata relationship (dcterms.ispartof, dc.relation.ispartof), its value will be used, otherwise it'll will be set to "(:unas) unassigned". |
| journal/journal\_metadata/abbrev\_title | "full\_title and abbrev\_title must both be submitted even if they are identical" | |
| journal/journal\_metadata/issn | | To successfully register an article, either a journal ISSN needs to be provided or a DOI for the journal. | By default an ISSN will be expected in the metadata field dc.identifier.issn |
| journal/journal\_metadata/doi\_data | Two children: doi, resource | To successfully register an article, either a journal ISSN needs to be provided or a DOI for the journal. | By default the following two metadata fields will be expected: Journal DOI (local.identifier.journaldoi, Note that the DOI should be entered with the http://dx.doi.org/ prefix.), Journal Resource: local.identifier.journalresource |
| journal/journal\_article | | | |
| journal/journal\_article/contributors/person\_name | Children: given\_name, surname, alt\_name Attribute:
contributor_role with one of the values author, editor, chair, reviewer, reviewer-assistant, stats-reviewer, reviewer-external, reader, translator Attribute: sequence with one of the values: first, additional | There needs to be a first authors (`sequence=first`) or Crossref is unhappy. | Currently, the first author in the list is being used as first author. All others are additional. |
| journal/journal\_article/titles/title | | Children of titles: title, subtitle, original\_language\_title (with attribute language), subtitle | |
| journal/journal\_article/publication\_date/year | | Year of publication | | 
| journal/journal\_article/doi\_data | Two children: doi, resource | | |

### Technical Report
| Node | Attributes | | Notes|
|----|----|----|----|
| report-paper | | | |
| report-paper/report-paper\_metadata | | | |
| report-paper/report-paper\_metadata/contributors/person\_name | Children: given\_name, surname, alt\_name Attribute contributor\_role with one of the values author, editor, chair, reviewer, reviewer-assistant, stats-reviewer, reviewer-external, reader, translator Attribute: sequence with one of the values: first, additional | There needs to be a first authors (`sequence=first`) or Crossref is unhappy. | Currently, the first author in the list is being used as first author. All others are additional. |
| report-paper/report-paper\_metadata/titles/title | | Children of titles: title, subtitle, original\_language\_title (with attribute language), subtitle | |
| report-paper/report-paper\_metadata/publication\_date/year | | Year of publication | |
| report-paper/report-paper\_metadata/doi\_data | Two children: doi, resource | | |

### Preprint
| Node | Attributes | | Notes|
|----|----|----|----|
| posted\_content | `type="preprint"` | | |
| posted\_content/contributors/person\_name | Children: given\_name, surname, alt\_name Attribute: contributor\_role with one of the values author, editor, chair, reviewer, reviewer-assistant, stats-reviewer, reviewer-external, reader, translator Attribute: sequence with one of the values: first, additional | There needs to be a first authors (`sequence=first`) or Crossref is unhappy. | Currently, the first author in the list is being used as first author. All others are additional. |
| posted\_content/titles/title | | Children of titles: title, subtitle, original\_language\_title (with attribute language), subtitle | |
| posted\_content/publication\_date/year | | Year of publication | |
| posted\_content/doi\_data | Two children: doi, resource | | |

### Working Paper
| Node | Attributes | | Notes|
|----|----|----|----|
| posted\_content | `type="working_paper"` | | |
| posted\_content/contributors/person\_name | Children: given\_name, surname, alt\_name Attribute: contributor\_role with one of the values author, editor, chair, reviewer, reviewer-assistant, stats-reviewer, reviewer-external, reader, translator Attribute: sequence with one of the values: first, additional | There needs to be a first authors (`sequence=first`) or Crossref is unhappy. | Currently, the first author in the list is being used as first author. All others are additional. |
| posted\_content/titles/title| | Children of titles: title, subtitle, original\_language\_title (with attribute language), subtitle | |
| posted\_content/publication\_date/year | | Year of publication | |
| posted\_content/doi\_data | Two children: doi, resource | | |

### Thesis

| Node | Attributes | | Notes|
|----|----|----|----|
| dissertation | `type="working_paper"`| | This is the closest resource type to a Thesis.|
| dissertation/contributors/person\_name | Children: given\_name, surname, alt\_name Attribute: contributor\_role with one of the values author, editor, chair, reviewer, reviewer-assistant, stats-reviewer, reviewer-external, reader, translator Attribute: sequence with one of the values: first, additional | There needs to be a first authors (`sequence=first`) or Crossref is unhappy. | Currently, the first author in the list is being used as first author. All others are additional. |
| dissertation/titles/title | | Children of titles: title, subtitle, original\_language\_title (with attribute language), subtitle | |
| dissertation/publication\_date/year | | Year of publication | |
| dissertation/institution/institution\_name | | Name of the institution of the thesis. | This is a required property.|
| dissertation/doi\_data | Two children: doi, resource | | |

### Anything without explicit XSLT: "Other"

| Node | Attributes | | Notes|
|----|----|----|----|
| posted_content | `type="other"` | | |
| posted\_content/contributors/person\_name |Children: given\_name, surname, alt\_name Attribute: contributor\_role with one of the values author, editor, chair, reviewer, reviewer-assistant, stats-reviewer, reviewer-external, reader, translator Attribute: sequence with one of the values: first, additional | There needs to be a first authors (`sequence=first`) or Crossref is unhappy. | Currently, the first author in the list is being used as first author. All others are additional. |
| posted\_content/titles/title | | Children of titles: title, subtitle, original\_language\_title (with attribute language), subtitle | |
| posted\_content/publication\_date/year | | Year of publication | |
| posted\_content/doi\_data | Two children: doi, resource | | |

