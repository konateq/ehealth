<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:n1="urn:hl7-org:v3"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:epsos="urn:epsos-org:ep:medication"
                version="1.0">

    <xsl:output method="html"
                indent="yes"
                version="4.01"
                doctype-system="http://www.w3.org/TR/html4/strict.dtd"
                doctype-public="-//W3C//DTD HTML 4.01//EN"/>

    <xsl:template name="strength">
        <xsl:call-template name="show-strength">
            <xsl:with-param name="medStrengthNumerator"
                            select="epsos:numerator"/>
            <xsl:with-param name="medStrengthDenominator"
                            select="epsos:denominator"/>
        </xsl:call-template>
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

    <!--<xsl:template name="intakes">-->
        <!--<xsl:call-template name="show-numberUnitIntake">-->
            <!--<xsl:with-param name="medUnitIntakeLow" select="n1:doseQuantity/n1:low/@value"/>-->
            <!--<xsl:with-param name="medUnitIntakeHigh" select="n1:doseQuantity/n1:high/@value"/>-->
            <!--<xsl:with-param name="medUnitIntakeUnitLow" select="n1:doseQuantity/n1:low/@unit"/>-->
            <!--<xsl:with-param name="medUnitIntakeUnitHigh" select="n1:doseQuantity/n1:high/@unit"/>-->
        <!--</xsl:call-template>-->
    <!--</xsl:template>-->

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
        <xsl:variable name="code"
                      select="n1:entryRelationship[@typeCode='SUBJ'][@inversionInd='true']/n1:observation[@classCode='OBS']"/>
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
                <xsl:call-template name="show-displayLabels">
                    <xsl:with-param name="code" select="'42'"/>
                </xsl:call-template>
                <!-- <xsl:value-of select="'No'"/>  -->
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="show-displayLabels">
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
                <xsl:if test="$code/@codeSystem and $code/@codeSystem='2.16.840.1.113883.6.73'">
                    ATC
                </xsl:if>
            </td>
            <td>
                <xsl:if test="$code/@code and $code/@codeSystem='2.16.840.1.113883.6.73'">
                    <xsl:choose>
                        <xsl:when test="not($code/@nullFlavor)">
                            <xsl:value-of select="$code/@code"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:call-template name="show-nullFlavor">
                                <xsl:with-param name="code" select="$code/@nullFlavor"/>
                            </xsl:call-template>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>
            </td>
            <td>
                <xsl:choose>
                    <xsl:when test="not($code/@nullFlavor)">
                        <xsl:value-of select="$code/@displayName"/>
                    </xsl:when>
                    <xsl:when
                            test="$code/n1:originalText/n1:reference/@value">
                        <xsl:call-template name="show-uncodedElement">
                            <xsl:with-param name="data" select="$code/n1:originalText/n1:reference/@value"/>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:choose>
                            <xsl:when test="not($name)">
                                <xsl:call-template name="show-nullFlavor">
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
                <xsl:for-each select="$strength">
                    <xsl:call-template name="strength"/>
                </xsl:for-each>
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
                <colgroup>
                    <col span="1" style="width: 20%;"/>
                    <col span="1" style="width: 15%;"/>
                    <col span="1" style="width: 10%;"/>
                    <col span="1" style="width: 15%;"/>
                    <col span="1" style="width: 20%;"/>
                    <col span="1" style="width: 20%;"/>
                </colgroup>
                <tbody>
                    <tr>
                        <th colspan="2">
                            <!-- Active Ingredient -->
                            <xsl:call-template name="show-displayLabels">
                                <xsl:with-param name="code" select="'1'"/>
                            </xsl:call-template>
                        </th>
                        <th colspan="{2-count($manufacturedMaterialStrength)}">
                            <!-- Number of Packages -->
                            <xsl:call-template name="show-displayLabels">
                                <xsl:with-param name="code" select="'43'"/>
                            </xsl:call-template>
                        </th>
                        <xsl:if test="$manufacturedMaterialStrength">
                            <th>
                                <!-- Strength -->
                                <xsl:call-template name="show-displayLabels">
                                    <xsl:with-param name="code" select="'70'"/>
                                </xsl:call-template>
                            </th>
                        </xsl:if>
                        <th>
                            <!-- Package Size -->
                            <xsl:call-template name="show-displayLabels">
                                <xsl:with-param name="code" select="'50'"/>
                            </xsl:call-template>
                        </th>
                        <th>
                            <!--  Dose Form -->
                            <xsl:call-template name="show-displayLabels">
                                <xsl:with-param name="code" select="'25'"/>
                            </xsl:call-template>
                        </th>
                        <th colspan="2">
                            <!-- Brand Name -->
                            <xsl:call-template name="show-displayLabels">
                                <xsl:with-param name="code" select="'9'"/>
                            </xsl:call-template>
                        </th>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <xsl:call-template name="show-active-ingredients"/>
                        </td>
                        <td colspan="{2-count($manufacturedMaterialStrength)}">
                            <xsl:call-template name="number-of-packages">
                                <xsl:with-param name="supply" select="n1:entryRelationship[@typeCode='COMP']"/>
                            </xsl:call-template>
                        </td>
                        <xsl:if test="$manufacturedMaterialStrength">
                            <td>
                                <xsl:call-template name="show-manufacturedMaterialStrength">
                                    <xsl:with-param name="parameter"
                                                    select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:desc"/>
                                </xsl:call-template>
                            </td>
                        </xsl:if>
                        <td>
                            <xsl:variable name="hasPackage">
                                <xsl:call-template name="check-Parameter">
                                    <xsl:with-param name="parameter"
                                                    select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:asContent/epsos:containerPackagedMedicine/epsos:capacityQuantity/@nullFlavor"/>
                                </xsl:call-template>
                            </xsl:variable>
                            <xsl:choose>
                                <xsl:when test="$hasPackage = 'false'">
                                    <xsl:call-template name="show-nullFlavor">
                                        <xsl:with-param name="code"
                                                        select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:asContent/epsos:containerPackagedMedicine/epsos:capacityQuantity/@nullFlavor"/>
                                    </xsl:call-template>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:call-template name="package"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </td>
                        <td>
                            <xsl:call-template name="show-formCode">
                                <xsl:with-param name="parameter"
                                                select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:formCode"/>
                            </xsl:call-template>
                        </td>
                        <td colspan="2">
                            <xsl:value-of select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/n1:name"/>
                        </td>
                    </tr>
                    <tr>
                        <th colspan="2" rowspan="2">
                            <!-- Route of Administration -->
                            <xsl:call-template name="show-displayLabels">
                                <xsl:with-param name="code" select="'67'"/>
                            </xsl:call-template>
                        </th>
                        <th colspan="2" rowspan="2">
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
                        <th rowspan="2">
                            <!-- Frequency of Intakes -->
                            <xsl:call-template name="show-displayLabels">
                                <xsl:with-param name="code" select="'32'"/>
                            </xsl:call-template>
                        </th>
                        <th colspan="2">
                            <!-- Units per intake -->
                            <xsl:call-template name="show-displayLabels">
                                <xsl:with-param name="code" select="'78'"/>
                            </xsl:call-template>
                        </th>
                    </tr>
                    <tr>
                        <th><xsl:value-of select="$min"/></th>
                        <th><xsl:value-of select="$max"/></th>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <!-- RoA -->
                            <xsl:call-template name="show-formCode">
                                <xsl:with-param name="parameter" select="n1:routeCode"/>
                            </xsl:call-template>
                        </td>
                        <td colspan="2">
                            <xsl:call-template name="show-time">
                                <xsl:with-param name="datetime"
                                                select="n1:effectiveTime[1][@xsi:type='IVL_TS' or substring-after(@xsi:type, ':')='IVL_TS']/n1:low"/>
                            </xsl:call-template>
                        </td>
                        <td>
                            <xsl:call-template name="show-time">
                                <xsl:with-param name="datetime"
                                                select="n1:effectiveTime[1][@xsi:type='IVL_TS' or substring-after(@xsi:type, ':')='IVL_TS']/n1:high"/>
                            </xsl:call-template>
                        </td>
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
                                    <xsl:call-template name="show-nullFlavor">
                                        <xsl:with-param name="code" select="n1:effectiveTime[2]/@nullFlavor"/>
                                    </xsl:call-template>

                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:choose>
                                        <xsl:when test="$hasFrequency = 'false'">
                                            <xsl:call-template name="show-nullFlavor">
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
                        <!--<td>-->
                            <!--<xsl:variable name="hasIntakes">-->
                                <!--<xsl:call-template name="check-Parameter">-->
                                    <!--<xsl:with-param name="parameter" select="n1:doseQuantity/@nullFlavor"/>-->
                                <!--</xsl:call-template>-->
                            <!--</xsl:variable>-->
                            <!--<xsl:choose>-->
                                <!--<xsl:when test="$hasIntakes = 'false'">-->
                                    <!--<xsl:call-template name="show-nullFlavor">-->
                                        <!--<xsl:with-param name="code" select="n1:doseQuantity/@nullFlavor"/>-->
                                    <!--</xsl:call-template>-->
                                <!--</xsl:when>-->
                                <!--<xsl:otherwise>-->
                                    <!--<xsl:call-template name="intakes"/>-->
                                <!--</xsl:otherwise>-->
                            <!--</xsl:choose>-->
                        <!--</td>-->
                        <td>
                            <xsl:call-template name="show-numberUnitIntakeLow">
                                <xsl:with-param name="medUnitIntake" select="n1:doseQuantity"/>
                            </xsl:call-template>
                        </td>
                        <td>
                            <xsl:call-template name="show-numberUnitIntakeHigh">
                                <xsl:with-param name="medUnitIntake" select="n1:doseQuantity"/>
                            </xsl:call-template>
                        </td>
                    </tr>
                    <tr>
                        <th colspan="2">
                            <xsl:call-template name="show-displayLabels">
                                <xsl:with-param name="code" select="'38'"/>
                            </xsl:call-template>
                        </th>
                        <td colspan="6">
                            <xsl:call-template name="substitution-code"/>
                        </td>
                    </tr>
                    <tr>
                        <th colspan="2">
                            <!--   Instructions to patient:-->
                            <span class="td_label">
                                <xsl:call-template name="show-displayLabels">
                                    <xsl:with-param name="code" select="'37'"/>
                                </xsl:call-template>
                            </span>
                        </th>
                        <td colspan="6">
                            <xsl:call-template name="show-text">
                                <xsl:with-param name="txt"
                                                select="n1:entryRelationship[@typeCode='SUBJ']/n1:act[n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.3'] ]/n1:text"/>
                            </xsl:call-template>
                        </td>
                    </tr>
                    <tr>
                        <th colspan="2">
                            <!--  Advise to the dispenser:-->
                            <span class="td_label">
                                <xsl:call-template name="show-displayLabels">
                                    <xsl:with-param name="code" select="'4'"/>
                                </xsl:call-template>
                            </span>
                        </th>
                        <td colspan="6">
                            <xsl:call-template name="show-text">
                                <xsl:with-param name="txt"
                                                select="n1:entryRelationship[@typeCode='SUBJ']/n1:act[n1:templateId[@root='1.3.6.1.4.1.19376.1.5.3.1.4.3.1'] ]/n1:text"/>
                            </xsl:call-template>
                        </td>
                    </tr>
                    <tr>
                        <th colspan="2">
                            <span class="td_label"><!--  Substitute:-->
                                <xsl:call-template name="show-displayLabels">
                                    <xsl:with-param name="code" select="'71'"/>
                                </xsl:call-template>
                                *
                            </span>
                        </th>
                        <td colspan="6">
                            <xsl:variable name="substitutionValue">
                                <xsl:call-template name="pure-substitution-code"/>
                            </xsl:variable>
                            <input type="checkbox" style="display:inline">
                                <xsl:choose>
                                    <xsl:when test="$substitutionValue !='Yes'">
                                        <xsl:attribute name="disabled"/>
                                    </xsl:when>
                                </xsl:choose>
                            </input>
                            <!--  Substitution help text:-->
                            <xsl:call-template name="show-displayLabels">
                                <xsl:with-param name="code" select="'202'"/>
                            </xsl:call-template>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="4" rowspan="3"/>
                        <th>
                            <!--  Dispensed Product:-->
                            <span class="td_label">
                                <xsl:call-template name="show-displayLabels">
                                    <xsl:with-param name="code" select="'24'"/>
                                </xsl:call-template>
                            </span>
                        </th>
                        <td colspan="3">
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
                                <xsl:call-template name="show-displayLabels">
                                    <xsl:with-param name="code" select="'23'"/>
                                </xsl:call-template>
                            </span>
                        </th>
                        <td colspan="3">
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
                                <xsl:value-of select="$dispensedNumberOfPackages"/>
                            </span>
                        </th>
                        <td colspan="3">
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
                            </input>
                        </td>
                    </tr>
                    <tr>
                        <td style="background-color:#ffffcc" colspan="{6+count($manufacturedMaterialStrength)}">
                            <!-- HOTFIX - Has to be replaced with a value from the epSOSDisplayLabel value set -->
                            *  If substitution of brand name is marked as not allowed, pharmacists may still consider dispensing the national equivalent even though the brand name might be slightly different.<br/>
                            &#160;&#160;This is a known situation: the same pharmaceutical company is marketing the same medicinal product in different countries with slightly different names due to marketing reasons.<br/>
                            &#160;&#160;If the pharmacist is certain that this is the case, the systems allows the input of the new brand name.<br/>
                        </td>
                        <td style="text-align:center;vertical-align:middle;">
                            <!--  Dispense -->
                            <xsl:choose>
                                <xsl:when test="$allowDispense='true'">
                                    <input type="submit" class="button">
                                        <xsl:attribute name="value">
                                            <xsl:call-template name="show-displayLabels">
                                                <xsl:with-param name="code" select="'22'"/>
                                            </xsl:call-template>
                                        </xsl:attribute>
                                    </input>
                                </xsl:when>
                            </xsl:choose>
                            <input type="hidden" name="prescriptionID" id="prescriptionID">
                                <xsl:attribute name="value">
                                    <xsl:value-of select="$prescriptionID"/>
                                </xsl:attribute>
                            </input>
                            <input type="hidden">
                                <xsl:call-template name="inputform">
                                    <xsl:with-param name="txt" select="'dispensationid_'"/>
                                    <xsl:with-param name="val" select="n1:id/@extension"/>
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
                                    <xsl:call-template name="strength"/>
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

    <xsl:template name="show-strengths">
        <xsl:for-each select="$strength">
            <xsl:call-template name="strength"/>
        </xsl:for-each>
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
                    <th><xsl:value-of select="$activeIngredientCodeSystem"/></th>
                    <th><xsl:value-of select="$activeIngredientCode"/></th>
                    <th><xsl:value-of select="$activeIngredientName"/></th>
                    <th>
                        <!-- Strength -->
                        <xsl:call-template name="show-displayLabels">
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

    <xsl:template name="epPrescriptionItem">
        <div class="wrap-collabsible">
            <input id="collapsible-medicine-list" class="toggle" type="checkbox" checked="true"/>
            <label for="collapsible-medicine-list" class="lbl-toggle-main">
                Medicine list
            </label>
            <div class="collapsible-content-main">
                <div class="content-inner-main">
                    <xsl:for-each select="$entryNode">
                        <div class="wrap-collabsible">
                            <input id="collapsible-prescriptionItem-header" class="toggle" type="checkbox" checked="true"/>
                            <label for="collapsible-prescriptionItem-header" class="lbl-toggle-title">
                                <countNo><xsl:value-of select="position()"/>.
                                </countNo>
                                <xsl:call-template name="pharmaceuticalSubstanceHeader"/>
                            </label>
                            <div class="collapsible-content-title">
                                <div class="content-inner-title">
                                    <table>
                                        <tr>
                                            <td>
                                                <xsl:call-template name="epPrescriptionItemDetails"/>
                                            </td>
                                        </tr>
                                    </table>

                                    <script>
                                        <xsl:attribute name="type">
                                            <xsl:text>text/javascript</xsl:text>
                                        </xsl:attribute>
                                        <xsl:text>i++;</xsl:text>
                                    </script>
                                </div>
                            </div>
                        </div>
                    </xsl:for-each>
                </div>
            </div>
        </div>
    </xsl:template>

    <xsl:template name="pharmaceuticalSubstanceHeader">
        &#160;
        <xsl:value-of select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/n1:name"/>
        <xsl:choose>
            <xsl:when test="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:asSpecializedKind">
                <xsl:if test="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:asSpecializedKind/epsos:generalizedMedicineClass/epsos:code/@codeSystem='2.16.840.1.113883.6.73'">
                    (<span class="codeSystem">ATC</span>
                    &#160;
                    <span class="codeSystem">
                        <xsl:value-of select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:asSpecializedKind/epsos:generalizedMedicineClass/epsos:code/@code"/>
                    </span>
                    &#160;
                    <xsl:value-of select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial/epsos:asSpecializedKind/epsos:generalizedMedicineClass/epsos:name"/>)
                </xsl:if>
            </xsl:when>
        </xsl:choose>
        &#160;
    </xsl:template>
</xsl:stylesheet>