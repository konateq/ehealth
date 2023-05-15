# Change Log

All notable changes to the <a href="https://ec.europa.eu/cefdigital/wiki/x/8CEZAg" rel="nofollow">CEF EHDSI project</a> will be documented in this file.<br/>
This project adheres to <a href="http://semver.org/" rel="nofollow">Semantic Versioning</a>.

## [5.2.9] - 09-05-2023
The OpenNCP 5.2.8 is the Wave 5 Operation Ready HotFix release.
This version is planned to be used as a bug fix version during the eHDSI Wave 5 Routine Operation as a recommended version.
Detailed release note is available here: https://webgate.ec.europa.eu/fpfis/wikis/x/gQGQWg

Main changes in this release are:<br/>
<ul>
<li>OpenNCP Gateway improvements: ATNA filtering and eADC KPIs handling</li>
<li>OpenNCP JAR dependencies upgrade based on Owasp vulnerabilities</li>
<li>SAML HCP authentication method updated list</li>
<li>Databases drivers versions upgrade (MySQL, MariDB, PostgreSQL)</li>
</ul>

### Bug
    [EHEALTH-9536] - OpenNCP Gateway ATNA Viewer - Filtering on Time doesn't work as expected.
    [EHEALTH-9549] - Security Manager accepts an AuthN method which is not allowed in eHDSI.
    [EHEALTH-9867] - Exported KPIs have their timestamp modified to be no later than 12:59:59.

### Task
    [EHEALTH-9912] - Releasing OpenNCP 5.2.9.

### Improvement
    [EHEALTH-8503] - Cosmetic issues - OpenNCP-Gateway ATNA-Viewer main page lacks functionality.
    [EHEALTH-9641] - OpenNCP-Gateway: "SMP File Update" UI needs improvement.
    [EHEALTH-9860] - Country codes should be uppercased before persistence in eADC.
    [EHEALTH-9864] - Exported KPI data should be sorted by date/time.

## [5.2.8] - 05-01-2023
The OpenNCP 5.2.8 is the Wave 5 Operation Ready HotFix release.
This version is planned to be used as a bug fix version during the eHDSI Wave 5 Routine Operation as a recommended version.
Detailed release note is available here: https://webgate.ec.europa.eu/fpfis/wikis/x/aAIvUg

Main changes in this release are:<br/>
<ul>
<li>OpenNCP Gateway (SMP editor, eADC and ATNA modules) bug fixing.</li>
<li>XCPD request and response alignment with IHE Technical Framework (removing empty XML tags).</li>
<li>CDA DisplayTool: substitution checkbox support.</li>
<li>OrCD test evidences bug fix.</li>
</ul>

### Bug
    [EHEALTH-5702] - Patient ID is not the same in XCA List Response and CDA record target.
    [EHEALTH-8928] - OpenATNA log shows IO error.
    [EHEALTH-9190] - WebSecurityConfigurerAdapter is deprecated in Spring Boot 2.7.0.
    [EHEALTH-9245] - OpenNCP Gateway- Export not possible when end date or start date is empty in eADC transaction table.
    [EHEALTH-9246] - OpenNCP Gateway - In SMP Editor, it is not possible to sign a document.
    [EHEALTH-9335] - OpenNCP-Gateway - ATNA Viewer: missing EventIDs in dropdown list.
    [EHEALTH-9350] - IHE ITI Responses with empty elements (optional) should not be present into the XML.
    [EHEALTH-9490] - Date format not ok in exported KPI file from OpenNCP gateway ATNA eADC.
    [EHEALTH-9497] - eP - On CDA- Substitue checkbox not working.
    [EHEALTH-9498] - OrCD - Pivot L1 getting generated for NCP B & NCP-A very delayed.

### Task
    [EHEALTH-9226] - eHDSI Monitoring Framework (KPIs) - implementation guides.
    [EHEALTH-8500] - Investigation - Identify all all the CDA sequence element that doesn't render CDA Display Tool (except for : guardian, participant, ingredients).

### Improvement
    [EHEALTH-9292] - OpenNCP-Gateway - eADC Viewer export: years in the dropdown list are displayed in random order.

## [5.2.7] - 07-11-2022
The OpenNCP 5.2.7 is the Wave 5 Operation Ready HotFix release.
This version is planned to be used as a bug fix version during the eHDSI Wave 5 Routine Operation as a recommended version.
Detailed release note is available here: https://webgate.ec.europa.eu/fpfis/wikis/x/QQAyTg

Main changes in this release are:<br/>
<ul>
<li>Apache Commons Text vulnerability resolved.</li>
<li>OpenNCP Gateway (ATNA, eADC and SMP) improvements and bug fix on UI and backend.</li>
<li>XPath Coded Elements list configuration file update (to be updated also into the custom configuration folder).</li>
<li>OpenNCP Gazelle Validator: module truststore set to OpenNCP one instead of the CACert from the system.</li>
<li>Audit message NullPointerException resolved during ISM fetch operation.</li>
</ul>

### Bug
    [EHEALTH-8129] - TM code_element_list.xml: wrong XPath for Substitution flag in ePrescription.
    [EHEALTH-8551] - OpenNCP Gateway doesn't support properly Oracle database LOB type.
    [EHEALTH-8892] - Problem with OpenNCP version 5.2.5 openatna viewer
    [EHEALTH-8923] - OpenNCP-Gateway - SMP-Editor: Delete dialog box is not closed after deletion of SMP file.
    [EHEALTH-8924] - OpenNCP-Gateway - SMP-Editor: no feedback to user after SMP file upload.
    [EHEALTH-8925] - OpenNCP-Gateway - EADC-Viewer: columns header doesn't change order of presentation.
    [EHEALTH-8927] - OpenNCP-WS-Server fails to establish TLS connection to remote Gazelle validators.
    [EHEALTH-9040] - Pagination issue with ATNA viewer.
    [EHEALTH-9069] - [PS Extended header] Trailing text in extended patient header.
    [EHEALTH-9116] - OpenNCP-Gateway: cannot export EADC from 2022 onwards (hardcoded values).
    [EHEALTH-9142] - OpenATNA: issue while storing events information during ISM synchronizing.
    [EHEALTH-9165] - Pagination in eADC generate exception if startDate or endDate fields are empty.

### Task
    [EHEALTH-9234] - Releasing OpenNCP 5.2.7.
    [EHEALTH-8399] - The type of address (/addr/@use) is not properly displayed.

### Improvement
    [EHEALTH-8778] - OpenNCP Gateway Backend - Spring Security warning.
    [EHEALTH-9239] - Apache Commons Text vulnerability.    

## [5.2.6] - 13-06-2022
The OpenNCP 5.2.6 is the Wave 5 Operation Ready release.
This version is planned to be used during the eHDSI Wave 5 Routine Operations.
Detailed release note is available here: https://webgate.ec.europa.eu/fpfis/wikis/x/zwVGRw

Main changes in this release are:<br/>
<ul>
<li>ePrescription List metadata handling improvement and bug fixing.</li>
<li>CDA Display Tool: major improvement and bug fixing.</li>
<li>XPath Coded Elements list configuration file update (to be updated also into the custom configuration folder).</li>
<li>OpenNCP Gateway UI improvements - Previous OpenATNA UI fully deprecated</li>
<li>Support of the OID and HL7 II identifiers for the Author and Organization.</li>
<li>Java dependencies vulnerability fix.</li>
</ul>

### Sub-task
    [EHEALTH-8148] - Start and end date for medical devices is not using the correct datatype
    [EHEALTH-8151] - eHDSIUnit code system OID is not updated correctly in the CDA Display Tool
    [EHEALTH-8185] - History of past problems section contains wrong table header

### Technical Sub-task
    [EHEALTH-8205] - Vital Signs: translated part doesn't display units
    [EHEALTH-8207] - CDA Display Tool doesn't render all the CDA sequence element (guardian, participant, ingredients)
    [EHEALTH-8238] - OpenNCP Spring framework vulnerability - security fix implementation
    [EHEALTH-8395] - Ambiguous XPath for the allergies severity
    [EHEALTH-8396] - HP (author) professional role not displayed.
    [EHEALTH-8399] - The type of address (/addr/@use) is not properly displayed
    [EHEALTH-8447] - Releasing OpenNCP 5.2.6 (Wave 5 Operation Ready)

### Bug
    [EHEALTH-5818] - XDS AuthorInstitution not correct HL7 XON Format
    [EHEALTH-5860] - NCP-A doesn't block requests containing a TRCA with non-eHDSI PoU
    [EHEALTH-6006] - hl7:doseQuantity element is not transcoded by the transformation manager
    [EHEALTH-8037] - Document list: confidentiality allows incoherence of code and description
    [EHEALTH-8047] - OpenATNA-Viewer UI/UX improvements
    [EHEALTH-8048] - eP List - Issue with the substitution flag being null
    [EHEALTH-8082] - OpenNCP-Gateway only supports alphanumeric passwords
    [EHEALTH-8083] - OpenNCP-Gateway Reset Password allows new password not matching the criteria
    [EHEALTH-8084] - OpenNCP-Gateway Reset Password email template needs improvements
    [EHEALTH-8108] - Gateway - SMP Editor ISM extension maximum size supported
    [EHEALTH-8112] - Gateway - SMP Editor ISM update doesn't work properly
    [EHEALTH-8123] - Wrong validator used for failed Order list response
    [EHEALTH-8124] - eP List response injects Generic substitution in case it's not provided by NI-A
    [EHEALTH-8147] - CDA Display Tool bugfixes for Wave 5
    [EHEALTH-8192] - ePrescription List XDS metadata cardinality not correct produce empty Classification
    [EHEALTH-8539] - Allergies and intolerances concern XPath uses incorrect templateID in coded_element_list_ehdsi.xml

### Story
    [EHEALTH-7021] - Prefix value of "authorPerson" is not a valid XCN datatype
    [EHEALTH-7954] - OpenNCP Gateway Backend - Pagination doesn't work properly (ATNA and eADC)

### Task
    [EHEALTH-6389] - Ignore all failing, unmaintained unit tests in OpenNCP in order in order to keep a reliable test set

### Improvement
    [EHEALTH-6660] - Remove ATNA user interface from OpenATNA module
    [EHEALTH-8086] - Provide the definition of the EvenType document name
    [EHEALTH-8105] - Gateway SMP Editor - creating configuration folder if not present

## [5.2.5] - 21-02-2022
The OpenNCP 5.2.5 is the Wave 5 Formal Test Event HotFix release.
This version is planned to be used during the eHDSI Formal Test event in February 2022.
Detailed installation manual is available here: https://webgate.ec.europa.eu/fpfis/wikis/x/E4pMQ

Main changes in this release are:<br/>
<ul>
<li>ePrescription List metadata bug fix.</li>
<li>OpenNCP Client Connector ClassLoading issue.</li>
<li>OpenNCP Client Connector Consumer - 2 Way SSL enabling.</li>
<li>OpenNCP Gateway Portal and Backend improvements (ATNA and eADC).</li>
<li>Log4j vulnerability fix.</li>
</ul>

### Sub-task
    [EHEALTH-7016] - Log4j core update implementation

### Technical Sub-task
    [EHEALTH-8010] - Releasing OpenNCP 5.2.5 (Wave 5 Formal Test Event HotFix)

### Bug
    [EHEALTH-6247] - OpenNCP-Gateway ATNA-Viewer main page lacks functionality
    [EHEALTH-7851] - OpenNCP-Gateway ATNA Viewer does not work with Oracle database
    [EHEALTH-7866] - Gateway Front - eADC viewing returns black screen
    [EHEALTH-7867] - Gateway Front - Npm build doesn't work on Windows machine with Node
    [EHEALTH-7921] - ATNA-Viewer: Code Systems are not displayed
    [EHEALTH-7922] - OpenNCP-Gateway-Backend doesn't support HikariCP-5.x
    [EHEALTH-7923] - OpenNCP-Gateway-Backend embedded application.yml misses Spring config
    [EHEALTH-7924] - eP List: dispensable value is switched
    [EHEALTH-7925] - eP List: substitution allows incoherence of code and description
    [EHEALTH-7971] - OpenNCP-Gateway-Backend: Spring mail configurations missing and undocumented
    [EHEALTH-7986] - OpenNCP Client Connector Consumer doesn't support 2-ways-SSL with PPT certificates

### Story
    [EHEALTH-7859] - Investigate NoSuchMethorError in openncp-client-connector for OpenNCP release v5.2.4

### Improvement
    [EHEALTH-7179] - OpenNCP Gateway Portal Password security strength implementation

### Test
    [EHEALTH-7987] - Test- ATNA-Viewer: Code Systems are not displayed
    [EHEALTH-7988] - Test - OpenNCP-Gateway ATNA-Viewer main page lacks functionality
    [EHEALTH-7989] - Test- Remove ATNA user interface from OpenATNA module
    [EHEALTH-7992] - Test - Gateway Front - eADC viewing returns black screen 
    [EHEALTH-7996] - Test - Investigate NoSuchMethorError in openncp-client-connector for OpenNCP release v5.2.4
    [EHEALTH-7999] - Test- OpenNCP-Gateway-Backend: Spring mail configurations missing and undocumented
    [EHEALTH-8001] - Test - OpenNCP-Gateway-Backend embedded application.yml misses Spring config
    [EHEALTH-8003] - Test - eP List: dispensable value is switched
    [EHEALTH-8004] - Test - eP List: substitution allows incoherence of code and description
    [EHEALTH-8009] - Regression PS
    [EHEALTH-8011] - Regression eP/eD
    [EHEALTH-8012] - Regression OrCD

## [5.2.4] - 25-01-2021
The OpenNCP 5.2.4 is the Wave 5 Formal Test Event release.
This version is planned to be used during the eHDSI Formal Test event in February 2022.
Detailed installation manual is available here: https://webgate.ec.europa.eu/fpfis/wikis/x/ogARPw

Main changes in this release are:<br/>
<ul>
<li>Apache Axis2 upgrade to version 1.8.0</li>
<li>Change Proposal 051: ePrescription list bug fix related to XDS metadata returned (substitution, ATC, dispensable etc.).</li>
<li>OpenNCP Gateway Portal and Backend: bug fix and improvements on the user interface.</li>
</ul>

### Sub-task
    [EHNCP-2063] - National Connector B: Error handling implementation.
    [EHNCP-2064] - Test activities - Implement CP-eHealthDSI-043.
    [EHNCP-2237] - Gateway Backend implementation.
    [EHNCP-2238] - Gateway Front-end implementation.

### Bug
    [EHNCP-1946] - NCP-A doesn't block requests containing a TRCA with non-eHDSI PoU.
    [EHNCP-2056] - OpenATNA-Web paginator doesn't work.
    [EHNCP-2221] - ePrescription list doesn't provide dispensable element and not aligned with Business Requirements.
    [EHNCP-2239] - Investigate critical vulnerability in Apache Log4j library.
    [EHNCP-2249] - ePrescription list doesn't provide substitution element aligned with Business Requirements.
    [EHNCP-2261] - OpenNCP-Gateway-Backend still depends on Thymeleaf.
    [EHNCP-2263] - OpenNCP-Gateway ATNA-Viewer doesn't show all records.
    [EHNCP-2264] - OpenNCP-Gateway ATNA-Viewer doesn't display Event Outcome Indicator.
    [EHNCP-2265] - OpenNCP-Gateway ATNA-Viewer doesn't display Active Participants details.
    [EHNCP-2267] - OpenNCP-Gateway-Backend has incorrect ORM annotations for EADC.
    [EHNCP-2268] - OpenNCP-Gateway-Backend Spring-Boot JPA misconfiguration.

### Story
    [EHNCP-2230] - Releasing OpenNCP 5.2.4 (Wave 5 Formal Test Event).

### Improvement
    [EHNCP-2197] - Upgrade to Axis2 1.8.0.
    [EHNCP-2262] - OpenNCP-Gateway-Backend Display Name is not shown in Tomcat web console.

### Issue
    [EHNCP-2215] - ClientConnectorConsumerException is a RuntimeException.
    [EHNCP-2260] - EHNCP_USER.EMAIL should be nullable.

## [5.2.3] - 17-11-2021
The OpenNCP 5.2.3 is the Wave 5 Preparatory Test Event HotFix release.
This version is an emergency version planned to be used by the Member States during the eHDSI Preparatory Test event for Wave 5 from October 2021 as a replacement of version 5.2.2.
Detailed installation manual is available here: https://webgate.ec.europa.eu/fpfis/wikis/x/GALmNg

Main changes in this release are:<br/>
<ul>
<li>OpenNCP BugFix related to the TRC SAML assertion extra parameters: prescription ID and dispense PIN Code.</li>
<li>OpenNCP Gateway Portal navigation menu improvement (SMP List services).</li>
</ul>

### Bug:
    [EHNCP-2219] - TRC SAML ePrescription ID and dispensePinCode are not processed

### Improvement:
    [EHNCP-2218] - Gateway Portal - List and delete SMP resources view

## [5.2.2] - 15-11-2021
The OpenNCP 5.2.2 is the Wave 5 Preparatory Test Event HotFix release.
This version is planned to be used by the Member States during the eHDSI Preparatory Test event for Wave 5 from October 2021.
Detailed installation manual is available here: https://webgate.ec.europa.eu/fpfis/wikis/x/GALmNg

Main changes in this release are:<br/>
<ul>
<li>OpenNCP BugFix JAXB and Saxon HE libraries update (Gateway and Protocol Terminator).</li>
<li>BugFix on ePrescription List metadata (CP-0051).</li>
<li>BugFix on OrCD List metadata and Audit Messages.</li>
<li>Minor fix on OpenNCP Gateway portal.</li>
</ul>

### Bug:
    [EHNCP-2199] - Deleting SMP files locked on European Commission jurisdiction
    [EHNCP-2202] - ePrescription List throws index out of bounds exception when some metadatas are not available
    [EHNCP-2203] - XCAServiceImpl - Dispensable event status not correct
    [EHNCP-2206] - Missing XDS classification scheme into the eP List (CP-0051)
    [EHNCP-2207] - OpenNCP Gateway Upload - Http code 200
    [EHNCP-2210] - JAXB libraries upgrade
    [EHNCP-2211] - Saxon-HE library is missing on Gateway module
    [EHNCP-2212] - OrCD list query return NullPointerException as Description is not available
    [EHNCP-2214] - OrCD Audit message - transaction code for Query and Retrieve operations is not mapped

### Task:
    [EHNCP-2205] - Update Home Community ID for Member State

### Issue:
    [EHNCP-2201] - OrCD XDS metadata issue
    [EHNCP-2204] - Hardcoded configurations of Gmail SMTP and email accounts

### Support:
    [EHNCP-2208] - Problem with initialization of Util Constants and configuration manager during XCPD transaction
    [EHNCP-2209] - Problem with OpenNCP Gateway and Saxon Provider

## [5.2.1] - 27-09-2021
The OpenNCP 5.2.1 is the Wave 5 Preparatory Test Event release.
This version is planned to be used by the Member States during the eHDSI Preparatory Test event for Wave 5 from October 2021.
Detailed installation manual is available here: https://webgate.ec.europa.eu/fpfis/wikis/x/GALmNg

Main changes in this release are:<br/>
<ul>
<li>OpenNCP Gateway refactoring (ATNA, eADC and SMP management through the SPA implemented with Vue.js), previous version removed</li>
<li>Improvement of Change Proposals: 39, 47 and 51
<ul>- CP-039 - Patient Identification through Next of Kin</ul>
<ul>- CP-047 - OrCD service implemented.</ul>
<ul>- CP-051 - ePrescriptions list improvement (structured format).</ul>
</li>
<li>CDA Display Tool improvements linked to Wave 5 requirements.</li>
</ul>

### Sub-task:
    [EHNCP-2141] - Java 11 source code minor refactoring
    [EHNCP-2174] - [CDA Display Tool] In the case of multiple clinical manifestations, the values are glued together
    [EHNCP-2177] - [CDA Display Tool] In the case of multiple first names, make sure all are displayed correctly
    [EHNCP-2178] - [CDA Display Tool] For ePrescription, correct the display logic of the active ingredient on product level

### Bug:
    [EHNCP-1905] - SMP Query ISM file producing not correct Audit Messages
    [EHNCP-2160] - OpenNCP Gateway doesn't start properly
    [EHNCP-2161] - OpenNCP Gateway login not implemented
    [EHNCP-2180] - ATNA Gateway admin console not able to provide audit message details
    [EHNCP-2182] - Reload OpenNCP Gateway Portal creates 404 error
    [EHNCP-2183] - OpenNCP Gateway Backend hotdeploy doesn't work properly
    [EHNCP-2185] - Gateway - SMP Editor generates inconsistency
    [EHNCP-2187] - Generate SMP File for ISM
    [EHNCP-2189] - NoK Assertion audit message is not correct
    [EHNCP-2191] - Validation fails for the OrCD service because of an empty description
    [EHNCP-2192] - Fix subsitution field for ePrescription List
    [EHNCP-2196] - XCPD Audit message doesn't return the Patient ID but the Identity Traits

### New Feature:
    [EHNCP-2184] - Implement change password feature for the Gateway console

### Task:
    [EHNCP-2194] - Integration of the new OpenNCP Gateway implementation

### Improvement:
    [EHNCP-1760] - Export KPIs functionality through OpenNCP Gateway
    [EHNCP-2144] - Display eADC data and Export to Excel
    [EHNCP-2179] - Remove the possibility of having 3 eADC DB schemas
    [EHNCP-2181] - OpenNCP Gateway - Front End improvements
    [EHNCP-2195] - Security Token Service improvements (TRC and NoK assertions)

### Support:
    [EHNCP-2188] - Login to the current gateway

### Issue:
    [EHNCP-2186] - OpenNCP Gateway Front - "Click me" Button

## [5.2.0] - 14-07-2021
The OpenNCP 5.2.0 is the Wave 5 Initial release including the implementation of the Wave 5 Change Proposals.
This version is planned to be used by the Member States as preparation for the eHDSI Preparatory Test event for Wave 5 from October 2021. 
Based on the Member States feedback and further improvement items as decided by the OpenNCP Steering Work Group, 
a new release is expected mid-September, in due time before the Preparatory PPT.

Main changes in this release are:<br/>
<ul>
<li>Update to Java 11 and OpenSAML 4</li>
<li>Implementation of Change Proposals: 39, 43, 46, 47 and 51
<ul>- CP-039 - Patient Identification through Next of Kin</ul>
<ul>- CP-043 - Error handling.</ul>
<ul>- CP-046 - Two-Factor Authentication and NCP-A role based access control removed.</ul>
<ul>- CP-047 - OrCD service implemented.</ul>
<ul>- CP-051 - ePrescriptions list improvement (structured format).</ul>
</li>
<li>CDA Display Tool compliant with Wave 5 requirements.</li>
</ul>

### Story:
    [EHNCP-1981] - Implement CP-eHealthDSI-039: Next of kin.
    [EHNCP-1988] - Implement CP-eHealthDSI-047: Enable the exchange of original (unstructured) clinical documents.
    [EHNCP-1989] - Implement CP-eHealthDSI-043: Error handling in NCP-B.
    [EHNCP-2065] - Implement CP-eHealthDSI-046: Two-Factor Authentication and NCP-A role based access control removed.
    [EHNCP-2070] - Implement CP-eHealthDSI-051: Improve ePrescription list.

### Task:
    [EHNCP-1990] - Analyze Technical impact on OpenNCP Reference Implementation.
    [EHNCP-2053] - Implement Original Clinical Document service.
    [EHNCP-2068] - OrCD exchange - Test activities.
    [EHNCP-2092] - [CDA Display Tool] Refactor CDA Display Tool implementation.
    [EHNCP-2116] - Create Mock CDA documents for OrCD Service.
    [EHNCP-2124] - [CDA Display Tool] Implementation for Wave 5.
    [EHNCP-2130] - [CP-046] Implementation activities - Two-factor authentication mechanism.
    [EHNCP-2136] - Implement Original Clinical Document service in openncp-client-connector.
    [EHNCP-2137] - Implement Original Clinical Document service in openncp-ws-server.
    [EHNCP-2140] - Releasing OpenNCP 5.2.0 (Wave 5 Initial Release).
    [EHNCP-2146] - [CDA Display Tool] Only SNOMED CT concepts are displayed from the eHDSIVaccine value set.
    [EHNCP-2147] - [CDA Display Tool] collapsible panels share the same name.
    [EHNCP-2148] - [CDA Display Tool] For different vital signs observations, the same date is displayed in the html.
    [EHNCP-2149] - [CDA Display Tool] Organisation Id not correctly displayed in the eP Prescriber header.
    [EHNCP-2150] - Update coded_element_list_ehdsi.xml with the updated OIDs.

### Improvement
    [EHNCP-2076] - Migration Java 8 to 11 (OpenJDK).
    [EHNCP-2081] - Upgrade to OpenSAML 4.

## [5.1.6] - 03-06-2021
The OpenNCP 5.1.6 is the Wave 4 Formal Second HotFix release including the Formal Test Event improvements, a bug fix of
the Medication Dispense Discard use case (eHDSI Business Requirements and eHDSI SAML Profile update), and some minor
improvements and bug fix based on the stakeholders feedback on version 5.1.5.
This version is planned to be used as the official eHDSI Wave 4 GoLive release from 01 July 2021 strongly recommended as a replacement of version 5.1.5.

Main changes in this release are:<br/>
<ul>
<li>OpenNCP Reference Implementation build - Maven HTTP repositories discarded.</li>
<li>XDR messages - date format compliant with IHE DTM definition.</li>
<li>Minor updates of JAR dependencies marked as vulnerable.</li>
</ul>

### Sub-Task
- [EHNCP-1966] - OpenNCP Java Dependencies Security analysis.
- [EHNCP-2132] - Releasing OpenNCP 5.1.6 (Wave 4 Operation Ready 2nd HotFix).

### Bug
- [EHNCP-2129] - XDR request invalid due to time format.
- [EHNCP-2128] - DocumentFactory uses wrong implementation class in PS and MRO.
- [EHNCP-2135] - ClientConnectorService WSDL doesn't enforce the use of CountryCode.

### Improvement
- [EHNCP-2133] - Remove trivial Java dependencies and implementations deprecated.
- [EHNCP-2134] - Support Maven build with version 3.8.1 (none http repository not supported).

## [5.1.5] - 17-05-2021
The OpenNCP 5.1.5 is the Wave 4 Formal HotFix release including the Formal Test Event improvements, and a clarification
of the Requirements for HCP identification during Medication Dispense Discard use case (eHDSI Business Requirements and eHDSI SAML Profile update).
This version is planned to be used as the official eHDSI Wave 4 GoLive release from 01 July 2021 as a replacement of version 5.1.4.

Main changes in this release are:<br/>
<ul>
<li>Practitioner Organization ID SAML token is now mandatory only for the Medication Discard workflow.</li>
<li>OpenNCP Gateway -  customization of the certificate used during the SML/SMP authentication.</li>
</ul>

### Change Request
- [EHNCP-2120] - eHDSI SAML OrganizationID mandatory for the medication discard operation (linked with [EHNCP-2089] - Organisation-id in SAML assertion vs. disp CDA value representedOrganization).

### Improvement
- [EHNCP-2119] - OpenNCP Gateway - Certificates independent configuration.

## [5.1.4] - 23-04-2021
The OpenNCP 5.1.4 release is the CEF eHDSI Wave 4 Operation Ready version based on the Formal Test event feedback.
This version is planned to be used as the official eHDSI Wave 4 GoLive release from 01 Jul 2021 .
<br/>Practitioner SAML token definition has been updated, and the field urn:oasis:names:tc:xspa:1.0:subject:organization-id is now mandatory.
<br/>Patient identifier root id is not overwritten anymore by the eHNCP Home Community ID during the XCA List operation.
<br/>XDS metadata improvement (data extracted from CDA instead of static values).
<br/>OpenNCP Gateway fix related to multiple database support.
<br/>Minor bug fixing on the Discard Dispense Medication operation.
<br/>Minor bug fixing into the CDA Display Tool (missing translation).

### Bug
- [EHNCP-1851] - Patient ID is not the same in XCA List Response and CDA record target.
- [EHNCP-2077] - [CDA Display Tool] Hardcoded label not retrieved from EpsosRepository value set XML.
- [EHNCP-2079] - XML audit evidences not generated on the correct folder after CDA transformation.
- [EHNCP-2080] - OpenNCP Gateway is forcing the use of MySQL hibernate.dialect.
- [EHNCP-2085] - XDS Metadata shall be aligned with CDA content exchanged.
- [EHNCP-2086] - XDR Discard operation doesn't return transaction status.
- [EHNCP-2087] - SAML Organization and Organization-ID are not processed properly during the Discard operation.
- [EHNCP-2088] - XDS metadata "languageCode" hardcoded in XDR transaction.
- [EHNCP-2107] - Common Components - Dynamic Discovery missing dependency at runtime.

### Improvement
- [EHNCP-217] - Circular dependency between unused XHTML pages.

### Decision
- [EHNCP-2089] - Organisation-id in SAML assertion vs. disp CDA value representedOrganization.

### Issue
- [EHNCP-2090] - Clinical logs have multiple occurrences of the same message.

### Test
- [EHNCP-2094] - Test Cases_01_OpenNCPeH Actor verifies XDS Metadata is aligned with CDA content exchanged.
- [EHNCP-2095] - Test Cases_02_OpenNCPeH Actor verifies the XDR Discard operation return transaction status
- [EHNCP-2096] - Test Cases_03_OpenNCPeH Actor XDS metadata "languageCode" hardcoded in XDR transaction.

## [5.1.3] - 08-02-2021
The OpenNCP 5.1.3 release is the CEF eHDSI Wave 4 Formal Test Event Security HotFix version.
<br/>Vulnerabilities contained into the project dependencies have been removed
<br/>OpenNCP Configuration Manager transaction handling improvement.
<br/>OpenNCP NCP-A mock has been improved according Wave 4 specifications.

### Sub-task
- [EHNCP-1782] - Oracle Database 12c support (bugFix).
- [EHNCP-1966] - OpenNCP Java Dependencies Security analysis.
- [EHNCP-2058] - Releasing OpenNCP 5.1.3 (Wave 4 Formal Test Event Security HotFix).

### Bug
- [EHNCP-2054] - Adapt CDA mock documents and logic to generate valid CDA L1 documents.

### Improvement
- [EHNCP-2057] - Configuration Manager - Save or Update operation using transaction rollback.

## [5.1.2] - 11-01-2021
The OpenNCP 5.1.2 release is the CEF eHDSI Wave 4 Formal Test Event HotFix version.
<br/>eHDSI Testing Portals inherited from the epSOS Pilot have been removed from the Reference Implementation.
<br/>CDA DisplayTool improvements and bug fix reported during the Wave 4 Preparatory Test Event.
<br/>Minor BugFix and improvements into the Reference Implementation.

### Sub-task
- [EHNCP-1969] - Alignment of the Gazelle validators to Wave 4.
- [EHNCP-2050] - Releasing OpenNCP 5.1.2 (Wave 4 Formal Test Event).

### Bug
- [EHNCP-2000] - ATNA FailedLogsHandlerImpl doesn't work properly for Sweden without Xerces.jar.
- [EHNCP-2031] - EADC Database Creation : Missing Column.
- [EHNCP-2048] - [CDA Display Tool] Units per intake duplicated in Prescription Details view.

### Issue
- [EHNCP-1983] - OpenATNA-Web Viewer filter has bug with date ranges.
- [EHNCP-2003] - Incoherent and confusing logging statement.
- [EHNCP-2044] - ISM XSD missing required attribute in birthDate format.
- [EHNCP-2045] - XCA WSDL exposes SOAP 1.1 binding.

### Improvement
- [EHNCP-1993] - FailedLogsHandler process is not working properly when an unexpected file format backuped is loaded.
- [EHNCP-2002] - CountryConfigManager can't find patient search ID label.
- [EHNCP-2049] - Improve the way the duration of treatment is represented.

### Decision to remove eHDSI Testing Portals from Pilot phase
- [EHNCP-2040] - Remove eHDSI Portals from the Reference Implementation core.
- [EHNCP-1101] - Migration from Liferay 6.2-GA6 to 7.0.2-GA3 Community Edition.
- [EHNCP-820] - Update Java Wicket version.
- [EHNCP-212] - Set the AllowScriptAccess.
- [EHNCP-214] - Verify that parameter values are in their expected ranges and types.
- [EHNCP-216] - Circular dependency between XHTML page and Java method.
- [EHNCP-178] - Block body parameters.

### Support
- [EHNCP-1364] - Gazelle CDA validator finds eD errors in data coming from eP.
- [EHNCP-1904] - Wrong PatientID Root in XCA OrderService Response for EP.
- [EHNCP-1992] - TSAM - Memory leak.
- [EHNCP-2016] - NC PatientSearchImpl support only id attributes from ISM, so demographics can't be processed by NC.
- [EHNCP-2052] - Portugal cannot retrieve Estonian HomeCommunity ID from configuration.

## [5.1.1] - 10-10-2020
The OpenNCP 5.1.1 release is the CEF eHDSI Wave 4 Preparatory Test Event HotFix version.
<br/>This version is fully supporting all the Patient Identification Traits.
<br/>OpenNCP Gateway has been fixed (support Media type, Hungary country code and Patient Address)
<br/>OpenNCP Portal improvement (Medication Discard feature and ISM Search Mask).
<br/>Detailed release notes: https://ec.europa.eu/cefdigital/wiki/x/H4NtEg

### Sub-task
- [EHNCP-1623] - Schema validation error for eD message when explicit unit is used for supply unit quantity in eP message.
- [EHNCP-2034] - Portals should support all the available identity traits.
- [EHNCP-2035] - Releasing OpenNCP 5.1.1 (Wave 4 Hotfix version).
- [EHNCP-2036] - TSAM Sync should support PostgreSQL database.

### Bug
- [EHNCP-1997] - Search masks with Media Content cannot be uploaded to SMP server.
- [EHNCP-2001] - In ehdsi-ism-2020.xsd the countryCode enumeration list does not contain a "HU" country code.

### Improvement
- [EHNCP-1998] - OpenNCP Portal Medications Dispensed List.

### Initiative
- [EHNCP-1996] - OpenNCP Portal - Patient confirmation doesn't work without Document Search criteria.

### Issue
- [EHNCP-1903] - Problem with Service Provider Element in ATNA Audits.
- [EHNCP-2015] - Incorrect parsing of Search Mask attribute "contextualDescription".

## [5.1.0] - 05-10-2020
The OpenNCP 5.1.0 release is the CEF eHDSI Wave 4 Preparatory Test Event version.
<br/>This version is fully supporting Change Proposals 33, 35, 36 and 42.
<br/>Dispensation workflow extension - discard operation (CP-036).
<br/>Improvement of the International Search Mask (CP-042).
<br/>CDA Display Tool supporting the Wave 4 - CDA Implementation Guidelines.
<br/>Detailed release notes: https://ec.europa.eu/cefdigital/wiki/x/YgATE

### Sub-task
- [EHNCP-1942] - IP address returned by TRC-STS not coherent for Portugal.
- [EHNCP-1955] - Implementation of Dispense Discard KPI.
- [EHNCP-1957] - OpenNCP default infrastructure evolution and upgrade.
- [EHNCP-1961] - Hibernate 5.4.18 vulnerabilities.
- [EHNCP-1966] - OpenNCP Java Dependencies Security analysis.
- [EHNCP-1967] - Releasing OpenNCP 5.1.0 (Wave 4 Preparatory version).
- [EHNCP-1969] - Alignment of the Gazelle validators to Wave 4.
- [EHNCP-1991] - Implement optional documents criteria parameters for CP-042 in Portals.

### Bug
- [EHNCP-1668] - PrettyPrinter uses a licensed Saxon feature.
- [EHNCP-1939] - ATNA persistence layer returns an exception with userId too long.
- [EHNCP-1952] - Transformation configuration not correct for PS - generalizedMedicineClass.
- [EHNCP-1958] - Serialization of ISM forms might not work on Windows OS.

### Story
- [EHNCP-1892] - Implement CP-036 in OpenNCP.
- [EHNCP-1895] - Implement CP-035 in OpenNCP.
- [EHNCP-1896] - Implement CP-042 in Interoperability Specifications.
- [EHNCP-1912] - Implement CP-033 in OpenNCP.

### Improvement
- [EHNCP-1510] - Specify the International Search Mask.
- [EHNCP-1962] - Empty parenthesis are shown for the Current problems section entry in the case of a nullFlavor.
- [EHNCP-1963] - Support indication of dosage period length for medication summary section.
- [EHNCP-1964] - Display descriptions from ICD-10 to the Problem column of the History of Past Illnesses section.

### Support
- [EHNCP-1511] - Substitution flag not sent to Country A when substitution is performed.

### Issue
- [EHNCP-1755] - LivingSubjectBirthPlaceAddress identity trait is not supported.
- [EHNCP-1970] - Java 8 and OpenSAML framework upgrade are creating an ACCESS_EXTERNAL_SCHEMA property.
- [EHNCP-1975] - TRC-STS: Evidence Emitter dependency is not needed.
- [EHNCP-1976] - eADC - XML Transformer issue NullPointerException.
- [EHNCP-1977] - Requests are logged in PROD.
- [EHNCP-1978] - ITI-38 audit message too long for OpenATNA's messageContent column.

## [5.0.0] - 07-07-2020
The OpenNCP 5.0.0 release is the CEF eHDSI Wave 4 Initial Release.
<br/>This version is the first release supporting the Change Proposals 36 and 42.
<br/>Improvement of the International Search Mask (CP-042).
<br/>Dispensation workflow extension - discard operation (CP-036).
<br/>CDA Display Tool supporting the Wave 4 - CDA Implementation Guidelines.
<br/>Detailed release notes: https://ec.europa.eu/cefdigital/wiki/x/sQFnDw
### Added
- [EHNCP-1954] - Support implementation of CP-036.
- [EHNCP-1953] - Support implementation of CP-042.
- [EHNCP-1951] - Provide CDA mock messages for Wave 4.
- [EHNCP-1896] - Add implementation for international search mask.
- [EHNCP-1892] - Add implementation for dispensation discard operation.

### Changed
- [EHNCP-1950] - Align xslt method names with new value set names.
- [EHNCP-1945] - Code quality - minor refactoring and logging improvements.
- [EHNCP-1944] - Adapt Audit Messages to Wave 4 specifications.
- [EHNCP-1941] - Display descriptions from ICD-10 for the clinical manifestations of allergic reactions.
- [EHNCP-1936] - Display the position of a vaccination in a series of immunizations when provided in the PS.
- [EHNCP-1935] - Improve the presentation of multi-ingredient medicinal products in the Medication Summary.
- [EHNCP-1927] - Replace hardcoded labels by concepts from the eHDSIDisplayLabel value set.
- [EHNCP-1926] - Improve the way the datatype IVL_TS is displayed in the CDA Display Tool.
- [EHNCP-1921] - Improve the labels in the display of the PS for 'creation date' and 'last update'.
- [EHNCP-1859] - Evidence Emitter - source code refactoring and improvements.

### Deprecated
- [N/A].

### Removed
- [N/A].

## [4.1.0] - 03-06-2020
The OpenNCP 4.1.0 release is the CEF eHDSI Wave 3 HotFix version - available for the eHDSI 2020 Re-Test Event.
<br/>This version is mainly improving the vulnerabilities from the JAR dependencies (Spring, Hibernate and SAML).
<br/>CDA Display Tool improvements according the Formal Test Event feedback.
<br/>Web Services communication is using SOAP 1.2 between all the components.
<br/>eHDSI Central Services (SMP Server) has been upgraded to 4.1.1-EC with Dynamic Discovery Client 1.11.
<br/>Detailed release notes: https://ec.europa.eu/cefdigital/wiki/x/qoKSDg
### Added
- [EHNCP-1938] - Central Services - SML/SMP server upgrade.
- [EHNCP-1924] - Add the possibility of TSAM returning semantic mappings from LTR.
- [EHNCP-1923] - OpenNCP Gateway - loading properties from database should be trimmed.
- [EHNCP-1919] - OpenNCP Java Dependencies Security analysis.
- [EHNCP-1863] - Enforce Soap 1.2 communication between National Connector B and Client Connector.
- [EHNCP-1746] - Display the description of the IPS Absent and Unknown Data concepts in the CDA Display.

### Changed
- [EHNCP-1934] - OpenNCP-Gateway demands username and password for non-authenticated proxy.
- [EHNCP-1933] - Move "units per intake" above the "frequency of intakes" in the prescription details.
- [EHNCP-1931] - Extended header, telecom types are hard coded, where they should come from the epSOSTelecomAddress VS.
- [EHNCP-1930] - Fix narrative text display if a large table is provided.
- [EHNCP-1928] - Allergies and Intolerances section - NullFlavor is displayed twice for the Agent.
- [EHNCP-1920] - OpenNCP minor improvements.
- [EHNCP-1917] - Medication Summary section - NullFlavor check doesn't work for substanceAdministration.
- [EHNCP-1916] - The ICD-10 code is no longer displayed in the Problem section.
- [EHNCP-1911] - Input fields during dispense process have encoding issues.
- [EHNCP-1907] - OpenNCP Portal should ignore unnecessary LivingSubjectID with value " ".
- [EHNCP-1906] - XCA Retrieve Document produces response with no valid HCID if the request contains wrong HCID.
- [EHNCP-1902] - Dispensation ID is null when there is no extension in ID of ePrescription.
- [EHNCP-1898] - Minor hot fix on HCP and TRC SAML assertions.
- [EHNCP-1897] - Problem (no fatal) retrieving cda document due to terminology file missing.
- [EHNCP-1893] - eHDSI SAML specification and OpenNCP incoherent for element healthcare facility type.
- [EHNCP-1887] - CDA validation issue of effectiveTime element for SubstanceAdministration.

### Deprecated
- [N/A].

### Removed
- [EHNCP-1885] - Patient summary portal consent checkboxes.

## [4.0.3] - 31-01-2020
The OpenNCP 4.0.3 release is the CEF eHDSI Wave 3 Third HotFix version - available for the eHDSI 2020 Formal Test Event.
<br/>This version is fixing an issue related to the Audit Message and the network equipments.
<br/>A fix has been applied to the CDA Display Tool for substitution purpose.
<br/>Marketing Authorization Holder has been also fixed since the last re-factoring of the CDA Display Tool.
<br/>Detailed release notes: https://ec.europa.eu/cefdigital/wiki/x/dguNCw
### Added
- [N/A].

### Changed
- [EHNCP-1884] - Marketing Authorization Holder not displayed after re-factoring of CDA DisplayTool.
- [EHNCP-1883] - Unexpected behaviour in some equipments causing an infinite loop.
- [EHNCP-1882] - Problem with CDA Display Tool and substitution checkbox.
- [EHNCP-1721] - Refactoring of the National Connector Mock.

### Deprecated
- [N/A].

### Removed
- [N/A].

## [4.0.2] - 13-01-2020
The OpenNCP 4.0.2 release is the CEF eHDSI Wave 3 Second HotFix version - available for the eHDSI Formal Test Event.
<br/>This is the first release compliant with the eHDSI MVC 3.1.0; most of the changes are related to the CDA Display Tool and the updated version of eHDSI CDA Implementation Guidelines 3.0.1.
<br/>Improvements have been implemented into the Audit Manager, Transformation Manager (configuration file embedded into the JAR) and Portals.
<br/>Oracle and PostgreSQL databases are now supported.
<br/>Detailed release notes: https://ec.europa.eu/cefdigital/wiki/x/QQEbCw
### Added
- [EHNCP-1871] - Adding default coded element list in Transformation Manager.
- [EHNCP-1869] - HCP and TRC Assertions - ServiceConsumer attribute set to localhost in NAT environment.
- [EHNCP-1844] - Authentication - role doctor is able to dispense a prescription.
- [EHNCP-1840] - OpenNCP Web Portal doesn't support all types of Identity Traits.
- [EHNCP-1839] - Add color layout to Logback configuration for clarity in logs.
- [EHNCP-1838] - Audit message (TRC, List and Retrieve Documents) providing HCP Role are not conformant with CP-023.
- [EHNCP-1837] - PostgreSQL database support.
- [EHNCP-1822] - Create ClientConnectorService client stub dynamic from WSDL definition.
- [EHNCP-1817] - OpenNCP Portal - Language_cs_CZ.properties - add new CZ translation.
- [EHNCP-1782] - Oracle Database 12c support.
- [EHNCP-1728] - Signed Audit messages - Gazelle Validation.
- [EHNCP-1578] - Add database schema management tool.

### Changed
- [EHNCP-1874] - Fix generic value set retrieval for known absence/no information scenarios.
- [EHNCP-1873] - ePrescription XML processing creates NullPointerException.
- [EHNCP-1870] - Personal data is being logged in PRODUCTION.
- [EHNCP-1868] - Improve distinction between date and time in the CDA Display Tool.
- [EHNCP-1867] - Improve PDF generation: align it with HTML view and apply small bug fixes.
- [EHNCP-1866] - Adapt extended header for Patient summary: distinguish clearly between assignedPerson and authoringDevice.
- [EHNCP-1865] - Filtering between other contacts and preferred Healthcare Provider is not done correctly.
- [EHNCP-1864] - CDA Display Tool - Prescriber telecom value not properly displayed.
- [EHNCP-1862] - CDA Display Tool - Remove brackets from strength.
- [EHNCP-1861] - Prescriber details are not displayed in CDA Display Tool.
- [EHNCP-1857] - Fix value set OIDs for no information/known absence cases in coded_element_list_epSOS2.xml.
- [EHNCP-1856] - Align coded_element_list_epSOS2.xml with correct XPATH.
- [EHNCP-1854] - Question about CDA Display Tool and procedures section.
- [EHNCP-1853] - CDA Display Tool - Fix bugs related to use of labels in ePrescription visualization.
- [EHNCP-1848] - Maven pom configuration build alignment.
- [EHNCP-1846] - Required attribute contextControlCode with value 'OP' is not added to the eDispensation document by the portal.
- [EHNCP-1843] - Proxy configuration is mandatory even if proxy is not used.
- [EHNCP-1841] - Wave 3 CDA L1 Validator has an incorect behaviour - hl7:realmCode.
- [EHNCP-1836] - TSAM Sync process is not handling concepts mappings.
- [EHNCP-1833] - Fixing Logback configuration typo in OpenNCP Portal.
- [EHNCP-1820] - Minor source code re-factoring of OpenNCP Web Portal.
- [EHNCP-1811] - National Connector Mock - Wave 3 CDA are not correct.
- [EHNCP-1799] - CDA Display Tool - Distinguish clearly between primary and secondary patientID.
- [EHNCP-1770] - Refactor CDA Display Tool Visualization for ePrescription.
- [EHNCP-1761] - LTRDB does not support Oracle Database - Hibernate Identifier strategy.

### Deprecated
- [N/A].

### Removed
- [N/A].

### Support
- [EHNCP-1855] - Question about pregnancy section.
- [EHNCP-1850] - Issue on CDA L1 retrieve PS-B.
- [EHNCP-1849] - Order Service test (epSOS_WF_ePresc_eDispens) : Friendly Dispensation document on NCP_A side.
- [EHNCP-1845] - Gazelle Validator eHDSI Audit - Fetch Document Service (Service Consumer) - Wave 3 (V3.1.0) Failure on ActiveParticipant > RoleIDCode.
- [EHNCP-1666] - Policy manager implementation guide.

## [4.0.1] - 11-10-2019
The OpenNCP 4.0.1 release is the CEF eHDSI Wave 3 RC HotFix version available for the eHDSI Preparatory Test Event.
<br/>Update of the validation performed by the default-policy-manager.
<br/>BugFix on XDS Metadata associated to the XCA List response message (incorrect type of documents).
<br/>Detailed release notes: https://ec.europa.eu/cefdigital/wiki/x/iwbKC
### Added
- [EHNCP-1829] Alignment of default-policy-manager to Change Proposal 23.
- [EHNCP-1824] OpenNCP ATNA webapp doesn't start properly.
- [EHNCP-1814] Audit message EHDSI-91 are not conformant with specification on portals.
- [EHNCP-1813] Alignment of Spring Boot and Spring Framework version among the project
- [EHNCP-1808] OpenNCP Web Portal is not handling ISM containing XML Namespaces.
- [EHNCP-1578] Add database schema management tool

### Changed
- [EHNCP-1830] Problem running OpenNCP TSAM-Sync.
- [EHNCP-1820] Minor source code re-factoring of OpenNCP Web Portal.
- [EHNCP-1819] Single comma displayed in Preferred HCP/Legal organization to contact.
- [EHNCP-1812] ePrescription list returned by OrderService contains incorrect type of document.
- [EHNCP-1811] National Connector Mock - Wave 3 CDA are not correct.
- [EHNCP-1743] Improve CDA Display tool in the case no primary care unit is provided.

### Deprecated
- [N/A].

### Removed
- [N/A].

## [4.0.0] - 23-09-2019
The OpenNCP 4.0.0 release is the CEF eHDSI Wave 3 Release Candidate available for the eHDSI Preparatory Test Event.
<br/>Most of the changed are related to the implementation of eHDSI Wave 3 Change Proposals.
<br/>Major technical update is related to the Apache Axis2 framework upgrade (1.6.2 to 1.7.9).
<br/>Detailed release notes: https://ec.europa.eu/cefdigital/wiki/x/rgC6Bg
### Added
- [EHNCP-1808] OpenNCP Web Portal is not handling ISM containing XML Namespaces.
- [EHNCP-1794] Dynamic Discovery Client doesn't work properly with Proxy and TLS.
- [EHNCP-1788] CP-eHealthDSI-023: SAML - Update Structural Role values of the HP.
- [EHNCP-1787] CP-eHealthDSI-024: Formalize the 'Description' element in the eP list.
- [EHNCP-1754] Use the labels from the MVC 3.0.0 RC1 instead of the hardcoded labels in the CDA Display Tool.
- [EHNCP-1753] Implement CP25 in the CDA Display Tool.
- [EHNCP-1752] Display Marketing Authorization Holder (MAH) in CDA Display Tool.
- [EHNCP-1737] Align value sets in MVC 3.0.0 for the representation of "no information"/"known absence" scenarios.

### Changed
- [EHNCP-1810] Add date information into the log files - Logback configuration.
- [EHNCP-1809] Update Gazelle configuration in order to call formal Wave 3 validators.
- [EHNCP-1807] Update national connector mock with Wave 3 examples.
- [EHNCP-1806] Alignment of audit messages and the Wave 3 specifications - Code System Name.
- [EHNCP-1804] NullFlavored brand names of immunizations are not displayed.
- [EHNCP-1801] TSAM-Sync backup filename only contains the date.
- [EHNCP-1798] Incorrect separator in openncp-web-portal.
- [EHNCP-1797] NullFlavor tooltip is cut by table margin.
- [EHNCP-1795] Harmonize OpenNCP Java dependencies.
- [EHNCP-1785] Frequency of intakes not well displayed in CDA Display Tool in the case of the EIVL_TS datatype.
- [EHNCP-1784] Administered icons are not displayed in PDF version of the Patient Summary.
- [EHNCP-1713] Some labels used into OpenNCP DisplayTool are not part of the VS epSOSDisplayLabel.
- [EHNCP-1571] Axis2 Framework version upgrade (1.7.9).

### Deprecated
- [N/A].

### Removed
- [N/A].

## [3.1.0] - 05-08-2019
The OpenNCP 3.1.0 release is the CEF eHDSI Wave 2 Operation Ready version which is fixing the different findings discovered during the latest Formal Test Event.
<br/>Most of the changed are related to the Audit and ATNA messages and the identification of the different network components used.
<br/>Detailed release notes: https://ec.europa.eu/cefdigital/wiki/x/qgC6Bg
### Added
- [EHNCP-1793] Audit Messages MUST be signed.
- [EHNCP-1792] eHNCP MUST write an audit trail entry for the CDA pivot translation.
- [EHNCP-1774] Audit Service: Implement threading mechanism.
- [EHNCP-1771] eADC: Implement threading mechanism
- [EHNCP-1768] CDA Display Tool HTML style CSS body padding too large 5rem.
- [EHNCP-1766] Refactor the Audit Manager with Cron Job or clean threading implementation.

### Changed
- [EHNCP-1783] eADC - XCA and XDR Retrieve Operation cannot store CDA's correct information.
- [EHNCP-1781] Audit Manager source code re-factoring.
- [EHNCP-1780] OpenNCP ATNA FailedLogsHandlerService is not working properly.
- [EHNCP-1779] Sections narrative text is not displayed correctly in CDA Display Tool.
- [EHNCP-1777] SMP Query and Push Audit messages attribute NetworkAccessPointID are not correct.
- [EHNCP-1776] Server and Client IPs are incorrect in NAT environments.
- [EHNCP-1773] Null-flavored Immunization with green check in Administered column.
- [EHNCP-1769] Problem fetching eP file from Greek OpenNCP.
- [EHNCP-1495] Refactoring of eADC according updated specifications.

### Deprecated
- [N/A].

### Removed
- [N/A].

## [3.0.0] - 04-06-2019
The OpenNCP 3.0.0 release is the CEF eHDSI Wave 2 required for the 2019 June eHDSI Forma Test Event.
Detailed release note: https://ec.europa.eu/cefdigital/wiki/x/nLSuB
### Added
- Wave 2: CDA Implementation Guide & CDA DisplayTool [EHNCP-1724] [EHNCP-1726] [EHNCP-1727].
- ISM XSD doesn't allow more than 1 textField element [EHNCP-1751].
- ISM XSD doesn't accept friendlyName attribute on elements other than textField [EHNCP-1750].
- CDA Display Tool - Clinical Manifestation is not translated [EHNCP-1732].
- Root CA Certificates from eDelivery SMP have to be imported automatically in OpenNCP truststore [EHNCP-1570].

### Changed
- Alignment of CDA Extended definition with Art Decor [EHNCP-1633].
- OpenSAML framework into OpenNCP Portal doesn't start correctly in some situation [EHNCP-1762].
- Audit Service - FailedLogHandler doesn't work properly [EHNCP-1765].
- OpenNCP Commons Components - Dynamic Discovery [EHNCP-1764].
- OpenNCP Web Portal - unexpected exceptions when multiple users are logged in [EHNCP-1244].
- TSAM Exporter does not recognize correctly Environment variable [EHNCP-1722].
- CDA Display Tool - Minor improvements [EHNCP-1725] [EHNCP-1743] [EHNCP-1744] [EHNCP-1757].
- First-Time Configuration Utility doesn't recognize properties table [EHNCP-1696].
- Minor code refactoring and BugFix on OpenNCP components [EHNCP-1698].

### Deprecated
- [N/A].

### Removed
- Remove the Database Logging (Logback) [EHNCP-1759].

## [3.0.0.RC4] - 30-04-2019
### Added
- Full support of CDA with elements and namespace prefixes [EHNCP-1716].
- SMP-Editor - XSD validation of the International Search Mask [EHNCP-1742].
- eHDSI Gazelle validation (asynchronous call, threading mechanism) and improvement of the evidences generated (format).
- SMP audit message adding correct NetworkAccessPointID [EHNCP-1723].
- Improvement of the data received from the Portal (trim of PAtient Demographics).
- ATC code added to coded_element_list_epSOS2.xml configuration file.
- Exception handling enhancement: eD submission (XDR) [EHNCP-1377] and XCA List and Retrieve at NCP-A [EHNCP-1425].

### Changed
- CDA Document processing are producing xmlns empty attributes [EHNCP-1714].
- eHDSI Gazelle missing evidences [EHNCP-1718] [EHNCP-1719].
- XCPD request and response, adding precision for creation date.
- Adapt Home Community ID for Spain.
- OpenNCP Commons Components - minor bug fix.
- SAML Assertions fix (Web Portal) and version alignment among the project [EHNCP-1729].
- OpenNCP National Connector Mock implementation - minor bug fix.
- CDA Display Tool enhancement.
- Audit message content fix after an error has been raised [EHNCP-1166].
- Logging appender misconfiguration fixed [EHNCP-1676].

### Deprecated
- eSense obligation for Consent transactions.
- Patient Consent handling out of eHDSI scope (creation, revocation).
- Eclipse Oomph setup removed.

### Removed
- [N/A].

## [3.0.0.RC3] - 10-02-2019
### Added
- [N/A].

### Changed
- [N/A].

### Deprecated
- [N/A].

### Removed
- Dependency openws.jar has been removed from the component Evidence Emitter to prevent classloading issue [EHNCP-1697]

## [3.0.0.RC2] - 06-02-2019
### Added
- eHDSI Gazelle platform updated: all the validators are now labelled according the Wave & Specification version.

### Changed
- Identity HCP Assertions validity has been extended to 4 hours instead of 2 hours (conformance to specification).
- Removing Personal Data (Patient ID) from the NRO evidences file produced during the Dispense (XDR).
- CDA DisplayTool improvements and alignment to eHDSI Specification (Change Proposal 09).
- Server Monitoring (Javamelody) and Running Mode (PAT messages) tools have been fixed (minor issue).
- Bug fix on the HCP Assertions signature (wrong Canonicalization method).
- Bug fix on XDR Request message: field authorPerson was not correct (HTML escape character).
- Member State HomeCommunityId list updated (pn-oid.xml).

### Deprecated
- [N/A].

### Removed
- eID confirmation button has been removed from the OpenNCP Demo Portal.
- OpenATNA logs have been cleaned (verbose and useless information removed).

## [3.0.0.RC1] - 11-12-2018
### Added
- Support for Wave 2 CDA Implementation Guidelines.

### Changed
- OpenSAML framework upgrade from 2.6.X (not supported anymore) to 3.3.1.
- OpenNCP Portal language properties files are now directly stored in ASCII.
- CDA DisplayTool is now showing original language of the CDA instead of user language

### Deprecated
- [N/A].

### Removed
- XSPA role "medical doctor" has been removed from the HCP Assertion generator, the role is still accepted by the
  default policy.
- Remove generated JAXB class from Evidence Emitter ETSI-REM (created on the fly).

## [2.5.6] - 19-07-2019
OpenNCP 2.5.6 is a maintenance release related to eHDSI Wave 1 fixing minor issues (CDA DisplayTool, eADC etc.).<br/>
Detailed release notes: https://ec.europa.eu/cefdigital/wiki/x/hAkIBQ
### Added
- [EHNCP-1783] eADC - XCA and XDR Retrieve Operation cannot store CDA's correct information.
- [EHNCP-1779] Sections narrative text is not displayed correctly in CDA Display Tool.
- [EHNCP-1672] Ampersand entity character incorrect in XDR Request.
- [EHNCP-1670] Uncomment use of CDA Display Label concept for eP brand name substitution.
- [EHNCP-1637] Adding configuration to enable/disable Javamelody Monitoring.

### Changed
- [EHNCP-1686] Canonicalization method algorithm is not correct according eHDSI Specification.
- [EHNCP-1680] Incoherent OIDs for pn-oid.xml.
- [EHNCP-1677] eHDSI Gazelle Validators re-factoring.
- [EHNCP-1676] Wrong appender-ref in all logback.xml.
- [EHNCP-1673] Identity Assertion Conditions dates are not correct.
- [EHNCP-1671] File names of NRO obligations for XDR contain Patient IDs.
- [EHNCP-1244] Unexpected exceptions when multiple users are logged in.

### Deprecated
- N/A

### Removed
- N/A

## [2.5.5] - 27-11-2018
### Added
- Audit messages are now supporting multiple ParticipantObjectId (i.e. List of Documents Ids provided).
- OpenNCP Portal property added to set the XSPA Locality to SAML HCP assertions.
- Audit messages for internal NCPs operations added.

### Changed
- Audit messages Participants Identifier aligned with specifications (HP Assurance Audit Schema, Patient Privacy Audit Schema and
  Patient ID Mapping Audit Schema).
- eHDSI Gazelle test platform updated (new Schematron WSDL definition).
- OpenNCP Gazelle Validation updated with new Schematron WS client.
- Enabling & disabling feature for Javamelody monitoring tool.
- Definition of a minimal Logging level for Production.
- Update of the OpenNCP Property database schema definition (removing usage of reserved word).
- Alignment of the Audit messages validators use according Event Type and tests definitions.
- Update of the TRC Assertions output based on HCP Assertions provided.
- eHDSI Gazelle Validators are set to Wave 1 requirements (manual customization available)

### Deprecated
- [N/A].

### Removed
- Removing SchemaLocation attribute definition into CDA handling operations (security).

## [2.5.4] - 05-10-2018
OpenNCP Wave 1 Operation Ready Hotfix and Wave 2 Release Candidate.<br/>
Detailed release notes: https://ec.europa.eu/cefdigital/wiki/x/Ha4SB
### Added
- [N/A].
### Changed
- [N/A].
### Deprecated
- [N/A].
### Removed
- [N/A].

## [2.5.3] - 28-08-2018
OpenNCP - eHDSI Wave 1 Deploying Countries.<br/>
Detailed release notes: https://ec.europa.eu/cefdigital/wiki/x/JIK-Aw
### Added
- Implementation of the SML Discovery standalone client.
- TSAM Exporter: Output Files are now generated based on OpenNCP Value Set and not Code System.
### Changed
- OpenNCP Portal - SML/SMP support.
- TSAM Exporter - Migration to Spring Boot framework.
### Deprecated
- [N/A].
### Removed
- [N/A].

### [2.5.3.RC3] - 18-07-2018
- Detailed information available from parent version [2.5.3]
### [2.5.3.RC2] - 30-04-2018
- Detailed information available from parent version [2.5.3]
### [2.5.3.RC1] - 19-01-2018
- Detailed information available from parent version [2.5.3]

## [2.5.2] - 18-12-2017
This version is the Final release for the eHDSI Wave 1 Deploying Countries and it should be used during the test events in January 2018.<br/>
Detailed release notes: https://ec.europa.eu/cefdigital/wiki/x/jbVUAw including RC1 to RC5 versions.
### Added
- [N/A]..
### Changed
- [N/A]..
### Deprecated
- [N/A]..
### Removed
- [N/A]..

### [2.5.2.RC5] - 30-11-2017
- Detailed information available from parent version [2.5.2]
### [2.5.2.RC4] - 21-11-2017
- Detailed information available from parent version [2.5.2]
### [2.5.2.RC3] - 03-10-2017
- Detailed information available from parent version [2.5.2]
### [2.5.2.RC2] - 15-09-2017
- Detailed information available from parent version [2.5.2]
### [2.5.2.RC1] - 09-08-2017
- Detailed information available from parent version [2.5.2]

## [2.5.1] - 19-06-2017
OpenNCP HotFix version for the eHDSI Wave 1 Deploying Countries, it should be use during the test events in June and September 2017.<br/>
Detailed release notes: https://ec.europa.eu/cefdigital/wiki/x/mn3QAg.
### Added
- Support of eHDSI Gazelle online validation.
### Changed
- Fixing XCA retrieve issue (endpoints cleared after error).
- Adding proxy configuration and authentication.
- Fixing the initial MySQL script for the LTRDB.
- Fixing the Path issue for the audit-backup folder required when the ATNA service is down.
### Deprecated
- [N/A]..
### Removed
- [N/A]..

## [2.5.0] - 07-06-2017
The OpenNCP 2.5.0 is the the version that eHDSI Wave 1 Deploying Countries should use during the test events in June and September 2017.<br/>
### Added
- EHNCP-1092 Migrating from JDK7 to JDK8.
- EHNCP-1090 Spring Framework version migration.
- EHNCP-1086 Migration to the Logback logging framework.
### Changed
- [N/A].
### Deprecated
- [N/A].
### Removed
- [N/A].

### [2.5.0.RC1] - 28-03-2017
This release is a major release as it is the first complete and aligned release under the eHealth DSI governance.<br/>
The current OpenNCP Reference Implementation is now fully supporting Java 8.<br/>
The OpenNCP 2.5.0.RC1 is now ready for the next connectathon session and this is the entry point for Going Live.
### Added
- EHNCP-1128 Create a profile for running integration tests.
- EHNCP-1111 Portal domain's object are not serializable and could create exception.
- EHNCP-1109 Model object Country is not serializable.
- EHNCP-1092 Migrating from JDK7 to JDK8.
- EHNCP-1090 Spring Framework version migration.
- EHNCP-1086 Migration to the Logback logging framework.
### Changed
- EHNCP-1131 XCA/XDR Retrieve Response Error with ePrescription documents.
- EHNCP-1127 UTF-8 problems due to OpenNCP Web Portal migration from Atlassian cloud.
- EHNCP-1122 Hexadecimal dumping in OpenATNA log.
- EHNCP-1116 Update Gazelle url endpoints.
- EHNCP-1115 XCA Server Error during XCA List Request.
- EHNCP-1112 Jasper reports - deprecated parameter 'isSplitAllowed'.
- EHNCP-1110 Displaying a PS through the Portal does not render the html labels.
- EHNCP-1107 Deployment Warning Tomcat 8 AntiJarLocking.
- EHNCP-1099 Error while building OpenNCP in clean environment.
- EHNCP-1085 Code factoring optimizations and quality review.
### Deprecated
- EHNCP-1094 References to deprecated XMLGregorianCalendarImpl constructor prevent build of OpenNCP.
### Removed
- [N/A].

## [2.4.3] - 01-02-2017
This release fixes minor issues of the OpenNCP reference implementation ready for the EU BootCamp session.
### Added
- [N/A].
### Changed
- Code improvements and SMP discriminator applied for properties retrieval.
- OpenNCP Web Portal: Integration with the current 2.4.3 implementation.
- Re-branding epSOS to OpenNCP reference implementation.
- Moving logging framework from java.util.Logging and Log4j to Logback + Slf4j.
- Version alignment of the core OpenNCP frameworks dependencies (Spring, Hibernate etc.).
### Deprecated
- [N/A].
### Removed
- Removing deprecated and duplicated external libraries.

## [2.4.2] - 08-11-2016
OpenNCP version used during the eHealth Week in Greece.
### Added
- Integration of a cache responsible for retrieving required properties by OpenNCP including the SMP lookup.
### Changed
- Re-branding epSOS to OpenNCP, organization of the new repositories.
### Deprecated
- [N/A].
### Removed
- [N/A].

## [2.4.1] - 24-05-2016
### Added
- [N/A].
### Changed
- BugFix on Audit Manager causing troubles on Order services.
- BugFix from Connectation 2016.
### Deprecated
- [N/A].
### Removed
- [N/A].

## [2.4.0] - 08-04-2016
This release is the final result of different testing healthcare events (Connectathon and Expandathon 2015).<br/>
Preparatory release of Connectathon 2016.
### Added
- eSens support of the eID level 3.
- Transformation of TLS configuration files to SMP (eDelivery building block).
### Changed
- CDA Display tool enhancements.
- Audit Manager reducing verbose logging and security configuration).
### Deprecated
- [N/A].
### Removed
- [N/A].

## [2.3.0-RC0] - 14-08-2015
### Added
- Implementation of the eSens eID Level 1 (LARMS) on the OpenNCP Portal.
### Changed
- Liferay Portal version 6.2.2 artifacts - this is the recomended version of the Liferay Portal.
### Deprecated
- [N/A].
### Removed
- [N/A].

## [2.2.0] - 22-05-2015
### Added
- [N/A].
### Changed
- Component tsl-sync: changes for assembling a jar with all the dependencies.
- Component tsl-editor: changes for assembling a jar with all the dependencies.
### Deprecated
- [N/A].
### Removed
- [N/A].

## [2.1.1] - 09-06-2014
### Added
- HotFix after IHE Connectathon 2014 in Vienna.
### Changed
- [N/A].
### Deprecated
- [N/A].
### Removed
- [N/A].

## [2.1.0] - 17-12-2013
### Added
- Support for a NEW epSOS 2 Service: Medication Related Overview (MRO).
### Changed
- CI Automated Testing Suite Enhancements.
### Deprecated
- [N/A].
### Removed
- [N/A].

## [2.0.3] - 04-11-2013
OpenNCP BugFix version of 2.0.2.
### Added
- [N/A].
### Changed
- CDA DisplayTool BugFix and improvements.
### Deprecated
- [N/A].
### Removed
- [N/A].

## [2.0.2] - 12-09-2013
### Added
- [N/A].
### Changed
- Application Log messages have been set correctly to TOMCAT_HOME/logs.
### Deprecated
- [N/A].
### Removed
- [N/A].

## [2.0.1] - 06-08-2013
### Added
- New implementation of Audit Manager, with new audit message event Id's.
- Audit messages for PAC and HCER (epsos 2 services).
### Changed
### Deprecated
### Removed

## [2.0.0] - 20-06-2013
OpenNCP release 2.0.0 - epSOS services 2 and features from epSOS 1 services including Audit Trails.
### Added
- epSOS 2 PAC service.
- epSOS 2 HCER service.
### Changed
- [N/A].
### Deprecated
- [N/A].
### Removed
- [N/A].

## [1.1.3] - 08-05-2013
Release 1.1.3 is the approved release after the IHE CAT.
### Added
- [N/A].
### Changed
- [N/A].
### Deprecated
- [N/A].
### Removed
- [N/A].

## [1.1.2] - 10-04-2013
Release 1.1.2 is a HotFix preparatory version for IHE CAT.
### Added
- [N/A].
### Changed
- [N/A].
### Deprecated
- [N/A].
### Removed
- [N/A].

## [1.1.1] - 05-03-2013
Release 1.1.1 is the result of the experiences gathered by the nations testing at the December 2012 PPT.
### Added
- [N/A].
### Changed
- [N/A].
### Deprecated
- [N/A].
### Removed
- [N/A].

## [1.1.0] - 24-01-2013
Release 1.1.0 is the first PPT-tested release of OpenNCP, and as such, OpenNCP is considered out of Beta testing!
### Added
- [N/A].
### Changed
- [N/A].
### Deprecated
- [N/A].
### Removed
- [N/A].

## [1.0.2] - 09-01-2013
Release 1.0.2 of the OpenNCP is a hotfix release, adding the dependencies of the defaultpolicy manager released with 1.0.1.
### Added
- [N/A].
### Changed
- [N/A].
### Deprecated
- [N/A].
### Removed
- [N/A].

## [1.0.1] - 08-01-2013
Release 1.0.1 of the OpenNCP is a scheduled BugFix release, among other things collecting on the experiences done by Finland and Portugal during the IHE PPT session before Christmas 2012.
### Added
- [N/A].
### Changed
- [N/A].
### Deprecated
- [N/A].
### Removed
- [N/A].

## [1.0.0] - 09-11-2012
Release 1.0.0 of the OpenNCP, based on the original SRDC NCP, with many additions and enhancements by OpenNCP team.<br/>
Implementation for the Country A role is based on the Axis2 framework. There are three service endpoints (XCPD, XCA and XDR).
### Added
- Identification service, operation getIdentityByTraits(). Only search by patient ids is implemented.
- Order service, operations list() and retrieve().
- Patient service, operations list() and retrieve().
- Dispensation service, operation initialize(). Operation discard() is not implemented.
- Consent service, operation put(). Operation discard() is not implemented.
- Interfaces for connecting the international part and national connector gateways are included.
### Changed
- [N/A].
### Deprecated
- [N/A].
### Removed
- [N/A].
