<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:n1="urn:hl7-org:v3" version="1.0">
    <xsl:output method="html" indent="yes" version="4.01" doctype-system="http://www.w3.org/TR/html4/strict.dtd"
                doctype-public="-//W3C//DTD HTML 4.01//EN"/>

    <!-- variable to check that at least one surgical procedure section exist -->
    <xsl:variable name="surgicalProceduresExist"
                  select="/n1:ClinicalDocument/n1:component/n1:structuredBody/n1:component/n1:section/n1:code[@code='47519-4']"/>

    <!--surgical procedures -->
    <xsl:template name="surgicalProcedures" match="/n1:ClinicalDocument/n1:component/n1:structuredBody">
        <xsl:choose>
            <!-- if we have at least one surgical procedure section -->
            <xsl:when test="($surgicalProceduresExist)">

                <xsl:for-each select="/n1:ClinicalDocument/n1:component/n1:structuredBody/n1:component/n1:section">
                    <xsl:call-template name="surgicalProceduresSection"/>
                </xsl:for-each>
                <br/>
                <br/>
            </xsl:when>
            <xsl:otherwise>
                <span class="sectionTitle">
                    <!-- The Procedures section is missing! -->
                    <xsl:choose>
                        <xsl:when test=" ($documentCode='60591-5')">
                            <xsl:call-template name="show-displayLabels">
                                <xsl:with-param name="code" select="'76'"/>
                            </xsl:call-template>
                        </xsl:when>
                    </xsl:choose>
                </span>
                <br/>
                <br/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="surgicalProceduresSection">
        <!-- Defining all needed variables -->
        <xsl:variable
                name="surgicalProceduresSectionTitleCode"
                select="n1:code/@code"/>
        <xsl:variable
                name="surgicalProceduresSectionTitle"
                select="n1:code[@code='47519-4']/@displayName"/>
        <xsl:variable
                name="nullEntry"
                select="n1:entry"/>
        <xsl:variable name="surgProcAct"
                      select="n1:entry/n1:procedure"/>
        <!-- End definition of variables-->
        <xsl:choose>
            <!-- if sectionTitle is not missing for alerts  (Exception alerts section is missing)-->
            <xsl:when test=" ($surgicalProceduresSectionTitleCode='47519-4')">
                <div class="wrap-collabsible">
                    <input id="collapsible-surgical-procedures-section-original" class="toggle" type="checkbox" checked="true" />
                    <label for="collapsible-surgical-procedures-section-original" class="lbl-toggle-title">
                        <xsl:value-of select="$surgicalProceduresSectionTitle"/>
                    </label>
                    <div class="collapsible-content-title">
                        <div class="content-inner-title">
                            <xsl:choose>
                                <xsl:when test="$shownarrative='true'">
                                    <div class="wrap-collabsible">
                                        <input id="collapsible-surgical-procedures-original" class="toggle" type="checkbox"/>
                                        <label for="collapsible-surgical-procedures-original" class="lbl-toggle">
                                            <xsl:value-of select="$originalNarrativeTableTitle"/>
                                        </label>
                                        <div class="collapsible-content">
                                            <div class="content-inner">
                                                <xsl:call-template name="show-narrative">
                                                    <xsl:with-param name="node" select="/n1:ClinicalDocument/n1:component/n1:structuredBody/n1:component/n1:section/n1:code[@code='47519-4']/../n1:text"/>
                                                </xsl:call-template>
                                            </div>
                                        </div>
                                    </div>
                                </xsl:when>
                            </xsl:choose>
                            <br/>
                            <!-- nullflavored act -->
                            <div class="wrap-collabsible">
                                <input id="collapsible-surgical-procedures-translated" class="toggle" type="checkbox" checked="true"/>
                                <label for="collapsible-surgical-procedures-translated" class="lbl-toggle">
                                    <xsl:value-of select="$translatedCodedTableTitle"/>
                                </label>
                                <div class="collapsible-content">
                                    <div class="content-inner">
                                        <xsl:choose>
                                            <xsl:when test="not($surgProcAct/@nullFlavor)">
                                                <table class="translation_table">
                                                    <tbody>
                                                        <tr>
                                                            <th>
                                                                <!--  Procedure -->
                                                                <xsl:call-template name="show-displayLabels">
                                                                    <xsl:with-param name="code" select="'62'"/>
                                                                </xsl:call-template>
                                                            </th>
                                                            <th>
                                                                <!--  Procedure Date -->
                                                                <xsl:call-template name="show-displayLabels">
                                                                    <xsl:with-param name="code" select="'63'"/>
                                                                </xsl:call-template>
                                                            </th>
                                                        </tr>
                                                        <xsl:for-each select="n1:entry">
                                                            <xsl:call-template name="surgicalProceduresSectionEntry">
                                                            </xsl:call-template>
                                                        </xsl:for-each>
                                                    </tbody>
                                                </table>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <br/>
                                                <xsl:call-template name="show-nullFlavor">
                                                    <xsl:with-param name="code" select="$surgProcAct/@nullFlavor"/>
                                                </xsl:call-template>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <!-- FOR EACH ENTRY -->
    <xsl:template name="surgicalProceduresSectionEntry">
        <!-- Defining all needed variables -->
        <xsl:variable
                name="surgicalProcedureNode"
                select="n1:procedure/n1:templateId[@root= '1.3.6.1.4.1.19376.1.5.3.1.4.19']/../n1:code"/>
        <xsl:variable
                name="surgicalProcedure"
                select="n1:procedure/n1:templateId[@root= '1.3.6.1.4.1.19376.1.5.3.1.4.19']/../n1:code/@displayName"/>
        <xsl:variable
                name="surgicalProcedureTranslation1"
                select="n1:procedure/n1:templateId[@root= '1.3.6.1.4.1.19376.1.5.3.1.4.19']/../n1:code/n1:translation/n1:translation/@displayName"/>
        <xsl:variable
                name="surgicalProcedureTranslation2"
                select="n1:procedure/n1:templateId[@root= '1.3.6.1.4.1.19376.1.5.3.1.4.19']/../n1:code/n1:translation/@displayName"/>
        <xsl:variable
                name="surgicalProcedureLowDate"
                select="n1:procedure/n1:templateId[@root= '1.3.6.1.4.1.19376.1.5.3.1.4.19']/../n1:effectiveTime/n1:low"/>
        <xsl:variable
                name="surgicalProcedureDate"
                select="n1:procedure/n1:templateId[@root= '1.3.6.1.4.1.19376.1.5.3.1.4.19']/../n1:effectiveTime"/>
        <xsl:variable
                name="nullEntry"
                select="."/>
        <xsl:variable name="surgProcAct"
                      select="n1:procedure"/>
        <!-- End definition of variables-->
        <xsl:choose>
            <xsl:when test="not($surgProcAct/@nullFlavor)">
                <tr>
                    <td>
                        <xsl:choose>
                            <xsl:when test="not ($surgicalProcedureNode/@nullFlavor)">
                                <xsl:choose>
                                    <xsl:when test="$surgicalProcedure">
                                        <xsl:value-of select="$surgicalProcedure"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <!--  uncoded element only if we don;t have displayName -->
                                        <xsl:if test="$surgicalProcedureNode/n1:originalText/n1:reference/@value">
                                            <xsl:call-template name="show-uncodedElement">
                                                <xsl:with-param name="code"
                                                                select="$surgicalProcedureNode/n1:originalText/n1:reference/@value"/>
                                            </xsl:call-template>
                                        </xsl:if>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:call-template name="show-nullFlavor">
                                    <xsl:with-param name="code" select="$surgicalProcedureNode/@nullFlavor"/>
                                </xsl:call-template>
                            </xsl:otherwise>
                        </xsl:choose>
                    </td>
                    <td>
                        <xsl:choose>
                            <xsl:when test="$surgicalProcedureLowDate">
                                <xsl:call-template name="show-time">
                                    <xsl:with-param name="datetime" select="$surgicalProcedureLowDate"/>
                                </xsl:call-template>&#160;
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:call-template name="show-time">
                                    <xsl:with-param name="datetime" select="$surgicalProcedureDate"/>
                                </xsl:call-template>&#160;
                            </xsl:otherwise>
                        </xsl:choose>
                    </td>
                </tr>
            </xsl:when>
            <xsl:otherwise>
                <tr>
                    <td colspan="3">
                        <xsl:call-template name="show-nullFlavor">
                            <xsl:with-param name="code" select="$surgProcAct/@nullFlavor"/>
                        </xsl:call-template>
                    </td>
                </tr>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
