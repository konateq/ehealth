<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:n1="urn:hl7-org:v3" version="1.0">
    <xsl:output method="html" indent="yes" version="4.01" doctype-system="http://www.w3.org/TR/html4/strict.dtd"
                doctype-public="-//W3C//DTD HTML 4.01//EN"/>

    <!-- variable to check that at least one alert section exist -->
    <xsl:variable name="alertsExist"
                  select="/n1:ClinicalDocument/n1:component/n1:structuredBody/n1:component/n1:section/n1:code[@code='48765-2']"/>

    <xsl:variable
            name="reactionTypeNarrative"
            select="/n1:ClinicalDocument/n1:component/n1:structuredBody/n1:component/n1:section/n1:code[@code='48765-2']/../n1:text"/>

    <!--alerts -->
    <xsl:template name="alerts" match="/n1:ClinicalDocument/n1:component/n1:structuredBody">
        <xsl:choose>
            <!-- if we have at least one alert section -->
            <xsl:when test="($alertsExist)">
                <xsl:for-each select="/n1:ClinicalDocument/n1:component/n1:structuredBody/n1:component/n1:section">
                    <xsl:call-template name="alertSection"/>
                </xsl:for-each>
                <br/>
                <br/>
            </xsl:when>
            <!-- else -->
            <xsl:otherwise>
                <span class="sectionTitle">
                    <!-- The Allergies, adverse reactions, alerts section is missing ! -->
                    <xsl:choose>
                        <xsl:when test=" ($documentCode='60591-5')">
                            <xsl:call-template name="show-displayLabels">
                                <xsl:with-param name="code" select="'73'"/>
                            </xsl:call-template>
                        </xsl:when>
                    </xsl:choose>
                </span>
                <br/>
                <br/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="alertSection">
        <!-- Defining all needed variables -->
        <xsl:variable
                name="sectionTitleCode"
                select="n1:code/@code"/>
        <xsl:variable
                name="sectionTitle"
                select="n1:code[@code='48765-2']/@displayName"/>
        <xsl:variable name="act"
                      select="n1:entry/n1:act"/>
        <xsl:variable name="obsCode"
                      select="n1:entry/n1:act/n1:templateId[@root='2.16.840.1.113883.10.20.1.27']/../n1:entryRelationship[@typeCode='SUBJ']/n1:observation/n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.6']/../n1:value/@code"/>
        <!-- in case of no info scenario the following displayName will be displayed -->
        <xsl:variable name="obsDisplay"
                      select="n1:entry/n1:act/n1:templateId[@root='2.16.840.1.113883.10.20.1.27']/../n1:entryRelationship[@typeCode='SUBJ']/n1:observation/n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.6']/../n1:value/@displayName"/>
        <!-- End definition of variables-->

        <!--- table Display -->
        <xsl:choose>
            <!-- If sectionTitle is not missing for alerts  (Exception alerts section is missing)-->
            <xsl:when test="($sectionTitleCode='48765-2')">
                <div class="wrap-collabsible">
                    <input id="collapsible-alerts-section-original" class="toggle" type="checkbox" checked="true" />
                    <label for="collapsible-alerts-section-original" class="lbl-toggle-title">
                        <xsl:value-of select="$sectionTitle"/>
                    </label>
                    <div class="collapsible-content-title">
                        <div class="content-inner-title">
                            <xsl:choose>
                                <xsl:when test="$shownarrative='true'">
                                    <div class="wrap-collabsible">
                                        <input id="collapsible-alerts-original" class="toggle" type="checkbox"/>
                                        <label for="collapsible-alerts-original" class="lbl-toggle">
                                            <xsl:value-of select="$originalNarrativeTableTitle"/>
                                        </label>
                                        <div class="collapsible-content">
                                            <div class="content-inner">
                                                <xsl:call-template name="show-narrative">
                                                    <xsl:with-param name="node" select="/n1:ClinicalDocument/n1:component/n1:structuredBody/n1:component/n1:section/n1:code[@code='48765-2']/../n1:text"/>
                                                </xsl:call-template>
                                            </div>
                                        </div>
                                    </div>
                                </xsl:when>
                            </xsl:choose>
                            <br/>
                            <!-- nullflavored act -->
                            <div class="wrap-collabsible">
                                <input id="collapsible-alerts-translated" class="toggle" type="checkbox" checked="true"/>
                                <label for="collapsible-alerts-translated" class="lbl-toggle">
                                    <xsl:value-of select="$translatedCodedTableTitle"/>
                                </label>
                                <div class="collapsible-content">
                                    <div class="content-inner">
                                        <xsl:choose>
                                            <xsl:when test="not($act/@nullFlavor)">
                                                <table class="translation_table">
                                                    <tbody>
                                                        <xsl:if test="not ($obsCode='716186003' or $obsCode='409137002')">
                                                            <tr>
                                                                <th>
                                                                    <!-- Reaction Type -->
                                                                    <xsl:call-template name="show-displayLabels">
                                                                        <xsl:with-param name="code" select="'65'"/>
                                                                    </xsl:call-template>
                                                                </th>
                                                                <th>
                                                                    <!-- Clinical Manifestation -->
                                                                    <xsl:call-template name="show-displayLabels">
                                                                        <xsl:with-param name="code" select="'10'"/>
                                                                    </xsl:call-template>
                                                                </th>
                                                                <th>
                                                                    <!-- Agent -->
                                                                    <xsl:call-template name="show-displayLabels">
                                                                        <xsl:with-param name="code" select="'5'"/>
                                                                    </xsl:call-template>
                                                                </th>
                                                                <th>
                                                                    <!-- OnSet Date -->
                                                                    <xsl:call-template name="show-displayLabels">
                                                                        <xsl:with-param name="code" select="'45'"/>
                                                                    </xsl:call-template>
                                                                </th>
                                                            </tr>
                                                        </xsl:if>
                                                        <xsl:for-each select="n1:entry">
                                                            <xsl:call-template name="alertSectionEntry">
                                                            </xsl:call-template>
                                                        </xsl:for-each>
                                                    </tbody>
                                                </table>

                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:call-template name="show-nullFlavor">
                                                    <xsl:with-param name="code" select="$act/@nullFlavor"/>
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
    <!-- alert section entry -->

    <xsl:template name="alertSectionEntry">
        <!-- Defining all needed variables -->
        <xsl:variable
                name="reactionTypeTranslation1"
                select="n1:act/n1:templateId[@root= '2.16.840.1.113883.10.20.1.27']/../n1:entryRelationship[@typeCode='SUBJ']/n1:observation/n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.6']/../n1:code/n1:translation/n1:translation/@displayName"/>

        <xsl:variable
                name="reactionTypeTranslation2"
                select="n1:act/n1:templateId[@root= '2.16.840.1.113883.10.20.1.27']/../n1:entryRelationship[@typeCode='SUBJ']/n1:observation/n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.6']/../n1:code/n1:translation/@displayName"/>

        <xsl:variable
                name="reactionType"
                select="n1:act/n1:templateId[@root= '2.16.840.1.113883.10.20.1.27']/../n1:entryRelationship[@typeCode='SUBJ']/n1:observation/n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.6']/../n1:code"/>

        <xsl:variable name="clinicalManifestation"
                      select="n1:act/n1:templateId[@root='2.16.840.1.113883.10.20.1.27']/../n1:entryRelationship[@typeCode='SUBJ']/n1:observation/n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.6']/../n1:entryRelationship[@typeCode='MFST']/n1:observation/n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.5']/../n1:value/@displayName"/>

        <xsl:variable name="clinicalManifestationTranslation1"
                      select="n1:act/n1:templateId[@root='2.16.840.1.113883.10.20.1.27']/../n1:entryRelationship[@typeCode='SUBJ']/n1:observation/n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.6']/../n1:entryRelationship[@typeCode='MFST']/n1:observation/n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.5']/../n1:value/n1:translation/n1:translation/@displayName"/>

        <xsl:variable name="clinicalManifestationTranslation2"
                      select="n1:act/n1:templateId[@root='2.16.840.1.113883.10.20.1.27']/../n1:entryRelationship[@typeCode='SUBJ']/n1:observation/n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.6']/../n1:entryRelationship[@typeCode='MFST']/n1:observation/n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.5']/../n1:value/n1:translation/@displayName"/>

        <xsl:variable name="clinicalManifestationNode"
                      select="n1:act/n1:templateId[@root='2.16.840.1.113883.10.20.1.27']/../n1:entryRelationship[@typeCode='SUBJ']/n1:observation/n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.6']/../n1:entryRelationship[@typeCode='MFST']/n1:observation/n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.5']/../n1:value"/>

        <xsl:variable name="agentID"
                      select="n1:act/n1:templateId[@root='2.16.840.1.113883.10.20.1.27']/../n1:entryRelationship[@typeCode='SUBJ']/n1:observation/n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.6']/../n1:participant[@typeCode='CSM']/n1:participantRole[@classCode='MANU']/n1:playingEntity[@classCode='MMAT']/n1:code/@code"/>

        <xsl:variable name="agentDescription"
                      select="n1:act/n1:templateId[@root='2.16.840.1.113883.10.20.1.27']/../n1:entryRelationship[@typeCode='SUBJ']/n1:observation/n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.6']/../n1:participant[@typeCode='CSM']/n1:participantRole[@classCode='MANU']/n1:playingEntity[@classCode='MMAT']/n1:code"/>

        <xsl:variable name="onSetDate"
                      select="n1:act/n1:templateId[@root='2.16.840.1.113883.10.20.1.27']/../n1:entryRelationship[@typeCode='SUBJ']/n1:observation/n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.6']/../n1:effectiveTime/n1:low"/>

        <!-- observation value code -->
        <!-- not sure if we have to filter with root and template ids-->
        <!---
        answer by Giorgio for checking the no info scenario
        entry/act[templateId/@root= ‘2.16.840.1.113883.10.20.1.27’]/entryRelationship[@typeCode=’SUBJ’]/observation[templateId/@root=’1.3.6.1.4.1.19376.1.5.3.1.4.6’]/code/@ displayName -->

        <xsl:variable name="obsCode"
                      select="n1:act/n1:templateId[@root='2.16.840.1.113883.10.20.1.27']/../n1:entryRelationship[@typeCode='SUBJ']/n1:observation/n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.6']/../n1:value/@code"/>
        <!-- in case of no info scenario the following displayName will be displayed -->
        <xsl:variable name="obsDisplay"
                      select="n1:act/n1:templateId[@root='2.16.840.1.113883.10.20.1.27']/../n1:entryRelationship[@typeCode='SUBJ']/n1:observation/n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.6']/../n1:value/@displayName"/>

        <xsl:variable name="act" select="n1:act"/>
        <!-- End definition of variables-->

        <xsl:choose>
            <xsl:when test="not($act/@nullFlavor)">
                <!-- No info Scenario... observation.value@code is one of the two values -->
                <xsl:choose>
                    <xsl:when test="($obsCode='716186003' or $obsCode='409137002')">
                        <tr>
                            <td colspan="4">
                                <span class="tdtext">
                                    <xsl:value-of select="$obsDisplay"/>
                                </span>
                                <br/>
                            </td>
                        </tr>
                    </xsl:when>
                    <xsl:otherwise>
                        <tr>
                            <td>
                                <xsl:choose>
                                    <xsl:when test="$reactionType/@nullFlavor">
                                        <xsl:call-template name="show-nullFlavor">
                                            <xsl:with-param name="code" select="$reactionType/@nullFlavor"/>
                                        </xsl:call-template>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="$reactionType/@displayName"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </td>
                            <td>
                                <xsl:choose>
                                    <xsl:when test="$clinicalManifestation">
                                        <xsl:value-of select="$clinicalManifestation"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <!-- uncoded element Problem -->
                                        <xsl:if test="$clinicalManifestationNode/n1:originalText/n1:reference/@value">
                                            <xsl:call-template name="show-uncodedElement">
                                                <xsl:with-param name="code"
                                                                select="$clinicalManifestationNode/n1:originalText/n1:reference/@value"/>
                                            </xsl:call-template>
                                        </xsl:if>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </td>
                            <td>
                                <xsl:choose>
                                    <xsl:when test=" not($agentDescription/@nullFlavor)">
                                        <xsl:choose>
                                            <xsl:when test="$agentDescription/@displayName">
                                                <xsl:value-of select="$agentDescription/@displayName"/>
                                                <br/>(<xsl:value-of select="$agentID"/>)
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <!--  uncoded element -->
                                                <xsl:if test="$agentDescription/n1:originalText/n1:reference/@value">
                                                    <xsl:call-template name="show-uncodedElement">
                                                        <xsl:with-param name="code"
                                                                        select="$agentDescription/n1:originalText/n1:reference/@value"/>
                                                    </xsl:call-template>
                                                </xsl:if>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:call-template name="show-nullFlavor">
                                            <xsl:with-param name="code" select="$agentDescription/@nullFlavor"/>
                                        </xsl:call-template>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </td>
                            <td>
                                <xsl:call-template name="show-time">
                                    <xsl:with-param name="datetime" select="$onSetDate"/>
                                </xsl:call-template>
                                &#160;
                            </td>
                        </tr>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <tr>
                    <td colspan="3">
                        <xsl:call-template name="show-nullFlavor">
                            <xsl:with-param name="code" select="$act/@nullFlavor"/>
                        </xsl:call-template>
                    </td>
                </tr>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>