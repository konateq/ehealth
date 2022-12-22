<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:n1="urn:hl7-org:v3"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:epsos="urn:epsos-org:ep:medication"
                version="2.0">

    <xsl:template match="/n1:ClinicalDocument/n1:component/n1:structuredBody/n1:component/n1:section[n1:templateId/@root='1.3.6.1.4.1.12559.11.10.1.3.1.2.1']/n1:entry/n1:substanceAdministration">
        <fieldset style="min-height:100px;">
            <legend>
                <!-- Pharmaceutical Substance Header -->
                <b><xsl:apply-templates select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial"/></b></legend>
            <table>
                <tr>
                    <td>
                        <xsl:call-template name="epPrescriptionItemDetails"/>
                    </td>
                </tr>
            </table>
        </fieldset>
    </xsl:template>

    <!-- Pharmaceutical Substance Header -->
    <xsl:template match="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial">
        &#160;
        <xsl:value-of select="n1:name"/>
        <xsl:choose>
            <xsl:when test="epsos:asSpecializedKind">
                <xsl:variable name="code" select="epsos:asSpecializedKind/epsos:generalizedMedicineClass/epsos:code"/>
                <xsl:choose>
                    <xsl:when test="not($code/@nullFlavor)">
                        <xsl:if test="$code/@codeSystem='2.16.840.1.113883.6.73'">
                            (<span class="codeSystem">ATC</span>
                            &#160;
                            <span class="codeSystem">
                            <xsl:value-of select="$code/@code"/>
                            </span>
                            &#160;
                            <xsl:call-template name="show-eHDSIActiveIngredient">
                                <xsl:with-param name="node" select="$code"/>
                            </xsl:call-template>
                            )
                        </xsl:if>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text> - </xsl:text>
                        <xsl:value-of select="epsos:asSpecializedKind/epsos:generalizedMedicineClass/epsos:name"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
        </xsl:choose>
        &#160;
    </xsl:template>

    <xsl:template name="package">
        <xsl:call-template name="show-package">
            <xsl:with-param name="medPackage"
                            select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:asContent/epsos:containerPackagedMedicine/epsos:capacityQuantity"/>
            <xsl:with-param name="showValue" select="'YES'"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="frequency">
        <xsl:call-template name="show-frequencyIntake">
            <xsl:with-param name="medFrequencyIntakeType" select="n1:effectiveTime[2]/@xsi:type"/>
            <xsl:with-param name="medFrequencyIntake" select="n1:effectiveTime[2]"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="add-underscore">
        <xsl:param name="variable"/>
        <xsl:value-of select="$variable"/>
        <xsl:value-of select="'_'"/>
        <xsl:value-of select="position()-1"/>
    </xsl:template>

    <xsl:template name="add-javascript-variable">
        <xsl:param name="variable"/>
        <xsl:param name="name"/>
        <xsl:value-of select="'var '"/>
        <xsl:value-of select="$variable"/>
        <xsl:value-of select="'_'"/>
        <xsl:value-of select="position()-1"/>
        <xsl:value-of select="'=document.getElementById('"/>
        <xsl:text>&apos;</xsl:text>
        <xsl:value-of select="$name"/>
        <xsl:value-of select="'_'"/>
        <xsl:value-of select="position()-1"/>
        <xsl:text>&apos;</xsl:text>
        <xsl:value-of select="');'"/>
    </xsl:template>

    <xsl:template name="number-of-packages">
        <xsl:param name="supply"/>
        <xsl:choose>
            <xsl:when test="$supply/n1:supply/@moodCode='RQO'">
                <xsl:choose>
                    <xsl:when test="$supply/n1:supply/n1:independentInd/@value='false'">
                        <xsl:value-of select="$supply/n1:supply/n1:quantity/@value"/>
                    </xsl:when>
                </xsl:choose>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="pure-substitution-code">
        <xsl:variable name="code" select="n1:entryRelationship[@typeCode='SUBJ'][@inversionInd='true']/n1:observation[@classCode='OBS']"/>
        <xsl:choose>
            <xsl:when test="$code/n1:code/@code='SUBST'">
                <xsl:choose>
                    <xsl:when test="$code/n1:code/@codeSystem='2.16.840.1.113883.5.6'">
                        <xsl:choose>
                            <xsl:when test="$code/n1:value/@code='N'">
                                <xsl:value-of select="'No'"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="'Yes'"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="'Yes'"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="'Yes'"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="substitution-code">
        <xsl:variable name="code">
            <xsl:call-template name="pure-substitution-code"/>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$code='No'">
                <xsl:call-template name="show-eHDSIDisplayLabel">
                    <xsl:with-param name="code" select="'42'"/>
                </xsl:call-template>
                <!-- <xsl:value-of select="'No'"/>  -->
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="show-eHDSIDisplayLabel">
                    <xsl:with-param name="code" select="'81'"/>
                </xsl:call-template>
                <!--  <xsl:value-of select="'Yes'"/> -->
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="show-active-ingredient">
        <xsl:param name="code"/>
        <xsl:param name="name"/>
        <xsl:param name="strength"/>
        <tr>
            <td>
                <xsl:if test="$code/@codeSystem and $code/@codeSystem='2.16.840.1.113883.6.73'">ATC</xsl:if>
            </td>
            <td>
                <xsl:if test="$code/@code and $code/@codeSystem='2.16.840.1.113883.6.73'">
                    <xsl:choose>
                        <xsl:when test="not($code/@nullFlavor)">
                            <xsl:value-of select="$code/@code"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:call-template name="show-eHDSINullFlavor">
                                <xsl:with-param name="code" select="$code/@nullFlavor"/>
                            </xsl:call-template>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>
            </td>
            <td>
                <xsl:choose>
                    <xsl:when test="not($code/@nullFlavor)">
                        <xsl:call-template name="show-eHDSIActiveIngredient">
                            <xsl:with-param name="node" select="$code"/>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:choose>
                            <xsl:when test="not($name)">
                                <xsl:call-template name="show-eHDSINullFlavor">
                                    <xsl:with-param name="code" select="code/@nullFlavor"/>
                                </xsl:call-template>
                            </xsl:when>
                            <xsl:otherwise>
                                <div class="tooltip">
                                    <span class="narrative">
                                        <xsl:value-of select="$name"/>
                                    </span>
                                    <xsl:text>&#10;</xsl:text>
                                    <span class="tooltiptext">Narrative text</span>
                                </div>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:otherwise>
                </xsl:choose>
            </td>
            <td>
                <xsl:call-template name="show-strength">
                    <xsl:with-param name="node" select="$strength"/>
                </xsl:call-template>
            </td>
        </tr>
    </xsl:template>

    <xsl:variable name="numberOfPacks">
        <xsl:call-template name="number-of-packages">
            <xsl:with-param name="supply" select="n1:entryRelationship[@typeCode='COMP']"/>
        </xsl:call-template>
    </xsl:variable>

    <xsl:template name="inputform">
        <xsl:param name="txt"/>
        <xsl:param name="val"/>
        <xsl:attribute name="id">
            <xsl:value-of select="$txt"/>
            <xsl:value-of select="position()-1"/>
        </xsl:attribute>
        <xsl:attribute name="name">
            <xsl:value-of select="$txt"/>
            <xsl:value-of select="position()-1"/>
        </xsl:attribute>
        <xsl:attribute name="value">
            <xsl:value-of select="$val"/>
        </xsl:attribute>
    </xsl:template>

    <xsl:template name="inputformID">
        <xsl:param name="txt"/>
        <xsl:param name="root"/>
        <xsl:param name="extension"/>
        <xsl:attribute name="id">
            <xsl:value-of select="$txt"/>
            <xsl:value-of select="position()-1"/>
        </xsl:attribute>
        <xsl:attribute name="name">
            <xsl:value-of select="$txt"/>
            <xsl:value-of select="position()-1"/>
        </xsl:attribute>
        <xsl:attribute name="value">
            <xsl:choose>
                <xsl:when test="$extension">
                    <xsl:value-of select="concat($root, '^', $extension)"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$root"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:attribute>
    </xsl:template>

    <xsl:template name="add-newUnitMeasure">
        <xsl:param name="unit"/>
        <xsl:text disable-output-escaping="yes">&lt;option value=&quot;</xsl:text>
        <xsl:value-of select="$unit"/>
        <xsl:text disable-output-escaping="yes">&quot; selected=&quot;selected&quot;&gt;</xsl:text>
        <xsl:value-of select="$unit"/>
        <xsl:text disable-output-escaping="yes">&lt;/option&gt;</xsl:text>
    </xsl:template>

    <xsl:template name="check-Parameter">
        <xsl:param name="parameter"/>
        <xsl:value-of select="''"/>
        <xsl:choose>
            <xsl:when test="$parameter">
                <xsl:value-of select="'false'"/>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="check-EffectiveTime">
        <xsl:value-of select="''"/>
        <xsl:choose>
            <xsl:when test="n1:effectiveTime[2]/n1:period/@nullFlavor">
                <xsl:value-of select="'false'"/>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="epPrescriptionItemDetails">
        <xsl:variable name="manufacturedMaterialStrength"
                      select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:desc"/>
        <xsl:attribute name="id">
            <xsl:text>mytable</xsl:text>
            <xsl:value-of select="position()"/>
        </xsl:attribute>
        <form name="DISPENSE_FORM" method="post">
            <xsl:attribute name="action">
                <xsl:value-of select="$actionpath"/>
            </xsl:attribute>
            <table>
                <tr>
                    <td>
                        <!-- Medicinal Product block -->
                        <fieldset style="min-height:100px;">
                            <legend>
                                <!-- Medicinal Product -->
                                <xsl:call-template name="show-eHDSIDisplayLabel">
                                    <xsl:with-param name="code" select="'128'"/>
                                </xsl:call-template>
                            </legend>
                            <table class="ep_table">
                                <tr>
                                    <th>
                                        <!-- Active Ingredient -->
                                        <xsl:call-template name="show-eHDSIDisplayLabel">
                                            <xsl:with-param name="code" select="'1'"/>
                                        </xsl:call-template>
                                    </th>
                                    <td>
                                        <xsl:call-template name="show-active-ingredients"/>
                                    </td>
                                </tr>
                                <tr>
                                    <th>
                                        <!-- Brand Name -->
                                        <xsl:call-template name="show-eHDSIDisplayLabel">
                                            <xsl:with-param name="code" select="'9'"/>
                                        </xsl:call-template>
                                    </th>
                                    <td>
                                        <xsl:value-of select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/n1:name"/>
                                    </td>
                                </tr>
                                <xsl:if test="n1:consumable/n1:manufacturedProduct/epsos:marketingAuthorizationHolder">
                                    <tr>
                                        <th>
                                            <!-- Marketing Authorization Holder -->
                                            <xsl:call-template name="show-eHDSIDisplayLabel">
                                                <xsl:with-param name="code" select="'124'"/>
                                            </xsl:call-template>
                                        </th>
                                        <td>
                                            <xsl:value-of select="n1:consumable/n1:manufacturedProduct/epsos:marketingAuthorizationHolder/epsos:name"/>
                                        </td>
                                    </tr>
                                </xsl:if>
                                <xsl:if test="$manufacturedMaterialStrength">
                                    <tr>
                                        <th>
                                            <!-- Strength -->
                                            <xsl:call-template name="show-eHDSIDisplayLabel">
                                                <xsl:with-param name="code" select="'70'"/>
                                            </xsl:call-template>
                                        </th>
                                        <td>
                                            <xsl:call-template name="show-manufacturedMaterialStrength">
                                                <xsl:with-param name="parameter"
                                                                select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:desc"/>
                                            </xsl:call-template>
                                        </td>
                                    </tr>
                                </xsl:if>
                                <tr>
                                    <th>
                                        <!-- Package Size -->
                                        <xsl:call-template name="show-eHDSIDisplayLabel">
                                            <xsl:with-param name="code" select="'50'"/>
                                        </xsl:call-template>
                                    </th>
                                    <td>
                                        <xsl:variable name="hasPackage">
                                            <xsl:call-template name="check-Parameter">
                                                <xsl:with-param name="parameter"
                                                                select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:asContent/epsos:containerPackagedMedicine/epsos:capacityQuantity/@nullFlavor"/>
                                            </xsl:call-template>
                                        </xsl:variable>
                                        <xsl:choose>
                                            <xsl:when test="$hasPackage = 'false'">
                                                <xsl:call-template name="show-eHDSINullFlavor">
                                                    <xsl:with-param name="code"
                                                                    select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:asContent/epsos:containerPackagedMedicine/epsos:capacityQuantity/@nullFlavor"/>
                                                </xsl:call-template>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:call-template name="package"/>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </td>
                                </tr>
                                <tr>
                                    <th>
                                        <!--  Dose Form -->
                                        <xsl:call-template name="show-eHDSIDisplayLabel">
                                            <xsl:with-param name="code" select="'25'"/>
                                        </xsl:call-template>
                                    </th>
                                    <td>
                                        <xsl:call-template name="show-formCode">
                                            <xsl:with-param name="parameter"
                                                            select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:formCode"/>
                                        </xsl:call-template>
                                    </td>
                                </tr>
                                <tr>
                                    <th>
                                        <!-- Route of Administration -->
                                        <xsl:call-template name="show-eHDSIDisplayLabel">
                                            <xsl:with-param name="code" select="'67'"/>
                                        </xsl:call-template>
                                    </th>
                                    <td>
                                        <!-- RoA -->
                                        <xsl:call-template name="show-eHDSIRouteOfAdministration">
                                            <xsl:with-param name="node" select="n1:routeCode"/>
                                        </xsl:call-template>
                                    </td>
                                </tr>
                            </table>
                        </fieldset>
                    </td>
                    <td>
                        <!-- Prescription details block -->
                        <fieldset style="min-height:100px;">
                            <legend>
                                <!-- Prescription Details -->
                                <xsl:call-template name="show-eHDSIDisplayLabel">
                                    <xsl:with-param name="code" select="'129'"/>
                                </xsl:call-template>
                            </legend>
                            <table class="ep_table">
                                <tr>
                                    <th>
                                        <!-- Duration of treatment -->
                                        <xsl:call-template name="show-eHDSIDisplayLabel">
                                            <xsl:with-param name="code" select="'150'"/>
                                        </xsl:call-template>
                                    </th>
                                    <td>
                                        <xsl:call-template name="show-IVL_TS">
                                            <xsl:with-param name="node"
                                                            select="n1:effectiveTime[1][@xsi:type='IVL_TS' or substring-after(@xsi:type, ':')='IVL_TS']"/>
                                        </xsl:call-template>
                                    </td>
                                </tr>
                                <tr>
                                    <th>
                                        <!-- Units per intake -->
                                        <xsl:call-template name="show-eHDSIDisplayLabel">
                                            <xsl:with-param name="code" select="'78'"/>
                                        </xsl:call-template>
                                    </th>
                                    <td>
                                        <xsl:call-template name="show-IVL_PQ">
                                            <xsl:with-param name="node" select="n1:doseQuantity"/>
                                        </xsl:call-template>
                                    </td>
                                </tr>
                                <tr>
                                    <th>
                                        <!-- Frequency of Intakes -->
                                        <xsl:call-template name="show-eHDSIDisplayLabel">
                                            <xsl:with-param name="code" select="'32'"/>
                                        </xsl:call-template>
                                    </th>
                                    <td>
                                        <xsl:variable name="hasFrequency">
                                            <xsl:call-template name="check-Parameter">
                                                <xsl:with-param name="parameter" select="n1:effectiveTime[2]/n1:period/@nullFlavor"/>
                                            </xsl:call-template>
                                        </xsl:variable>

                                        <xsl:variable name="intakesFrequency">
                                            <xsl:call-template name="check-Parameter">
                                                <xsl:with-param name="parameter" select="n1:effectiveTime[2]/@nullFlavor"/>
                                            </xsl:call-template>
                                        </xsl:variable>

                                        <xsl:choose>
                                            <xsl:when test="$intakesFrequency = 'false'">
                                                <xsl:call-template name="show-eHDSINullFlavor">
                                                    <xsl:with-param name="code" select="n1:effectiveTime[2]/@nullFlavor"/>
                                                </xsl:call-template>

                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:choose>
                                                    <xsl:when test="$hasFrequency = 'false'">
                                                        <xsl:call-template name="show-eHDSINullFlavor">
                                                            <xsl:with-param name="code"
                                                                            select="n1:effectiveTime[2]/n1:period/@nullFlavor"/>
                                                        </xsl:call-template>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:call-template name="frequency"/>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </td>
                                </tr>
                                <tr>
                                    <th>
                                        <!-- Number of Packages -->
                                        <xsl:call-template name="show-eHDSIDisplayLabel">
                                            <xsl:with-param name="code" select="'43'"/>
                                        </xsl:call-template>
                                    </th>
                                    <td>
                                        <xsl:call-template name="number-of-packages">
                                            <xsl:with-param name="supply" select="n1:entryRelationship[@typeCode='COMP']"/>
                                        </xsl:call-template>
                                    </td>
                                </tr>
                                <tr>
                                    <th>
                                        <!--   Instructions to patient:-->
                                        <span class="td_label">
                                            <xsl:call-template name="show-eHDSIDisplayLabel">
                                                <xsl:with-param name="code" select="'37'"/>
                                            </xsl:call-template>
                                        </span>
                                    </th>
                                    <td>
                                        <xsl:call-template name="show-text">
                                            <xsl:with-param name="txt"
                                                            select="n1:entryRelationship[@typeCode='SUBJ']/n1:act[n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.3'] ]/n1:text"/>
                                        </xsl:call-template>
                                    </td>
                                </tr>
                                <tr>
                                    <th>
                                        <!--  Advise to the dispenser:-->
                                        <span class="td_label">
                                            <xsl:call-template name="show-eHDSIDisplayLabel">
                                                <xsl:with-param name="code" select="'4'"/>
                                            </xsl:call-template>
                                        </span>
                                    </th>
                                    <td>
                                        <xsl:call-template name="show-text">
                                            <xsl:with-param name="txt"
                                                            select="n1:entryRelationship[@typeCode='SUBJ']/n1:act[n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.3.1'] ]/n1:text"/>
                                        </xsl:call-template>
                                    </td>
                                </tr>
                                <tr>
                                    <th>
                                        <xsl:call-template name="show-eHDSIDisplayLabel">
                                            <xsl:with-param name="code" select="'38'"/>
                                        </xsl:call-template>
                                    </th>
                                    <td>
                                        <xsl:call-template name="substitution-code"/>
                                    </td>
                                </tr>
                            </table>
                        </fieldset>
                    </td>
                </tr>
                <tr>
                    <td colspan="2">
                        <!-- Dispensation details block -->
                        <fieldset style="min-height:100px;">
                            <legend>
                                <!-- Dispensation details -->
                                <xsl:call-template name="show-eHDSIDisplayLabel">
                                    <xsl:with-param name="code" select="'130'"/>
                                </xsl:call-template>
                            </legend>
                            <xsl:variable name="substitutionValue">
                                <xsl:call-template name="pure-substitution-code"/>
                            </xsl:variable>
                            <table class="ep_table">
                                <tbody>
                                    <tr>
                                        <th>
                                            <span class="td_label"><!--  Substitute:-->
                                                <xsl:call-template name="show-eHDSIDisplayLabel">
                                                    <xsl:with-param name="code" select="'71'"/>
                                                </xsl:call-template>
                                                *
                                            </span>
                                        </th>
                                        <td colspan="2">
                                            <input type="checkbox" id="substituted_0" name="substituted_0" style="display:inline">
                                                <xsl:if test="$substitutionValue != 'Yes'">
                                                    <xsl:attribute name="disabled"/>
                                                </xsl:if>
                                            </input>
                                            <!--  Substitution help text:-->
                                            <xsl:call-template name="show-eHDSIDisplayLabel">
                                                <xsl:with-param name="code" select="'202'"/>
                                            </xsl:call-template>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td rowspan="3"/>
                                        <th>
                                            <!--  Dispensed Product:-->
                                            <span class="td_label">
                                                <xsl:call-template name="show-eHDSIDisplayLabel">
                                                    <xsl:with-param name="code" select="'24'"/>
                                                </xsl:call-template>
                                            </span>
                                        </th>
                                        <td>
                                            <input type="text" size="50">
                                                <xsl:attribute name="id">
                                                    <xsl:text>dispensedProductValue_</xsl:text>
                                                    <xsl:value-of select="position()-1"/>
                                                </xsl:attribute>
                                                <xsl:attribute name="name">
                                                    <xsl:text>dispensedProductValue_</xsl:text>
                                                    <xsl:value-of select="position()-1"/>
                                                </xsl:attribute>
                                                <xsl:attribute name="value">
                                                    <xsl:value-of
                                                            select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/n1:name"/>
                                                </xsl:attribute>
                                            </input>
                                        </td>
                                    </tr>
                                    <tr>
                                        <th>
                                            <!-- Dispensed Package Size:-->
                                            <span class="td_label">
                                                <xsl:call-template name="show-eHDSIDisplayLabel">
                                                    <xsl:with-param name="code" select="'23'"/>
                                                </xsl:call-template>
                                            </span>
                                        </th>
                                        <td>
                                            <input type="text" size="50">
                                                <xsl:attribute name="id">
                                                    <xsl:text>dispensedPackageSize_</xsl:text>
                                                    <xsl:value-of select="position()-1"/>
                                                </xsl:attribute>
                                                <xsl:attribute name="name">
                                                    <xsl:text>dispensedPackageSize_</xsl:text>
                                                    <xsl:value-of select="position()-1"/>
                                                </xsl:attribute>
                                                <xsl:attribute name="value">
                                                    <xsl:value-of
                                                            select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:asContent/epsos:containerPackagedMedicine/epsos:capacityQuantity/@value"/>
                                                </xsl:attribute>
                                                <xsl:if test="$substitutionValue != 'Yes'">
                                                    <xsl:attribute name="disabled"/>
                                                </xsl:if>
                                            </input>
                                            <xsl:call-template name="show-package">
                                                <xsl:with-param name="medPackage"
                                                                select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:asContent/epsos:containerPackagedMedicine/epsos:capacityQuantity"/>
                                                <xsl:with-param name="showValue" select="'NO'"/>
                                            </xsl:call-template>
                                        </td>
                                    </tr>
                                    <tr>
                                        <th>
                                            <!-- Dispensed Number of Packages:-->
                                            <span class="td_label">
                                                <xsl:call-template name="show-eHDSIDisplayLabel">
                                                    <xsl:with-param name="code" select="'115'"/>
                                                </xsl:call-template>
                                            </span>
                                        </th>
                                        <td>
                                            <input type="text" size="50">
                                                <xsl:attribute name="id">
                                                    <xsl:text>dispensedNumberOfPackages_</xsl:text>
                                                    <xsl:value-of select="position()-1"/>
                                                </xsl:attribute>
                                                <xsl:attribute name="name">
                                                    <xsl:text>dispensedNumberOfPackages_</xsl:text>
                                                    <xsl:value-of select="position()-1"/>
                                                </xsl:attribute>
                                                <xsl:attribute name="value">
                                                    <xsl:call-template name="number-of-packages">
                                                        <xsl:with-param name="supply" select="n1:entryRelationship[@typeCode='COMP']"/>
                                                    </xsl:call-template>
                                                </xsl:attribute>
                                                <xsl:if test="$substitutionValue != 'Yes'">
                                                    <xsl:attribute name="disabled"/>
                                                </xsl:if>
                                            </input>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td colspan="3">
                                            <!-- HOTFIX - Has to be replaced with a value from the epSOSDisplayLabel value set -->
                                            *  If substitution of brand name is marked as not allowed, pharmacists may still consider dispensing the national equivalent even though the brand name might be slightly different.<br/>
                                            &#160;&#160;This is a known situation: the same pharmaceutical company is marketing the same medicinal product in different countries with slightly different names due to marketing reasons.<br/>
                                            &#160;&#160;If the pharmacist is certain that this is the case, the systems allows the input of the new brand name.<br/>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="text-align:center;vertical-align:middle;">
                                            <!--  Dispense -->
                                            <xsl:choose>
                                                <xsl:when test="$allowDispense='true'">
                                                    <input type="submit" class="button">
                                                        <xsl:attribute name="value">
                                                            <xsl:call-template name="show-eHDSIDisplayLabel">
                                                                <xsl:with-param name="code" select="'22'"/>
                                                            </xsl:call-template>
                                                        </xsl:attribute>
                                                    </input>
                                                </xsl:when>
                                            </xsl:choose>
                                            <input type="hidden" name="prescriptionID" id="prescriptionID">
                                                <xsl:attribute name="value">
                                                    <xsl:value-of select="/n1:ClinicalDocument/n1:component/n1:structuredBody/n1:component/n1:section[n1:templateId/@root='1.3.6.1.4.1.12559.11.10.1.3.1.2.1']/n1:id/@extension"/>
                                                </xsl:attribute>
                                            </input>
                                            <input type="hidden">
                                                <xsl:call-template name="inputformID">
                                                    <xsl:with-param name="txt" select="'dispensationid_'"/>
                                                    <xsl:with-param name="root" select="n1:id/@root"/>
                                                    <xsl:with-param name="extension" select="n1:id/@extension"/>
                                                </xsl:call-template>
                                            </input>
                                            <xsl:variable name="currentPackageFormName">
                                                <xsl:call-template name="show-formCode">
                                                    <xsl:with-param name="parameter"
                                                                    select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:formCode"/>
                                                </xsl:call-template>
                                            </xsl:variable>
                                            <input type="hidden">
                                                <xsl:call-template name="inputform">
                                                    <xsl:with-param name="txt" select="'packaging1_'"/>
                                                    <xsl:with-param name="val" select="$currentPackageFormName"/>
                                                </xsl:call-template>
                                            </input>
                                            <xsl:variable name="currentDoseFormName">
                                                <xsl:call-template name="show-formCode">
                                                    <xsl:with-param name="parameter"
                                                                    select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:asContent/epsos:containerPackagedMedicine/epsos:formCode"/>
                                                </xsl:call-template>
                                            </xsl:variable>
                                            <input type="hidden">
                                                <xsl:call-template name="inputform">
                                                    <xsl:with-param name="txt" select="'packaging2_'"/>
                                                    <xsl:with-param name="val" select="$currentDoseFormName"/>
                                                </xsl:call-template>
                                            </input>
                                            <input type="hidden">
                                                <xsl:call-template name="inputform">
                                                    <xsl:with-param name="txt" select="'packaging3_'"/>
                                                    <xsl:with-param name="val"
                                                                    select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:asContent/epsos:quantity/epsos:numerator/@value"/>
                                                </xsl:call-template>
                                            </input>

                                            <xsl:variable name="currentIngredient">
                                                <xsl:call-template name="show-formCode">
                                                    <xsl:with-param name="parameter"
                                                                    select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:ingredient[@classCode='ACTI']/epsos:ingredient/epsos:code"/>
                                                </xsl:call-template>
                                            </xsl:variable>
                                            <input type="hidden">
                                                <xsl:call-template name="inputform">
                                                    <xsl:with-param name="txt" select="'ingredient_'"/>
                                                    <xsl:with-param name="val" select="$currentIngredient"/>
                                                </xsl:call-template>
                                            </input>
                                            <input type="hidden">
                                                <xsl:attribute name="id">
                                                    <xsl:text>name_</xsl:text>
                                                    <xsl:value-of select="position()-1"/>
                                                </xsl:attribute>
                                                <xsl:attribute name="name">
                                                    <xsl:text>name_</xsl:text>
                                                    <xsl:value-of select="position()-1"/>
                                                </xsl:attribute>
                                                <xsl:attribute name="value">
                                                    <xsl:call-template name="show-strength">
                                                        <xsl:with-param name="node" select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:ingredient[@classCode='ACTI'][position()-1]/epsos:quantity"/>
                                                    </xsl:call-template>
                                                </xsl:attribute>
                                            </input>
                                            <input type="hidden">
                                                <xsl:attribute name="id">
                                                    <xsl:text>nrOfPacks_</xsl:text>
                                                    <xsl:value-of select="position()-1"/>
                                                </xsl:attribute>
                                                <xsl:attribute name="name">
                                                    <xsl:text>nrOfPacks_</xsl:text>
                                                    <xsl:value-of select="position()-1"/>
                                                </xsl:attribute>
                                                <xsl:attribute name="value">
                                                    <xsl:call-template name="number-of-packages">
                                                        <xsl:with-param name="supply" select="n1:entryRelationship[@typeCode='COMP']"/>
                                                    </xsl:call-template>
                                                </xsl:attribute>
                                            </input>
                                            <input type="hidden">
                                                <xsl:attribute name="id">
                                                    <xsl:text>country</xsl:text>
                                                </xsl:attribute>
                                                <xsl:attribute name="name">
                                                    <xsl:text>country</xsl:text>
                                                </xsl:attribute>
                                                <xsl:attribute name="value">
                                                    <xsl:value-of
                                                            select=" /n1:ClinicalDocument/n1:recordTarget/n1:patientRole/n1:addr/n1:country"/>
                                                </xsl:attribute>
                                            </input>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </fieldset>
                    </td>
                </tr>
            </table>
        </form>
        <script>
            <xsl:attribute name="type">
                <xsl:text>text/javascript</xsl:text>
            </xsl:attribute>
            <xsl:if test="position()>1">
                <xsl:text disable-output-escaping="yes">showhide(&apos;mytable</xsl:text>
                <xsl:value-of select="position()"/>
                <xsl:text disable-output-escaping="yes">&apos;);</xsl:text>&#160;
            </xsl:if>

            <!-- Check for the value of the checkbox -->
            <xsl:call-template name="add-javascript-variable">
                <xsl:with-param name="variable" select="'cb'"/>
                <xsl:with-param name="name" select="'dispense'"/>
            </xsl:call-template>&#160;
            <xsl:call-template name="add-javascript-variable">
                <xsl:with-param name="variable" select="'productValue'"/>
                <xsl:with-param name="name" select="'dispensedProductValue'"/>
            </xsl:call-template>&#160;
            <xsl:variable name="substitutionValueCheck">
                <xsl:call-template name="pure-substitution-code"/>
            </xsl:variable>
            <xsl:choose>
                <xsl:when test="$substitutionValueCheck ='Yes'">
                    <xsl:call-template name="add-javascript-variable">
                        <xsl:with-param name="variable" select="'measures'"/>
                        <xsl:with-param name="name" select="'measures'"/>
                    </xsl:call-template>&#160;
                </xsl:when>
            </xsl:choose>
            <xsl:text disable-output-escaping="yes">if  (</xsl:text>
            <xsl:call-template name="add-underscore">
                <xsl:with-param name="variable" select="'cb'"/>
            </xsl:call-template>
            <xsl:text disable-output-escaping="yes">.checked == 0 ) { </xsl:text> &#160;
            <xsl:call-template name="add-underscore">
                <xsl:with-param name="variable" select="'productValue'"/>
            </xsl:call-template>
            <xsl:text disable-output-escaping="yes">.disabled=&apos;true&apos;;</xsl:text>&#160;
            <xsl:choose>
                <xsl:when test="$substitutionValueCheck ='Yes'">
                    <xsl:call-template name="add-underscore">
                        <xsl:with-param name="variable" select="'measures'"/>
                    </xsl:call-template>
                    <xsl:text disable-output-escaping="yes">.disabled=&apos;true&apos;;</xsl:text>&#160;
                </xsl:when>
            </xsl:choose>
            <xsl:text disable-output-escaping="yes">};</xsl:text>&#160;
            <xsl:text disable-output-escaping="yes">function </xsl:text>
            <xsl:call-template name="add-underscore">
                <xsl:with-param name="variable" select="'enableValues'"/>
            </xsl:call-template>
            <xsl:text disable-output-escaping="yes">(){</xsl:text>&#160;
            <xsl:text disable-output-escaping="yes">if  (</xsl:text>
            <xsl:call-template name="add-underscore">
                <xsl:with-param name="variable" select="'cb'"/>
            </xsl:call-template>
            <xsl:text disable-output-escaping="yes">.checked == 0 ) { </xsl:text> &#160;
            <xsl:call-template name="add-underscore">
                <xsl:with-param name="variable" select="'productValue'"/>
            </xsl:call-template>
            <xsl:text disable-output-escaping="yes">.disabled=&apos;true&apos;; </xsl:text>
            <xsl:choose>
                <xsl:when test="$substitutionValueCheck ='Yes'">
                    <xsl:call-template name="add-underscore">
                        <xsl:with-param name="variable" select="'measures'"/>
                    </xsl:call-template>
                    <xsl:text disable-output-escaping="yes">.disabled=&apos;true&apos;;</xsl:text>&#160;
                </xsl:when>
            </xsl:choose>
            <xsl:text> } else {</xsl:text>&#160;
            <xsl:call-template name="add-underscore">
                <xsl:with-param name="variable" select="'productValue'"/>
            </xsl:call-template>
            <xsl:text disable-output-escaping="yes">};</xsl:text>&#160;
            <xsl:text disable-output-escaping="yes">};</xsl:text>
        </script>
    </xsl:template>

    <xsl:template name="check-FrequencyOfIntakes">
        <xsl:value-of select="''"/>
        <xsl:choose>
            <xsl:when test="n1:effectiveTime[2]/@nullFlavor">
                <xsl:value-of select="'false'"/>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="show-active-ingredients">
        <table class="ingredients_table" width="100%">
            <colgroup>
                <col span="1" style="width: 15%;"/>
                <col span="1" style="width: 10%;"/>
                <col span="1" style="width: 45%;"/>
                <col span="1" style="width: 30%;"/>
            </colgroup>

            <tbody>
                <tr>
                    <th>
                        <!-- Code System -->
                        <xsl:call-template name="show-eHDSIDisplayLabel">
                            <xsl:with-param name="code" select="'112'"/>
                        </xsl:call-template>
                    </th>
                    <th>
                        <!-- Code -->
                        <xsl:call-template name="show-eHDSIDisplayLabel">
                            <xsl:with-param name="code" select="'113'"/>
                        </xsl:call-template>
                    </th>
                    <th>
                        <!-- Name -->
                        <xsl:call-template name="show-eHDSIDisplayLabel">
                            <xsl:with-param name="code" select="'114'"/>
                        </xsl:call-template>
                    </th>
                    <th>
                        <!-- Strength -->
                        <xsl:call-template name="show-eHDSIDisplayLabel">
                            <xsl:with-param name="code" select="'70'"/>
                        </xsl:call-template>
                    </th>
                </tr>
                <xsl:for-each select="$activeIngredient">
                    <xsl:call-template name="show-active-ingredient">
                        <xsl:with-param name="code"
                                        select="epsos:ingredient/epsos:code"/>
                        <xsl:with-param name="name"
                                        select="epsos:ingredient/epsos:name"/>
                        <xsl:with-param name="strength"
                                        select="epsos:quantity"/>
                    </xsl:call-template>
                </xsl:for-each>
            </tbody>
        </table>
    </xsl:template>

</xsl:stylesheet>