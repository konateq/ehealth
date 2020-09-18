# Change Log

All notable changes to the <a href="https://ec.europa.eu/cefdigital/wiki/x/8CEZAg" rel="nofollow">CEF EHDSI project</a> will be documented in this file.<br/> 
This project adheres to <a href="http://semver.org/" rel="nofollow">Semantic Versioning</a>.

## [4.1.1] - 17-09-2020
The OpenNCP 4.1.1 HotFix is the release available in the context of CEF eHDSI Wave 3, available for the eHDSI Wave 3 Upgrade. 
This version is fixing specific issues encountered by some Member State in their National infrastructure.
Main issues fixed are described below:
<br/>Fixing eADC component and TransformerFactoryImpl implementation (forced to use Saxon).
<br/>Transformation Manager configuration file updated for Patient Summary.
<br/>Evidence Emitter evidences logged even in Production mode has been removed.
<br/>TRC-STS not retrieving anymore localhost or local IP address.
<br/>Fixing File serialization on Windows Operating System.
<br/>Non-Repudiation component removed from the TRC-STS module (not used).
<br/>ATNA Database schema - MySQL BLOB(64KB) type replaced by LONGBLOB(up to 4GB).
### Added:
- [EHNCP-1952] - Transformation configuration not correct for PS - generalizedMedicineClass.

### Changed
- [EHNCP-1978] - ITI-38 audit message too long for OpenATNA's messageContent column.
- [EHNCP-1977] - Requests are logged in PROD.
- [EHNCP-1976] - eADC - XML Transformer issue NullPointerException.
- [EHNCP-1958] - Serialization of ISM forms might not work on Windows OS.
- [EHNCP-1942] - IP address returned by TRC-STS not coherent for Portugal.
- [EHNCP-1939] - ATNA persistence layer returns an exception with userId too long.

### Deprecated
- [N/A].

### Removed
- [EHNCP-1975] - TRC-STS: Evidence Emitter dependency is not needed.
- [EHNCP-1970] - Java 8 and OpenSAML framework upgrade are creating an ACCESS_EXTERNAL_SCHEMA property.

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
