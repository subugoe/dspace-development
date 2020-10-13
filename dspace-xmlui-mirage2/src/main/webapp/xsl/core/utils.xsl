<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->

<!--
    This stylesheet contains helper templates for things like i18n and standard attributes.

    Author: art.lowel at atmire.com
    Author: lieven.droogmans at atmire.com
    Author: ben at atmire.com
    Author: Alexey Maslov

-->

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

    <xsl:output indent="yes"/>


    <!-- templates for required textarea attributes used if not found in DRI document -->
    <xsl:template name="textAreaCols">
        <xsl:attribute name="cols">20</xsl:attribute>
    </xsl:template>

    <xsl:template name="textAreaRows">
        <xsl:attribute name="rows">5</xsl:attribute>
    </xsl:template>



    <!-- This does it for all the DRI elements. The only thing left to do is to handle Cocoon's i18n
        transformer tags that are used for text translation. The templates below simply push through
        the i18n elements so that they can translated after the XSL step. -->
    <xsl:template match="i18n:text">
        <xsl:param name="text" select="."/>
        <xsl:choose>
            <xsl:when test="contains($text, '&#xa;')">
                <xsl:value-of select="substring-before($text, '&#xa;')"/>
                <ul>
                    <xsl:attribute name="style">float:left; list-style-type:none; text-align:left;</xsl:attribute>
                    <xsl:call-template name="linebreak">
                        <xsl:with-param name="text" select="substring-after($text,'&#xa;')"/>
                    </xsl:call-template>
                </ul>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="$text"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Function to replace \n -->
    <xsl:template name="linebreak">
        <xsl:param name="text" select="."/>
        <xsl:choose>
            <xsl:when test="contains($text, '&#xa;')">
                <li>
                    <xsl:value-of select="substring-before($text, '&#xa;')"/>
                </li>
                <xsl:call-template name="linebreak">
                    <xsl:with-param name="text" select="substring-after($text,'&#xa;')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$text"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="i18n:translate">
        <xsl:copy-of select="."/>
    </xsl:template>

    <xsl:template match="i18n:param">
        <xsl:copy-of select="."/>
    </xsl:template>

    <!--added classes to differentiate between collections, communities and items-->
    <xsl:template match="dri:reference" mode="summaryList">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- Since this is a summary only grab the descriptive metadata, and the thumbnails -->
            <xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
            <!-- An example of requesting a specific metadata standard (MODS and QDC crosswalks only work for items)-->
        </xsl:variable>
        <xsl:comment> External Metadata URL: <xsl:value-of select="$externalMetadataURL"/> </xsl:comment>
        <li>
            <xsl:attribute name="class">
                <xsl:text>ds-artifact-item </xsl:text>
                <xsl:choose>
                    <xsl:when test="contains(@type, 'Community')">
                        <xsl:text>community </xsl:text>
                    </xsl:when>
                    <xsl:when test="contains(@type, 'Collection')">
                        <xsl:text>collection </xsl:text>
                    </xsl:when>
                </xsl:choose>
                <xsl:choose>
                    <xsl:when test="position() mod 2 = 0">even</xsl:when>
                    <xsl:otherwise>odd</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:apply-templates select="document($externalMetadataURL)" mode="summaryList"/>
            <xsl:apply-templates />
        </li>
    </xsl:template>

    <xsl:template name="standardAttributes">
        <xsl:param name="class"/>
        <xsl:param name="placeholder"/>
        <xsl:if test="@id">
            <xsl:attribute name="id"><xsl:value-of select="translate(@id,'.','_')"/></xsl:attribute>
        </xsl:if>
        <xsl:attribute name="class">
            <xsl:value-of select="normalize-space($class)"/>
            <xsl:if test="@rend">
                <xsl:text> </xsl:text>
                <xsl:value-of select="@rend"/>
            </xsl:if>
        </xsl:attribute>
        <xsl:if test="string-length($placeholder)>0">
            <xsl:attribute name="placeholder"><xsl:value-of select="$placeholder"/></xsl:attribute>
            <xsl:attribute name="i18n:attr">placeholder</xsl:attribute>
        </xsl:if>
    </xsl:template>

    <!-- Message catalogue -->
    <xsl:template match="dri:table[@rend='catalogue-messages']">
    	<script>
    		//# sourceURL=backdrop.js
    		var backdrop = document.createElement("div");
    		backdrop.classList.add("modal-backdrop");
    		backdrop.classList.add("backdrop-transparency");
    		backdrop.id="backdropDiv";

    		var div = document.createElement("div");
    		div.classList.add("backdrop-loading-icon");
    		div.id = "loading";

    		var icon = document.createElement("i");
    		icon.classList.add("glyphicon");
    		icon.classList.add("glyphicon-repeat");
    		icon.classList.add("right-spinner");

    		var text = document.createTextNode("<i18n:text>xmlui.administrative.catalogue.message.loading</i18n:text>");
    		var textDiv = document.createElement("div");
    		textDiv.appendChild(text);

    		backdrop.appendChild(div);
    		div.appendChild(icon);
    		div.appendChild(textDiv);
    		var body = document.getElementsByTagName("body")[0];
    		body.prepend(backdrop);
    	</script>
    	<div class="alert alert-danger" id="errorAddAlert" role="alert" style="display:none">
    	<i18n:text>xmlui.administrative.catalogue.message.server.error.add</i18n:text>
    	<span class="pull-right" aria-hidden="true" id="closeErrorAddAlert"><i class="glyphicon glyphicon-remove"></i></span>
    	</div>
    	<div class="alert alert-danger" id="errorDeleteAlert" role="alert" style="display:none">
    	<i18n:text>xmlui.administrative.catalogue.message.server.error.delete</i18n:text>
    	<span class="pull-right" aria-hidden="true" id="closeErrorDeleteAlert"><i class="glyphicon glyphicon-remove"></i></span>
    	</div>
    	<div class="alert alert-success" id="successAddAlert" role="alert" style="display:none">
    	<i18n:text>xmlui.administrative.catalogue.message.server.success.add</i18n:text>
    	<span class="pull-right" aria-hidden="true" id="closeSuccessAddAlert"><i class="glyphicon glyphicon-remove"></i></span>
    	</div>
    	<div class="alert alert-success" id="successDeleteAlert" role="alert" style="display:none">
    	<i18n:text>xmlui.administrative.catalogue.message.server.success.delete</i18n:text>
    	<span class="pull-right" aria-hidden="true" id="closeSuccessDeleteAlert"><i class="glyphicon glyphicon-remove"></i></span>
    	</div>
    	<div class="alert alert-warning" id="warningInvalidXmlAlert" role="alert" style="display:none">
    	<i18n:text>xmlui.administrative.catalogue.message.server.warning.invalidxml</i18n:text>
    	<span class="pull-right" aria-hidden="true" id="closeWarningInvalidXmlAlert"><i class="glyphicon glyphicon-remove"></i></span>
    	</div>
   	 	<div class="modal fade" id="deleteModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel">
		  <div class="modal-dialog" role="document">
		    <div class="modal-content">
		      <div class="modal-header">
		        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true" class="glyphicon glyphicon-remove"></span></button>
		        <h4 class="modal-title" id="myModalLabel"><i18n:text>xmlui.administrative.catalogue.message.delete.modal.header</i18n:text></h4>
		      </div>
		      <div class="modal-body">
		        <i18n:text>xmlui.administrative.catalogue.message.delete.modal.message</i18n:text>
		        <br></br><span id="keyToDelete"></span>
		      </div>
		      <div class="modal-footer">
		        <button type="button" class="btn btn-default" data-dismiss="modal"><i18n:text>xmlui.administrative.catalogue.message.delete.modal.cancel</i18n:text></button>
		        <button type="button" class="btn btn-primary" id="okDeleteBtn"><i18n:text>xmlui.administrative.catalogue.message.delete.modal.ok</i18n:text></button>
		      </div>
		    </div>
		  </div>
		</div>
		<div class="modal fade" id="addModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel">
		  <div class="modal-dialog" role="document">
		    <div class="modal-content">
		      <div class="modal-header">
		        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true" class="glyphicon glyphicon-remove"></span></button>
		        <h4 class="modal-title" id="myModalLabel"><i18n:text>xmlui.administrative.catalogue.message.add.modal.header</i18n:text></h4>
		      </div>
		      <div class="modal-body">
		        <label><i18n:text>xmlui.administrative.catalogue.message.add.modal.message.key</i18n:text></label>
		        <input type="text" class="form-control" id="new-message-key"></input>
		        <div id="key-error-msg" style="display:none;" class="text-danger"><small><i18n:text>xmlui.administrative.catalogue.message.add.modal.message.key.missing</i18n:text></small></div>
		        <label><i18n:text>xmlui.administrative.catalogue.message.add.modal.message.value</i18n:text></label>
		        <input type="text" class="form-control" id="new-message-value"></input>
		        <div id="invalidXmlErrorMsg" class="text-danger" style="display:none;">
		        <i18n:text>xmlui.administrative.catalogue.message.add.warning.invalidxml.message</i18n:text>
		        </div>
		      </div>
		      <div class="modal-footer">
		        <button type="button" class="btn btn-default" data-dismiss="modal"><i18n:text>xmlui.administrative.catalogue.message.add.modal.cancel</i18n:text></button>
		        <button type="button" class="btn btn-primary" id="okAddBtn"><i18n:text>xmlui.administrative.catalogue.message.add.modal.ok</i18n:text></button>
		      </div>
		    </div>
		  </div>
		</div>
		<button style="margin-bottom: 10px;" class="btn btn-default" id="addMessageBtn">
			<i18n:text>xmlui.administrative.catalogue.message.add</i18n:text>
		</button>
		<p>
		<i18n:text>xmlui.administrative.catalogue.message.description</i18n:text>
		</p>
    	<table id="message-table" class="ds-table table table-striped table-hover" style="table-layout:fixed;">
    		<xsl:apply-templates />
    	</table>
    </xsl:template>
    <xsl:template match="dri:cell[@rend='catalogue-message-key']">
    	<td style="width: 40%; word-break: break-word;">
    		<xsl:apply-templates />
    	</td>
    </xsl:template>
    <xsl:template match="dri:cell[@rend='catalogue-message-value']">
    	<td style="width: 50%; ">
    		<div>
    		<xsl:apply-templates />
    		</div>
    	</td>
    </xsl:template>
    <xsl:template match="dri:field[@n='EditButtons'][@type='button']">
    	<nobr><button class="btn btn-default" name="Edit"><span class="glyphicon glyphicon-edit"></span></button>
    	<button class="btn btn-default" name="Delete"><span class="glyphicon glyphicon-trash"></span></button></nobr>
    </xsl:template>

    <xsl:template match="dri:list[@rend='catalogue-list']">
    	<ul class="list-group">
    		<xsl:apply-templates />
    	</ul>
    </xsl:template>
    <xsl:template match="dri:list[@rend='catalogue-list']//dri:item[dri:xref]">
    	<li class="list-group-item">
    		<a href="{dri:xref/@target}">
            <xsl:choose>
                <xsl:when test="dri:xref/node()">
                    <xsl:apply-templates select="dri:xref/node()"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="dri:xref"/>
                </xsl:otherwise>
            </xsl:choose>
        </a>
    	</li>
    </xsl:template>


</xsl:stylesheet>
