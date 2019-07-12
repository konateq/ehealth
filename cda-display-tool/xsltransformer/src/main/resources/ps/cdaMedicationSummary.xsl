<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:n1="urn:hl7-org:v3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:epsos="urn:epsos-org:ep:medication" version="1.0">
    <xsl:output method="html" indent="yes" version="4.01"
                doctype-system="http://www.w3.org/TR/html4/strict.dtd" doctype-public="-//W3C//DTD HTML 4.01//EN"/>

    <!-- variable to check that at least one medication summary section exist -->
    <xsl:variable name="medicationSummaryExist"
                  select="/n1:ClinicalDocument/n1:component/n1:structuredBody/n1:component/n1:section/n1:code[@code='10160-0']"/>

    <!-- medication summaries -->
    <xsl:template name="medicationSummary"
                  match="/n1:ClinicalDocument/n1:component/n1:structuredBody">
        <xsl:choose>
            <!-- if we have at least one medication summary section -->
            <xsl:when test="($medicationSummaryExist)">
                <xsl:for-each select="/n1:ClinicalDocument/n1:component/n1:structuredBody/n1:component/n1:section">
                    <xsl:call-template name="medicationSummarySection"/>
                </xsl:for-each>
                <br/>
                <br/>
            </xsl:when>
            <!-- else -->
            <xsl:otherwise>
                <span class="sectionTitle">
                    <!-- Medications Summary non present! -->
                    <xsl:choose>
                        <xsl:when test=" ($documentCode='60591-5')">
                            <xsl:call-template name="show-displayLabels">
                                <xsl:with-param name="code" select="'75'"/>
                            </xsl:call-template>
                        </xsl:when>
                    </xsl:choose>
                </span>
                <br/>
                <br/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="medicationSummarySection">
        <!-- Defining all needed variables -->
        <xsl:variable name="medSectionTitleCode" select="n1:code/@code"/>
        <xsl:variable name="medSectionTitle"
                      select="n1:code[@code='10160-0']/@displayName"/>
        <xsl:variable name="nullEntry" select="n1:entry"/>
        <xsl:variable name="medAct" select="n1:entry/n1:act"/>
        <xsl:variable name="medCode" select="n1:entry/n1:substanceAdministration/n1:code/@code"/>
        <!-- End definition of variables -->

        <xsl:choose>
            <!-- if sectionTitle is not missing for medication summary (Exception medication summary section is missing) -->
            <xsl:when test="($medSectionTitleCode='10160-0')">
                <div class="wrap-collabsible">
                    <input id="collapsible-medication-summary-section-original" class="toggle" type="checkbox" checked="true" />
                    <label for="collapsible-medication-summary-section-original" class="lbl-toggle-title">
                        <xsl:value-of select="$medSectionTitle"/>
                    </label>
                    <div class="collapsible-content-title">
                        <div class="content-inner-title">
                            <xsl:choose>
                                <xsl:when test="$shownarrative='true'">
                                    <div class="wrap-collabsible">
                                        <input id="collapsible-medicationSummary-original" class="toggle" type="checkbox"/>
                                        <label for="collapsible-medicationSummary-original" class="lbl-toggle">
                                            <xsl:value-of select="$originalNarrativeTableTitle"/>
                                        </label>
                                        <div class="collapsible-content">
                                            <div class="content-inner">
                                                <xsl:call-template name="show-narrative">
                                                    <xsl:with-param name="node" select="/n1:ClinicalDocument/n1:component/n1:structuredBody/n1:component/n1:section/n1:code[@code='10160-0']/../n1:text"/>
                                                </xsl:call-template>
                                            </div>
                                        </div>
                                    </div>
                                </xsl:when>
                            </xsl:choose>
                            <br/>
                            <!-- nullflavored act -->
                            <div class="wrap-collabsible">
                                <input id="collapsible-medication-summary-translated" class="toggle" type="checkbox" checked="true" />
                                <label for="collapsible-medication-summary-translated" class="lbl-toggle">
                                    <xsl:value-of select="$translatedCodedTableTitle"/>
                                </label>
                                <div class="collapsible-content">
                                    <div class="content-inner">
                                        <xsl:choose>
                                            <xsl:when test="not($medAct/@nullFlavor)">
                                                <table class="translation_table">
                                                    <tbody>
                                                        <xsl:if
                                                                test="	not ($medCode='182849000' or $medCode='408350003' or $medCode='182904002')">
                                                            <tr>
                                                                <th rowspan="2">
                                                                    <!-- Active ingredient -->
                                                                    <xsl:call-template name="show-displayLabels">
                                                                        <xsl:with-param name="code" select="'1'"/>
                                                                    </xsl:call-template>
                                                                </th>
                                                                <th rowspan="2">
                                                                    <!-- Strength -->
                                                                    <xsl:call-template name="show-displayLabels">
                                                                        <xsl:with-param name="code" select="'70'"/>
                                                                    </xsl:call-template>
                                                                </th>
                                                                <th rowspan="2">
                                                                    <!-- Dose form -->
                                                                    <xsl:call-template name="show-displayLabels">
                                                                        <xsl:with-param name="code" select="'25'"/>
                                                                    </xsl:call-template>
                                                                </th>
                                                                <th colspan="2">
                                                                    <!-- Units per intake -->
                                                                    <xsl:call-template name="show-displayLabels">
                                                                        <xsl:with-param name="code" select="'78'"/>
                                                                    </xsl:call-template>
                                                                </th>
                                                                <th rowspan="2">
                                                                    <!-- Frequency of intakes -->
                                                                    <xsl:call-template name="show-displayLabels">
                                                                        <xsl:with-param name="code" select="'32'"/>
                                                                    </xsl:call-template>
                                                                </th>
                                                                <th rowspan="2">
                                                                    <!-- Route of Administration -->
                                                                    <xsl:call-template name="show-displayLabels">
                                                                        <xsl:with-param name="code" select="'67'"/>
                                                                    </xsl:call-template>
                                                                </th>
                                                                <th rowspan="2">
                                                                    <!-- Onset Date -->
                                                                    <xsl:call-template name="show-displayLabels">
                                                                        <xsl:with-param name="code" select="'45'"/>
                                                                    </xsl:call-template>
                                                                </th>
                                                                <th rowspan="2">
                                                                    <!-- End Date -->
                                                                    <xsl:call-template name="show-displayLabels">
                                                                        <xsl:with-param name="code" select="'26'"/>
                                                                    </xsl:call-template>
                                                                </th>
                                                            </tr>
                                                            <tr>
                                                                <th><xsl:value-of select="$min"/></th>
                                                                <th><xsl:value-of select="$max"/></th>
                                                            </tr>
                                                        </xsl:if>
                                                        <xsl:for-each select="n1:entry">
                                                            <xsl:call-template name="medicationSummarySectionEntry"/>
                                                        </xsl:for-each>
                                                    </tbody>
                                                </table>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:call-template name="show-nullFlavor">
                                                    <xsl:with-param name="code" select="$medAct/@nullFlavor"/>
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

    <xsl:template name="medicationSummarySectionEntry">
        <xsl:variable name="medDose"
                      select="n1:substanceAdministration/n1:templateId[@root= '1.3.6.1.4.1.12559.11.10.1.3.1.3.4']/../n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:formCode"/>
        <xsl:variable name="medUnitIntake"
                      select="n1:substanceAdministration/n1:templateId[@root= '1.3.6.1.4.1.12559.11.10.1.3.1.3.4']/../n1:doseQuantity"/>
        <xsl:variable name="medFrequencyIntake"
                      select="n1:substanceAdministration/n1:templateId[@root= '1.3.6.1.4.1.12559.11.10.1.3.1.3.4']/../n1:effectiveTime[2]"/>
        <xsl:variable name="medFrequencyIntakeType"
                      select="n1:substanceAdministration/n1:templateId[@root= '1.3.6.1.4.1.12559.11.10.1.3.1.3.4']/../n1:effectiveTime[2]/@xsi:type"/>
        <xsl:variable name="medRouteAdministration"
                      select="n1:substanceAdministration/n1:templateId[@root= '1.3.6.1.4.1.12559.11.10.1.3.1.3.4']/../n1:routeCode"/>

        <xsl:variable name="medOnSetDate"
                      select="n1:substanceAdministration/n1:templateId[@root= '1.3.6.1.4.1.12559.11.10.1.3.1.3.4']/../n1:effectiveTime[1][@xsi:type='IVL_TS' or substring-after(@xsi:type, ':')='IVL_TS']/n1:low"/>

        <xsl:variable name="medEndDate"
                      select="n1:substanceAdministration/n1:templateId[@root= '1.3.6.1.4.1.12559.11.10.1.3.1.3.4']/../n1:effectiveTime[1][@xsi:type='IVL_TS' or substring-after(@xsi:type, ':')='IVL_TS']/n1:high"/>
        <xsl:variable name="nullEntry" select="."/>
        <xsl:variable name="medAct" select="n1:act"/>
        <xsl:variable name="medCode"
                      select="n1:substanceAdministration/n1:templateId[@root= '1.3.6.1.4.1.12559.11.10.1.3.1.3.4']/../n1:code/@code"/>
        <!-- <xsl:variable name="medDisplay" select="n1:substanceAdministration/n1:code/n1:translation/n1:translation/@displayName"/> -->
        <xsl:variable name="medDisplay" select="n1:substanceAdministration/n1:code/@displayName"/>
        <xsl:for-each
                select="n1:substanceAdministration/n1:templateId[@root= '1.3.6.1.4.1.12559.11.10.1.3.1.3.4']/../n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:ingredient[@classCode='ACTI']">

            <xsl:variable name="medActiveIngredientNode" select="epsos:ingredient/epsos:code"/>
            <xsl:variable name="medActiveIngredient" select="epsos:ingredient/epsos:code/@displayName"/>
            <xsl:variable name="medActiveIngredientTranslation1"
                          select="epsos:ingredient/epsos:code/epsos:translation/epsos:translation/@displayName"/>
            <xsl:variable name="medActiveIngredientTranslation2"
                          select="epsos:ingredient/epsos:code/epsos:translation/@displayName"/>
            <xsl:variable name="medActiveIngredientID" select="epsos:ingredient/epsos:code/@code"/>

            <xsl:variable name="medStrengthNumerator" select="epsos:quantity/epsos:numerator"/>
            <xsl:variable name="medStrengthDenominator" select="epsos:quantity/epsos:denominator"/>

            <!-- nullflavored act -->
            <xsl:choose>
                <xsl:when test="not($medAct/@nullFlavor)">
                    <!-- no info scenario code is one of the three values -->
                    <xsl:choose>
                        <xsl:when
                                test="($medCode='182849000' or $medCode='408350003' or $medCode='182904002')">
                            <tr>
                                <td colspan="6">
                                    <span class="tdtext">
                                        <xsl:call-template name="show-codeNoMedication">
                                            <xsl:with-param name="code" select="$medCode"/>
                                        </xsl:call-template>
                                    </span>
                                    <br/>
                                </td>
                            </tr>
                        </xsl:when>
                        <xsl:otherwise>
                            <tr>
                                <td>
                                    <xsl:choose>
                                        <xsl:when test="not ($medActiveIngredientNode/@nullFlavor)">
                                            <xsl:choose>
                                                <xsl:when test="$medActiveIngredient">
                                                    <xsl:value-of select="$medActiveIngredient"/>
                                                    <br/>
                                                    (
                                                    <xsl:value-of select="$medActiveIngredientID"/>
                                                    )
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <!-- uncoded element Problem -->
                                                    <xsl:if test="$medActiveIngredientNode/n1:originalText/n1:reference/@value">
                                                        <xsl:call-template name="show-uncodedElement">
                                                            <xsl:with-param name="code"
                                                                            select="$medActiveIngredientNode/n1:originalText/n1:reference/@value"/>
                                                        </xsl:call-template>
                                                    </xsl:if>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:call-template name="show-nullFlavor">
                                                <xsl:with-param name="code"
                                                                select="$medActiveIngredientNode/@nullFlavor"/>
                                            </xsl:call-template>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </td>
                                <td>
                                    <xsl:call-template name="show-strength">
                                        <xsl:with-param name="medStrengthNumerator" select="$medStrengthNumerator"/>
                                        <xsl:with-param name="medStrengthDenominator" select="$medStrengthDenominator"/>
                                    </xsl:call-template>
                                </td>
                                <td>
                                    <xsl:choose>
                                        <xsl:when test="$medDose/@nullFlavor">
                                            <xsl:call-template name="show-nullFlavor">
                                                <xsl:with-param name="code"
                                                                select="$medDose/@nullFlavor"/>
                                            </xsl:call-template>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:value-of select="$medDose/@displayName"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </td>
                                <td>
                                    <xsl:call-template name="show-numberUnitIntakeLow">
                                        <xsl:with-param name="medUnitIntake" select="$medUnitIntake"/>
                                    </xsl:call-template>
                                </td>
                                <td>
                                    <xsl:call-template name="show-numberUnitIntakeHigh">
                                        <xsl:with-param name="medUnitIntake" select="$medUnitIntake"/>
                                    </xsl:call-template>
                                </td>
                                <td>
                                    <xsl:choose>
                                        <xsl:when test="not ($medFrequencyIntake/@nullFlavor)">
                                            <xsl:call-template name="show-frequencyIntake">
                                                <xsl:with-param name="medFrequencyIntakeType" select="$medFrequencyIntakeType"/>
                                                <xsl:with-param name="medFrequencyIntake" select="$medFrequencyIntake"/>
                                            </xsl:call-template>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:call-template name="show-nullFlavor">
                                                <xsl:with-param name="code" select="$medFrequencyIntake/@nullFlavor"/>
                                            </xsl:call-template>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </td>
                                <td>
                                    <xsl:value-of select="$medRouteAdministration/@displayName"/>
                                </td>
                                <td>
                                    <xsl:call-template name="show-time">
                                        <xsl:with-param name="datetime" select="$medOnSetDate"/>
                                    </xsl:call-template>
                                </td>
                                <td>
                                    <xsl:call-template name="show-time">
                                        <xsl:with-param name="datetime" select="$medEndDate"/>
                                    </xsl:call-template>
                                    &#160;
                                </td>
                            </tr>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                    <tr>
                        <td colspan="5">
                            <xsl:call-template name="show-nullFlavor">
                                <xsl:with-param name="code" select="$medAct/@nullFlavor"/>
                            </xsl:call-template>
                        </td>
                    </tr>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="substring-after-if-containsJe">
        <xsl:param name="arg"/>
        <xsl:param name="delim"/>

        <xsl:choose>
            <xsl:when test="contains($arg,$delim)">
                <xsl:value-of select="substring-after($arg,$delim)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$arg"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>