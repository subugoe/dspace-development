<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : DIM2CRossref_article.xsl
    Created on : October 4, 2020, 1:26 PM
    Author     : jdamerow
    Description: Converts metadata from DSpace Intermediat Format (DIM) into
                 metadata following the Crossref Schema for Article, version 4.4.2
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dspace="http://www.dspace.org/xmlns/dspace/dim"
                xmlns="http://www.crossref.org/schema/4.4.2"
                version="1.0">
    <xsl:variable name="bookType" select="//dspace:field[@mdschema='dc' and @element='type']/text()" ></xsl:variable>

    <xsl:output method="xml" indent="yes" encoding="utf-8" />

    <!-- Don't copy everything by default! -->
    <xsl:template match="@* | text()" />

    <xsl:template match="/dspace:dim[@dspaceType='ITEM']">
      <doi_batch version="4.4.2" xmlns="http://www.crossref.org/schema/4.4.2"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://www.crossref.org/schema/4.4.2 http://www.crossref.org/schema/deposit/crossref4.4.2.xsd">

          <head>
              <!-- This section will be filled programmatically. Do not remove! -->
          </head>

          <body>
            <journal>
              <journal_metadata>

                <!--
                  CrossRef
                  Add title information
                -->
                <full_title>
                    <xsl:choose>
                      <xsl:when test="//dspace:field[@mdschema='dc' and @element='relation' and @qualifier='ispartof']">
                        <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='relation' and @qualifier='ispartof']"/>
                      </xsl:when>
                      <xsl:otherwise>
                        (:unas) unassigned
                      </xsl:otherwise>
                    </xsl:choose>
                </full_title>
                <abbrev_title>
                    <xsl:choose>
                      <xsl:when test="//dspace:field[@mdschema='dc' and @element='relation' and @qualifier='ispartof']">
                        <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='relation' and @qualifier='ispartof']"/>
                      </xsl:when>
                      <xsl:otherwise>
                        (:unas) unassigned
                      </xsl:otherwise>
                    </xsl:choose>
                </abbrev_title>

                <xsl:choose>
                  <xsl:when test="//dspace:field[@mdschema='dc' and @element='identifier' and @qualifier='issn']">
                    <xsl:apply-templates select="dspace:field[@mdschema='dc' and @element='identifier' and @qualifier='issn']" />
                  </xsl:when>
                  <xsl:when test="//dspace:field[@mdschema='local' and @element='identifier' and @qualifier='journaldoi']">
                    <xsl:apply-templates select="dspace:field[@mdschema='local' and @element='identifier' and @qualifier='journaldoi']" />
                  </xsl:when>
                </xsl:choose>
              </journal_metadata>

              <!-- Add journal article info -->
              <journal_article>

                <!--
                  CrossRef
                  Add title information
                -->
                <titles>
                    <xsl:choose>
                      <xsl:when test="//dspace:field[@mdschema='dc' and @element='title']">
                          <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='title']"/>
                      </xsl:when>
                      <xsl:otherwise>
                        <title>
                          (:unas) unassigned
                        </title>
                      </xsl:otherwise>
                    </xsl:choose>
                </titles>

                <!--
                  CrossRef
                  Add author information
                -->
                <contributors>
                  <xsl:choose>
                      <xsl:when test="//dspace:field[@mdschema='dc' and @element='contributor' and @qualifier='author']">
                        <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='contributor' and @qualifier='author']" />
                      </xsl:when>
                      <xsl:otherwise>
                        <anonymous contributor_role="author" sequence="first" />
                      </xsl:otherwise>
                  </xsl:choose>
                </contributors>

                <!--
                  CrossRef
                  Add publication year information
                -->
                <publication_date>
                  <year>
                    <xsl:choose>
                        <xsl:when test="//dspace:field[@mdschema='dc' and @element='date' and @qualifier='issued']">
                            <xsl:value-of select="substring(//dspace:field[@mdschema='dc' and @element='date' and @qualifier='issued'], 1, 4)" />
                        </xsl:when>
                        <xsl:when test="//dspace:field[@mdschema='dc' and @element='date' and @qualifier='available']">
                            <xsl:value-of select="substring(//dspace:field[@mdschema='dc' and @element='date' and @qualifier='issued'], 1, 4)" />
                        </xsl:when>
                        <xsl:when test="//dspace:field[@mdschema='dc' and @element='date']">
                            <xsl:value-of select="substring(//dspace:field[@mdschema='dc' and @element='date'], 1, 4)" />
                        </xsl:when>
                        <xsl:otherwise>0000</xsl:otherwise>
                    </xsl:choose>
                  </year>
                </publication_date>

                <!--
                  CrossRef
                  Add DOI information
                -->
                <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='identifier' and starts-with(., 'http://dx.doi.org/')]" />
              </journal_article>
            </journal>
          </body>
      </doi_batch>
    </xsl:template>

    <!-- template to create DOI -->
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

    <!-- template to create first author -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='contributor' and @qualifier='author'][1]">
      <person_name sequence="first" contributor_role="author" >
      <given_name>
        <xsl:value-of select="substring-before(./text(), ',')"/>
      </given_name>
      <surname>
        <xsl:value-of select="substring-after(./text(), ',')"/>
      </surname>
    </person_name>
    </xsl:template>

    <!-- template to create additional authors -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='contributor' and @qualifier='author'][position() > 1]">
      <person_name sequence="additional" contributor_role="author" >
      <given_name>
        <xsl:value-of select="substring-before(./text(), ',')"/>
      </given_name>
      <surname>
        <xsl:value-of select="substring-after(./text(), ',')"/>
      </surname>
    </person_name>
    </xsl:template>

    <!-- template to create titles -->
    <xsl:template match="dspace:field[@mdschema='dc' and @element='title']">
      <title>
        <xsl:value-of select="." />
      </title>
    </xsl:template>

    <!-- template to create journal title -->
    <xsl:template match="dspace:field[@mdschema='dc' and @element='relation' and @qualifier='ispartof']">
      <xsl:value-of select="." />
    </xsl:template>

    <!-- template to create journal issn -->
    <xsl:template match="dspace:field[@mdschema='dc' and @element='identifier' and @qualifier='issn']">
      <issn>
        <xsl:value-of select="." />
      </issn>
    </xsl:template>

    <!-- template to create journal doi -->
    <xsl:template match="dspace:field[@mdschema='local' and @element='identifier' and @qualifier='journaldoi']">
        <doi_data>
          <doi>
            <xsl:value-of select="substring(., 19)"/>
          </doi>
          <xsl:if test="//dspace:field[@mdschema='local' and @element='identifier' and @qualifier='journalresource']" >
            <resource>
              <xsl:value-of select="//dspace:field[@mdschema='local' and @element='identifier' and @qualifier='journalresource']"/>
            </resource>
          </xsl:if>
        </doi_data>
    </xsl:template>
</xsl:stylesheet>
