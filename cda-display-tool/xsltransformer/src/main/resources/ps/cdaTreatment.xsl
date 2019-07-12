<?xml version="1.0" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:n1="urn:hl7-org:v3"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:epsos="urn:epsos-org:ep:medication">

    <xsl:output method="html" indent="yes" version="4.01" doctype-system="http://www.w3.org/TR/html4/strict.dtd"
                doctype-public="-//W3C//DTD HTML 4.01//EN"/>

    <!-- variable to check that at least one treatment section exist -->
    <xsl:variable name="treatmentExist"
                  select="/n1:ClinicalDocument/n1:component/n1:structuredBody/n1:component/n1:section/n1:code[@code='18776-5']"/>

    <!-- treatments -->
    <xsl:template name="treatment" match="/n1:ClinicalDocument/n1:component/n1:structuredBody">
        <xsl:choose>
            <!-- if we have at least one alert section -->
            <xsl:when test="($treatmentExist)">
                <xsl:for-each select="/n1:ClinicalDocument/n1:component/n1:structuredBody/n1:component/n1:section">
                    <xsl:call-template name="treatmentSection"/>
                </xsl:for-each>
                <br/>
                <br/>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="treatmentSection">
        <!-- Defining all needed variables -->
        <xsl:variable
                name="treatmentSectionTitleCode"
                select="n1:code/@code"/>
        <xsl:variable
                name="treatmentSectionTitle"
                select="n1:code[@code='18776-5']/@displayName"/>
        <xsl:variable
                name="treatmentSectionText"
                select="n1:code[@code='18776-5']/../n1:text"/>
        <!-- End definition of variables-->

        <xsl:choose>
            <!-- if sectionTitle is not missing for treatments  (Exception treatments section is missing)-->
            <xsl:when test=" ($treatmentSectionTitleCode='18776-5')">
                <div class="wrap-collabsible">
                    <input id="collapsible-treatment-section-original" class="toggle" type="checkbox" checked="true" />
                    <label for="collapsible-treatment-section-original" class="lbl-toggle-title">
                        <xsl:value-of select="$treatmentSectionTitle"/>
                    </label>
                    <div class="collapsible-content-title">
                        <div class="content-inner-title">
                            <xsl:choose>
                                <xsl:when test="$shownarrative='true'">
                                    <div class="wrap-collabsible">
                                        <input id="collapsible-treatment-original" class="toggle" type="checkbox"/>
                                        <label for="collapsible-treatment-original" class="lbl-toggle">
                                            <xsl:value-of select="$originalNarrativeTableTitle"/>
                                        </label>
                                        <div class="collapsible-content">
                                            <div class="content-inner">
                                                <xsl:call-template name="show-narrative">
                                                    <xsl:with-param name="node" select="/n1:ClinicalDocument/n1:component/n1:structuredBody/n1:component/n1:section/n1:code[@code='18776-5']/../n1:text"/>
                                                </xsl:call-template>
                                            </div>
                                        </div>
                                    </div>
                                </xsl:when>
                            </xsl:choose>
                        </div>
                    </div>
                </div>
            </xsl:when>
        </xsl:choose>

        <xsl:for-each select="n1:component/n1:section">
            <xsl:call-template name="treatmentSection">
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>
    <!-- top level section title -->
</xsl:stylesheet>
