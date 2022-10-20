<?xml version="1.0"  ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:n1="urn:hl7-org:v3" version="2.0">

    <!--- Extended HEADER ELEMENT -->
    <!-- patient address -->
    <xsl:variable
            name="patientRole"
            select="/n1:ClinicalDocument/n1:recordTarget/n1:patientRole"/>

    <xsl:variable
            name="patientPreferLang"
            select="$patientRole/n1:patient/n1:languageCommunication/n1:languageCode"/>

    <!-- guardian -->
    <xsl:variable
            name="patientGuardian"
            select="$patientRole/n1:patient/n1:guardian"/>

    <!--Preferred HCP/ Legal Organization -->
    <xsl:variable
            name="participantPRS"
            select="/n1:ClinicalDocument/n1:participant/n1:functionCode[@code='PCP'][@codeSystem='2.16.840.1.113883.5.88']/../n1:associatedEntity[@classCode='PRS']"/>

    <!-- Authoring device -->
    <xsl:variable
            name="AuthoringDeviceName"
            select="/n1:ClinicalDocument/n1:author/n1:assignedAuthor/n1:assignedAuthoringDevice"/>


    <!-- Legal Authenticator -->
    <xsl:variable
            name="LegalAuthenticator"
            select="/n1:ClinicalDocument/n1:legalAuthenticator/n1:assignedEntity/n1:assignedPerson"/>

    <xsl:variable
            name="LegalAuthenticator2"
            select="/n1:ClinicalDocument/n1:legalAuthenticator/n1:assignedEntity/n1:representedOrganization"/>

    <!--Custodian-->
    <xsl:variable
            name="patientCustodian"
            select="/n1:ClinicalDocument/n1:custodian/n1:assignedCustodian/n1:representedCustodianOrganization"/>


    <xsl:template name="extendedCdaHeader">
        <div class="container">
            <div class="item"><xsl:call-template name="displayPatientContactInformation"/></div>
            <div class="item"><xsl:call-template name="displayPreferredHCPAndLegalOrganization"/></div>
            <div class="item"><xsl:call-template name="displayAuthors"/></div>
            <div class="item"><xsl:call-template name="displayLegalAuthenticator"/></div>
            <div class="item"><xsl:call-template name="displayOtherContacts"/></div>
            <div class="item"><xsl:call-template name="displayGuardian"/></div>
            <div class="item"><xsl:call-template name="displayCustodian"/></div>
        </div>
    </xsl:template>

    <xsl:template name="displayPatientContactInformation">
        <div class="extended_header_block">
            <table class="extended_header_table">
                <tbody>
                    <!-- Patient Contact Info-->
                    <tr>
                        <th colspan="2">
                            <xsl:call-template name="show-eHDSIDisplayLabel">
                                <xsl:with-param name="code" select="'51'"/>
                            </xsl:call-template>
                        </th>
                    </tr>
                    <tr>
                        <td>
                        </td>
                        <td>
                            <table class="contact_information_table">
                                <tr>
                                    <th>
                                        <xsl:call-template name="show-eHDSIDisplayLabel">
                                            <xsl:with-param name="code" select="'12'"/>
                                        </xsl:call-template>
                                    </th>
                                </tr>
                                <tr>
                                    <td>
                                        <xsl:call-template name="show-contactInfo">
                                            <xsl:with-param name="contact" select="$patientRole"/>
                                        </xsl:call-template>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </xsl:template>

    <xsl:template name="displayPreferredHCPAndLegalOrganization">
        <div class="extended_header_block">
            <table class="extended_header_table">
                <tbody>
                    <tr>
                        <th colspan="2">
                            <!-- Preferred HCP/Legal organization to contact-->
                            <xsl:call-template name="show-eHDSIDisplayLabel">
                                <xsl:with-param name="code" select="'54'"/>
                            </xsl:call-template>
                        </th>
                    </tr>
                    <xsl:for-each select="$participantPRS">
                        <tr>
                            <td>
                                <!-- show person's name and if exists organization name -->
                                <xsl:if test="not(n1:associatedPerson/n1:name/@nullFlavor)">
                                    <xsl:value-of select="n1:associatedPerson/n1:name/n1:given"/>&#160;
                                    <xsl:value-of select="n1:associatedPerson/n1:name/n1:family"/>
                                </xsl:if>
                                <xsl:if test="n1:scopingOrganization">
                                    <xsl:if test="(n1:associatedPerson/n1:name/n1:given or n1:associatedPerson/n1:name/n1:family) and n1:scopingOrganization/n1:name">
                                        ,&#160;
                                    </xsl:if>
                                    <xsl:value-of select="$participantPRS/n1:scopingOrganization/n1:name"/>
                                </xsl:if>
                            </td>
                            <td>
                                <table class="contact_information_table">
                                    <tr>
                                        <th>
                                            <xsl:call-template name="show-eHDSIDisplayLabel">
                                                <xsl:with-param name="code" select="'12'"/>
                                            </xsl:call-template>
                                        </th>
                                    </tr>
                                    <tr>
                                        <td>
                                            <xsl:choose>
                                                <!-- first person's address.. then if not exist show org address -->
                                                <xsl:when test="n1:addr">
                                                    <xsl:call-template name="show-contactInfo">
                                                        <xsl:with-param name="contact" select="."/>
                                                    </xsl:call-template>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <xsl:call-template name="show-contactInfo">
                                                        <xsl:with-param name="contact" select="./n1:scopingOrganization"/>
                                                    </xsl:call-template>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </xsl:for-each>
                </tbody>
            </table>
        </div>
    </xsl:template>

    <xsl:template name="displayAuthors">
        <xsl:for-each select="/n1:ClinicalDocument/n1:author">
            <xsl:variable
                    name="hcpCounter"
                    select="position()"/>
            <xsl:variable
                    name="assignedAuthor"
                    select="/n1:ClinicalDocument/n1:author[$hcpCounter]/n1:assignedAuthor"/>
            <xsl:variable
                    name="assignedPerson"
                    select="/n1:ClinicalDocument/n1:author[$hcpCounter]/n1:assignedAuthor/n1:assignedPerson"/>
            <xsl:variable
                    name="assignedEntity"
                    select="/n1:ClinicalDocument/n1:documentationOf[$hcpCounter]/n1:serviceEvent/n1:performer/n1:assignedEntity"/>
            <xsl:variable
                    name="representedOrganization"
                    select="/n1:ClinicalDocument/n1:author[$hcpCounter]/n1:assignedAuthor/n1:representedOrganization"/>
            <xsl:variable
                    name="functionCode"
                    select="/n1:ClinicalDocument/n1:author[$hcpCounter]/n1:functionCode"/>
            <div class="extended_header_block">
                <table class="extended_header_table">
                    <tbody>
                        <tr>
                            <th colspan="2">
                                <xsl:choose>
                                    <xsl:when test="$assignedPerson">
                                        <!-- Author (HCP) -->
                                        <xsl:call-template name="show-eHDSIDisplayLabel">
                                            <xsl:with-param name="code" select="'7'"/>
                                        </xsl:call-template>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <!-- Authoring Device -->
                                        <xsl:call-template name="show-eHDSIDisplayLabel">
                                            <xsl:with-param name="code" select="'8'"/>
                                        </xsl:call-template>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </th>
                        </tr>
                        <tr>
                            <td>
                                <xsl:choose>
                                    <xsl:when test="$assignedPerson">
                                        <xsl:choose>
                                            <xsl:when test="not($assignedPerson/n1:name/@nullFlavor)">
                                                <xsl:value-of select="$assignedPerson/n1:name/n1:given"/>&#160;
                                                <xsl:value-of select="$assignedPerson/n1:name/n1:family"/>&#160;
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:value-of select="$assignedEntity/n1:assignedPerson/n1:name/n1:given"/>&#160;
                                                <xsl:value-of select="$assignedEntity/n1:assignedPerson/n1:name/n1:family"/>&#160;
                                                <xsl:value-of select="$assignedEntity/n1:representedOrganization/n1:name"/>
                                                <xsl:call-template name="show-contactInfo">
                                                    <xsl:with-param name="contact" select="$assignedEntity/n1:representedOrganization"/>
                                                </xsl:call-template>&#160;
                                            </xsl:otherwise>
                                        </xsl:choose>
                                        <xsl:if test="$functionCode and not($functionCode/@nullFlavor)">
                                            <span class="label otherContacts-roleClass">
                                                <xsl:call-template name="show-eHDSIHealthcareProfessionalRole">
                                                    <xsl:with-param name="node" select="$functionCode"/>
                                                </xsl:call-template>
                                            </span>
                                        </xsl:if>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <!-- Manufacturer Model Name -->
                                        <xsl:value-of select="$AuthoringDeviceName/n1:manufacturerModelName"/>&#160;
                                        <!-- Software Name -->
                                        (<xsl:value-of select="$AuthoringDeviceName/n1:softwareName"/>)
                                    </xsl:otherwise>
                                </xsl:choose>
                            </td>
                            <td>
                                <table class="contact_information_table">
                                    <tr>
                                        <th>
                                            <xsl:call-template name="show-eHDSIDisplayLabel">
                                                <xsl:with-param name="code" select="'12'"/>
                                            </xsl:call-template>
                                        </th>
                                    </tr>
                                    <tr>
                                        <td>
                                            <xsl:call-template name="show-contactInfo">
                                                <xsl:with-param name="contact" select="$assignedAuthor"/>
                                            </xsl:call-template>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                        <tr>
                            <td>
                            </td>
                            <td>
                                <table class="contact_information_table">
                                    <tr>
                                        <th>
                                            <!-- Represented organization -->
                                            <xsl:call-template name="show-eHDSIDisplayLabel">
                                                <xsl:with-param name="code" select="'127'"/>
                                            </xsl:call-template>
                                        </th>
                                    </tr>
                                    <tr>
                                        <td>
                                            <xsl:value-of select="$representedOrganization/n1:name"/>
                                            <xsl:call-template name="show-contactInfo">
                                                <xsl:with-param name="contact" select="$representedOrganization"/>
                                            </xsl:call-template>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="displayLegalAuthenticator">
        <div class="extended_header_block">
            <table class="extended_header_table">
                <tbody>
                    <tr>
                        <th colspan="2">
                            <!-- Legal Authenticator-->
                            <xsl:call-template name="show-eHDSIDisplayLabel">
                                <xsl:with-param name="code" select="'40'"/>
                            </xsl:call-template>
                        </th>
                    </tr>
                    <tr>
                        <td>
                            <xsl:value-of select="$LegalAuthenticator/n1:name/n1:given"/>&#160;
                            <xsl:value-of select="$LegalAuthenticator/n1:name/n1:family"/>
                            <xsl:if test="$LegalAuthenticator2/n1:name and not($LegalAuthenticator2/n1:name/@nullFlavor)">
                                ,&#160;<xsl:value-of select="$LegalAuthenticator2/n1:name"/>&#160;
                            </xsl:if>
                        </td>
                        <td>
                            <table class="contact_information_table">
                                <tr>
                                    <th>
                                        <xsl:call-template name="show-eHDSIDisplayLabel">
                                            <xsl:with-param name="code" select="'12'"/>
                                        </xsl:call-template>
                                    </th>
                                </tr>
                                <tr>
                                    <td>
                                        <xsl:choose>
                                            <xsl:when
                                                    test="n1:legalAuthenticator/n1:assignedEntity/n1:addr | n1:legalAuthenticator/n1:assignedEntity/n1:telecom">
                                                <xsl:call-template name="show-contactInfo">
                                                    <xsl:with-param name="contact" select="n1:legalAuthenticator/n1:assignedEntity"/>
                                                </xsl:call-template>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:call-template name="show-contactInfo">
                                                    <xsl:with-param name="contact" select="$LegalAuthenticator2"/>
                                                </xsl:call-template>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </xsl:template>

    <xsl:template name="displayOtherContacts">
        <div class="extended_header_block">
            <table class="extended_header_table">
                <tbody>
                    <tr>
                        <th colspan="2">
                            <!-- Other Contacts-->
                            <xsl:call-template name="show-eHDSIDisplayLabel">
                                <xsl:with-param name="code" select="'49'"/>
                            </xsl:call-template>
                        </th>
                    </tr>
                    <xsl:for-each select="/n1:ClinicalDocument/n1:participant/n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.2.4']/../n1:associatedEntity">
                        <tr>
                            <xsl:if test="not(../n1:functionCode) or not(../n1:functionCode/@code='PCP')">
                                <xsl:if test="n1:associatedPerson/n1:name/* or n1:scopingOrganization">
                                    <td>
                                        <xsl:value-of select="n1:associatedPerson/n1:name/n1:given"/>&#160;
                                        <xsl:value-of select="n1:associatedPerson/n1:name/n1:family"/>&#160;
                                        <xsl:value-of select="n1:scopingOrganization/n1:name"/>&#160;
                                        <xsl:if test="@classCode">
                                            <span class="label otherContacts-roleClass">
                                                <xsl:call-template name="show-eHDSIRoleClass">
                                                    <xsl:with-param name="code" select="@classCode"/>
                                                </xsl:call-template>
                                            </span>&#160;
                                        </xsl:if>
                                        <xsl:if test="../n1:functionCode and not(../n1:functionCode/@nullFlavor)">
                                            <span class="label otherContacts-personalRelationship">
                                                <xsl:call-template name="show-eHDSIPersonalRelationship">
                                                    <xsl:with-param name="node" select="../n1:functionCode"/>
                                                </xsl:call-template>
                                            </span>
                                        </xsl:if>
                                    </td>
                                    <td>
                                        <table class="contact_information_table">
                                            <tr>
                                                <th>
                                                    <xsl:call-template name="show-eHDSIDisplayLabel">
                                                        <xsl:with-param name="code" select="'12'"/>
                                                    </xsl:call-template>
                                                </th>
                                            </tr>
                                            <tr>
                                                <td>
                                                    <xsl:if test="not(../n1:functionCode) or not(../n1:functionCode/@code='PCP')">
                                                        <xsl:call-template name="show-contactInfo">
                                                            <xsl:with-param name="contact" select="."/>
                                                        </xsl:call-template>
                                                    </xsl:if>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </xsl:if>
                            </xsl:if>
                        </tr>
                    </xsl:for-each>
                </tbody>
            </table>
        </div>
    </xsl:template>

    <xsl:template name="displayGuardian">
        <div class="extended_header_block">
            <table class="extended_header_table">
                <tbody>
                    <tr>
                        <th colspan="2">
                            <!-- Guardian-->
                            <xsl:call-template name="show-eHDSIDisplayLabel">
                                <xsl:with-param name="code" select="'35'"/>
                            </xsl:call-template>
                        </th>
                    </tr>
                    <xsl:for-each select="$patientGuardian">
                        <tr>
                            <td>
                                <xsl:value-of select="n1:guardianPerson/n1:name/n1:given"/>&#160;
                                <xsl:value-of select="n1:guardianPerson/n1:name/n1:family"/>&#160;
                            </td>
                            <td>
                                <table class="contact_information_table">
                                    <tr>
                                        <th>
                                            <xsl:call-template name="show-eHDSIDisplayLabel">
                                                <xsl:with-param name="code" select="'12'"/>
                                            </xsl:call-template>
                                        </th>
                                    </tr>
                                    <tr>
                                        <td>
                                            <xsl:call-template name="show-contactInfo">
                                                <xsl:with-param name="contact" select="."/>
                                            </xsl:call-template>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </xsl:for-each>
                </tbody>
            </table>
        </div>
    </xsl:template>

    <xsl:template name="displayCustodian">
        <div class="extended_header_block">
            <table class="extended_header_table">
                <tbody>
                    <tr>
                        <th colspan="2">
                            <!-- Custodian-->
                            <xsl:call-template name="show-eHDSIDisplayLabel">
                                <xsl:with-param name="code" select="'16'"/>
                            </xsl:call-template>
                        </th>
                    </tr>
                    <tr>
                        <td>
                            <xsl:value-of select="$patientCustodian/n1:name"/>
                        </td>
                        <td>
                            <table class="contact_information_table">
                                <tr>
                                    <th>
                                        <!-- Contact Information -->
                                        <xsl:call-template name="show-eHDSIDisplayLabel">
                                            <xsl:with-param name="code" select="'12'"/>
                                        </xsl:call-template>
                                    </th>
                                </tr>
                                <tr>
                                    <td>
                                        <xsl:call-template name="show-contactInfo">
                                            <xsl:with-param name="contact" select="$patientCustodian"/>
                                        </xsl:call-template>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </xsl:template>
</xsl:stylesheet>