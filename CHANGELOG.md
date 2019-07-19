# Change Log

All notable changes to the <a href="https://ec.europa.eu/cefdigital/wiki/x/8CEZAg" rel="nofollow">CEF EHDSI project</a> will be documented in this file.<br/> 
This project adheres to <a href="http://semver.org/" rel="nofollow">Semantic Versioning</a>.

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
- [EHNCP-1686]	Canonicalization method algorithm is not correct according eHDSI Specification.
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
OpenNCP 2.5.5 is the official version Operation Ready to be used for eHDSI Wave 1 Go Live.<br/>
Detailed release notes: https://ec.europa.eu/cefdigital/wiki/x/Ny9HB
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
- N/A
### Removed
- Removing SchemaLocation attribute definition into CDA handling operations (security).

## [2.5.4] - 05-10-2018
OpenNCP Wave 1 Operation Ready Hotfix and Wave 2 Release Candidate.<br/>
Detailed release notes: https://ec.europa.eu/cefdigital/wiki/x/Ha4SB
### Added
- N/A
### Changed
- N/A
### Deprecated
- N/A
### Removed
- N/A

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
- N/A
### Removed
- N/A

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
- N/A.
### Changed
- N/A.
### Deprecated
- N/A.
### Removed
- N/A.

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
- N/A.
### Removed
- N/A.

## [2.5.0] - 07-06-2017
The OpenNCP 2.5.0 is the the version that eHDSI Wave 1 Deploying Countries should use during the test events in June and September 2017.<br/>
### Added
- EHNCP-1092 Migrating from JDK7 to JDK8.
- EHNCP-1090 Spring Framework version migration.
- EHNCP-1086 Migration to the Logback logging framework.
### Changed
- N/A
### Deprecated
- N/A
### Removed
- N/A

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
- N/A

## [2.4.3] - 01-02-2017
This release fixes minor issues of the OpenNCP reference implementation ready for the EU BootCamp session.
### Added
- N/A
### Changed
- Code improvements and SMP discriminator applied for properties retrieval.
- OpenNCP Web Portal: Integration with the current 2.4.3 implementation.
- Re-branding epSOS to OpenNCP reference implementation.
- Moving logging framework from java.util.Logging and Log4j to Logback + Slf4j.    
- Version alignment of the core OpenNCP frameworks dependencies (Spring, Hibernate etc.).
### Deprecated
- N/A
### Removed
- Removing deprecated and duplicated external libraries.

## [2.4.2] - 08-11-2016
OpenNCP version used during the eHealth Week in Greece. 
### Added
- Integration of a cache responsible for retrieving required properties by OpenNCP including the SMP lookup.
### Changed
- Re-branding epSOS to OpenNCP, organization of the new repositories.
### Deprecated
- N/A
### Removed
- N/A

## [2.4.1] - 24-05-2016
### Added
- N/A
### Changed
- BugFix on Audit Manager causing troubles on Order services.
- BugFix from Connectation 2016.
### Deprecated
- N/A
### Removed
- N/A

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
- N/A
### Removed
- N/A

## [2.3.0-RC0] - 14-08-2015
### Added
- Implementation of the eSens eID Level 1 (LARMS) on the OpenNCP Portal.
### Changed
- Liferay Portal version 6.2.2 artifacts - this is the recomended version of the Liferay Portal.
### Deprecated
- N/A
### Removed
- N/A

## [2.2.0] - 22-05-2015
### Added
- N/A
### Changed
- Component tsl-sync: changes for assembling a jar with all the dependencies.
- Component tsl-editor: changes for assembling a jar with all the dependencies.
### Deprecated
- N/A
### Removed
- N/A

## [2.1.1] - 09-06-2014
### Added
- HotFix after IHE Connectathon 2014 in Vienna.
### Changed
- N/A
### Deprecated
- N/A
### Removed
- N/A

## [2.1.0] - 17-12-2013
### Added
- Support for a NEW epSOS 2 Service: Medication Related Overview (MRO).
### Changed
- CI Automated Testing Suite Enhancements.
### Deprecated
- N/A
### Removed
- N/A

## [2.0.3] - 04-11-2013
OpenNCP BugFix version of 2.0.2.
### Added
- N/A
### Changed
- CDA DisplayTool BugFix and improvements.
### Deprecated
- N/A
### Removed
- N/A

## [2.0.2] - 12-09-2013
### Added
- N/A
### Changed
- Application Log messages have been set correctly to TOMCAT_HOME/logs.
### Deprecated
- N/A
### Removed
- N/A

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
- N/A
### Deprecated
- N/A
### Removed
- N/A

## [1.1.3] - 08-05-2013
Release 1.1.3 is the approved release after the IHE CAT.
### Added
- N/A
### Changed
- N/A
### Deprecated
- N/A
### Removed
- N/A

## [1.1.2] - 10-04-2013
Release 1.1.2 is a HotFix preparatory version for IHE CAT. 
### Added
- N/A
### Changed
- N/A
### Deprecated
- N/A
### Removed
- N/A

## [1.1.1] - 05-03-2013
Release 1.1.1 is the result of the experiences gathered by the nations testing at the December 2012 PPT.
### Added
- N/A
### Changed
- N/A
### Deprecated
- N/A
### Removed
- N/A

## [1.1.0] - 24-01-2013
Release 1.1.0 is the first PPT-tested release of OpenNCP, and as such, OpenNCP is considered out of Beta testing!
### Added
- N/A
### Changed
- N/A
### Deprecated
- N/A
### Removed
- N/A

## [1.0.2] - 09-01-2013
Release 1.0.2 of the OpenNCP is a hotfix release, adding the dependencies of the defaultpolicy manager released with 1.0.1.
### Added
- N/A
### Changed
- N/A
### Deprecated
- N/A
### Removed
- N/A

## [1.0.1] - 08-01-2013
Release 1.0.1 of the OpenNCP is a scheduled BugFix release, among other things collecting on the experiences done by Finland and Portugal during the IHE PPT session before Christmas 2012.
### Added
- N/A
### Changed
- N/A
### Deprecated
- N/A
### Removed
- N/A

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
- N/A
### Deprecated
- N/A
### Removed
- N/A