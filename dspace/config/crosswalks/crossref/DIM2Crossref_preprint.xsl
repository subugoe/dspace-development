<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : DIM2Crossref_preprint.xsl
    Created on : October 4, 2020, 1:26 PM
    Author     : jdamerow
    Description: Converts metadata from DSpace Intermediat Format (DIM) into
                 metadata following the Crossref Schema for Preprints, version 4.4.2
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dspace="http://www.dspace.org/xmlns/dspace/dim"
                xmlns="http://www.crossref.org/schema/4.4.2"
                version="1.0">

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
            <posted_content type="preprint">
              <!--
                CrossRef
                Add author information
              -->
              <contributors>
                <xsl:choose>
                    <xsl:when test="//dspace:field[@mdschema='dc' and @element='contributor' and (@qualifier='author' or @qualifier='editor')]">
                      <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='contributor'  and (@qualifier='author' or @qualifier='editor')]" />
                    </xsl:when>
                    <xsl:otherwise>
                      <anonymous contributor_role="author" />
                    </xsl:otherwise>
                </xsl:choose>

              </contributors>

              <!--
                CrossRef
                Add title information
                Crossref requires this field
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
                Add publication year information
                Crossref requires this field
              -->
              <posted_date media_type="print">
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
              </posted_date>

              <xsl:apply-templates select="dspace:field[@mdschema='dc' and @element='identifier' and starts-with(., 'http://dx.doi.org/')]" />
          </posted_content>
        </body>
      </doi_batch>
    </xsl:template>

    <!-- template to create titles -->
    <xsl:template match="dspace:field[@mdschema='dc' and @element='title']">
        <xsl:choose>
            <xsl:when test="@qualifier='alternative'">
                <subtitle>
                  <xsl:value-of select="." />
                </subtitle>
            </xsl:when>
            <xsl:otherwise>
              <title>
                <xsl:value-of select="." />
              </title>
            </xsl:otherwise>
        </xsl:choose>
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

    <!-- template to create editors -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='contributor' and @qualifier!='author']">
      <person_name sequence="additional">
        <xsl:attribute name="contributor_role">
          <xsl:choose>
              <xsl:when test="self::node()[@qualifier='editor']">editor</xsl:when>
          </xsl:choose>
        </xsl:attribute>
        <given_name>
          <xsl:value-of select="substring-before(./text(), ',')"/>
        </given_name>
        <surname>
          <xsl:value-of select="substring-after(./text(), ',')"/>
        </surname>
      </person_name>
    </xsl:template>

</xsl:stylesheet>
