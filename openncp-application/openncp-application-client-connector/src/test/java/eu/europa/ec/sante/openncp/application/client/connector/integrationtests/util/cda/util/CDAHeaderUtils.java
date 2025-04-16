package eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.cda.util;

import eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.cda.model.AuthorInformation;
import eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.cda.model.CustodianInformation;
import eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.cda.model.LegalAuthenticatorInformation;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.ElementFilter;

import java.util.Date;

public class CDAHeaderUtils {

    private static final String CDA_II_EXTENSION = "extension";
    private static final String CDA_II_ROOT = "root";
    private static final String CDA_CLASS_CODE = "classCode";

    private CDAHeaderUtils() {
    }

    public static void addRecordTarget(Element rootElement, org.jdom2.Document ePDocument) {
        CDAUtils.copyAllNodesAndAttributes(ePDocument, null, rootElement, "recordTarget");
    }

    public static void addAuthor(Namespace hl7Namespace, Element rootElement, AuthorInformation authorInformation) {
        var author = new Element("author", hl7Namespace);
        author.setAttribute("typeCode", "AUT");
        author.setAttribute("contextControlCode", "OP");
        addFunctionCode(hl7Namespace, author);
        addTime(hl7Namespace, author);
        addAssignedAuthor(hl7Namespace, author, authorInformation);
        rootElement.addContent(author);
    }

    public static void addCustodian(Namespace hl7Namespace, Element rootElement, CustodianInformation custodianInformation) {
        var custodian = new Element("custodian", hl7Namespace);
        var assignedCustodian = new Element("assignedCustodian", hl7Namespace);
        assignedCustodian.setAttribute(CDA_CLASS_CODE, "ASSIGNED");
        var representedCustodianOrganization = new Element("representedCustodianOrganization", hl7Namespace);
        representedCustodianOrganization.setAttribute(CDA_CLASS_CODE, "ORG");
        representedCustodianOrganization.setAttribute("determinerCode", "INSTANCE");
        var id = new Element("id", hl7Namespace);
        id.setAttribute(CDA_II_ROOT, custodianInformation.getIdentifier());
        representedCustodianOrganization.addContent(id);
        var name = new Element("name", hl7Namespace);
        name.addContent(custodianInformation.getName());
        representedCustodianOrganization.addContent(name);
        var telecom = new Element("telecom", hl7Namespace);
        telecom.setAttribute("nullFlavor", "NI");
        representedCustodianOrganization.addContent(telecom);
        var addr = new Element("addr", hl7Namespace);
        var country = new Element("country", hl7Namespace);
        country.addContent(custodianInformation.getCountry());
        addr.addContent(country);
        representedCustodianOrganization.addContent(addr);
        assignedCustodian.addContent(representedCustodianOrganization);
        custodian.addContent(assignedCustodian);
        rootElement.addContent(custodian);
    }

    public static void addLegalAuthenticator(Namespace hl7Namespace, Element rootElement, LegalAuthenticatorInformation legalAuthenticatorInformation) {
        var legalAuthenticator = new Element("legalAuthenticator", hl7Namespace);
        var time = new Element("time", hl7Namespace);
        time.setAttribute("value", HL7Util.formatDateHL7(new Date()));
        legalAuthenticator.addContent(time);
        var signatureCode = new Element("signatureCode", hl7Namespace);
        signatureCode.setAttribute("code", "S");
        legalAuthenticator.addContent(signatureCode);
        addAssignedEntity(hl7Namespace, legalAuthenticator, legalAuthenticatorInformation);
        rootElement.addContent(legalAuthenticator);
    }

    public static void addInFulfillmentOf(Namespace hl7Namespace, Element rootElement, org.jdom2.Document ePDocument) {
        var inFulfillmentOf = new Element("inFulfillmentOf", hl7Namespace);
        var order = new Element("order", hl7Namespace);
        order.setAttribute("moodCode", "RQO");
        var id = new Element("id", hl7Namespace);

        var root = ePDocument.getRootElement();
        var filter = new ElementFilter("section");
        for (Element c : root.getDescendants(filter)) {
            var sectionId = c.getChild("id", hl7Namespace);
            if (sectionId.getAttribute(CDA_II_EXTENSION) != null) {
                id.setAttribute(CDA_II_EXTENSION, sectionId.getAttribute(CDA_II_EXTENSION).getValue());
            }
            id.setAttribute(CDA_II_ROOT, sectionId.getAttribute(CDA_II_ROOT).getValue());
        }
        order.addContent(id);
        inFulfillmentOf.addContent(order);
        rootElement.addContent(inFulfillmentOf);
    }

    private static void addFunctionCode(Namespace hl7Namespace, Element rootElement) {
        var functionCode = new Element("functionCode", hl7Namespace);
        functionCode.setAttribute("code", "2262");
        functionCode.setAttribute("codeSystem", eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.cda.enums.CodeSystem.ISCO.getOid());
        functionCode.setAttribute("codeSystemName", eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.cda.enums.CodeSystem.ISCO.getName());
        functionCode.setAttribute("displayName", "Pharmacists");
        rootElement.addContent(functionCode);
    }

    private static void addTime(Namespace hl7Namespace, Element rootElement) {
        var time = new Element("time", hl7Namespace);
        //TODO To be aligned with eD document effectiveTime
        time.setAttribute("value", HL7Util.formatDateHL7(new Date()));
        rootElement.addContent(time);
    }

    private static void addAssignedAuthor(Namespace hl7Namespace, Element rootElement, AuthorInformation authorInformation) {
        var assignedAuthor = new Element("assignedAuthor", hl7Namespace);
        assignedAuthor.setAttribute(CDA_CLASS_CODE, "ASSIGNED");
        CDAUtils.addId(hl7Namespace, authorInformation.getIdentifier(), assignedAuthor);
        CDAUtils.addAddress(hl7Namespace, authorInformation.getAddress(), assignedAuthor);
        CDAUtils.addTelecom(hl7Namespace, authorInformation.getTelecom(), assignedAuthor);
        CDAUtils.addAssignedPerson(hl7Namespace, authorInformation.getAssignedPerson(), assignedAuthor);
        CDAUtils.addRepresentedOrganization(hl7Namespace, authorInformation.getRepresentedOrganization(), assignedAuthor);
        rootElement.addContent(assignedAuthor);
    }

    private static void addAssignedEntity(Namespace hl7Namespace, Element legalAuthenticator, LegalAuthenticatorInformation legalAuthenticatorInformation) {
        var assignedEntity = new Element("assignedEntity", hl7Namespace);
        CDAUtils.addId(hl7Namespace, legalAuthenticatorInformation.getIdentifier(), assignedEntity);
        CDAUtils.addAddress(hl7Namespace, legalAuthenticatorInformation.getAddress(), assignedEntity);
        CDAUtils.addTelecom(hl7Namespace, legalAuthenticatorInformation.getTelecom(), assignedEntity);
        CDAUtils.addAssignedPerson(hl7Namespace, legalAuthenticatorInformation.getAssignedPerson(), assignedEntity);
        CDAUtils.addRepresentedOrganization(hl7Namespace, legalAuthenticatorInformation.getRepresentedOrganization(), assignedEntity);
        legalAuthenticator.addContent(assignedEntity);
    }
}
