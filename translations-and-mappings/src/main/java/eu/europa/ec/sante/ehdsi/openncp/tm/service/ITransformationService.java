package eu.europa.ec.sante.ehdsi.openncp.tm.service;

import eu.europa.ec.sante.ehdsi.openncp.tm.domain.TMResponseStructure;
import org.hl7.fhir.r4.model.ValueSet;
import org.w3c.dom.Document;

public interface ITransformationService {

    /**
     * After having received a translate() request, this component starts to
     * process the received EpSOSosCDA in order extract the epSOS coded
     * concepts. Subsequently, for each coded concept found, it makes use of the
     * TSAM capabilities to obtain the representation of that concept in the
     * target TargetLanguageCode identifier . This information is therefore used
     * by this component to update the displayName attribute of that coded
     * entry. After the completion of this translation phase, an epSOS pivot
     * document with translated concepts is obtained. This document is therefore
     * returned to the requesting party. No changes are applied to the document
     * identifiers.<br>
     * <i>Exceptions:</i> in case of processing error or warning, the
     * responseStatusStructure will be used to convey this information to the
     * calling component with an appropriated error and warning code. A detailed
     * list of the managed exceptions will be provided in the Detail Design
     * Specification document. Each exception condition occurred will be logged
     * (standard and audit), reporting both the exception code and its English
     * description.
     * <br><br>
     * <b>Algorithm:</b>
     * <li>1. Schema based validation of input CDA document. (Can be
     * enabled/disabled through property <i>tm.schema.validation.enabled</i>)</li>
     * <li>2. Checking document type.(Allowed/expected document types are
     * configured through <i>tm.documenttype.patientsummary ;
     * tm.documenttype.eprescription ; tm.documenttype.edispensation</i>
     * properties) <br>
     * Checking if document is structured or unstructured. (Level 3 or Level 1
     * CDA document)</li> <li>3. Schematron based validation of input CDA
     * document. (Can be enabled/disabled through property
     * <i>tm.schematron.validation.enabled)</i></li> <li>4. Document processing
     * - translation.<br>
     * (During the processing are selected Coded Elements either acording
     * CodedElementList or simply all elements with code and codeSystem
     * attributes. This can be configured through property
     * <i>tm.codedelementlist.enabled</i>)</li>
     * <li>5. Schematron based validation of output CDA
     * document. (Can be enabled/disabled through property
     * <i>tm.schematron.validation.enabled)</i></li>
     *
     * @param pivotCDA           Document in epSOS pivot format (with eHDSI codes )
     * @param targetLanguageCode Identifier (code) of the target language.
     * @return TranslatedEpSOSCDA - response structure including the epSOS pivot
     * CDA with translated epSOS codes into the consumer country
     * language and the response status structure. The response status
     * structure provides information about the operation results,
     * including possible errors and warning.
     */
    TMResponseStructure translate(Document pivotCDA, String targetLanguageCode);

    /**
     * After having received a transcode() request, this component takes the EpSOSOriginalData
     * (already compliant to epSOS CDA syntax) and using the TSAM capabilities, accomplishes the eventual
     * transcoding of the terms present in the epSOS value sets, while also keeping the original codes and display name.
     * An epSOS pivot document with epSOS coded concepts is therefore produced. <br>
     * <i>Exceptions:</i> in case of processing error or warning, the responseStatusStructure will be used to convey
     * this information to the calling component with an appropriated error and warning code.
     * A detailed list of the managed exceptions will be provided in the Detail Design Specification document.
     * Each exception condition occurred will be logged (standard and audit), reporting both the exception code and
     * its English description.
     * <br><br>
     * <b>Algorithm:</b>
     * <li>1. Schema based validation of input CDA document. (Can be enabled/disabled through property
     * <i>tm.schema.validation.enabled</i>)</li>
     * <li>2. Checking document type.(Allowed/expected document types are configured through <i>tm.documenttype.patientsummary ;
     * tm.documenttype.eprescription ; tm.documenttype.edispensation</i> properties) <br>
     * Checking if document is structured or unstructured. (Level 3 or Level 1 CDA document)</li> <li>3.
     * Schematron based validation of input CDA document. (Can be enabled/disabled through property <i>tm.schematron.validation.enabled)</i></li> <li>4.
     * Document processing - transcoding.<br>
     * (During the processing are selected Coded Elements either acording CodedElementList or simply all elements with code and codeSystem attributes.
     * This can be configured through property <i>tm.codedelementlist.enabled</i>)</li>
     * <li>5. Schematron based validation of output CDA
     * document. (Can be enabled/disabled through property
     * <i>tm.schematron.validation.enabled)</i></li>
     *
     * @param friendlyCDA Medical document in its original data format as provided from the NationalConnector to
     *                          this component. The provided document is compliant with the epSOS pivot CDA (see D 3.5.2 Appendix C)
     *                          unless the adoption of the element binding with the epSOS reference Value Sets. [Mandatory]
     * @return - EpSOSCDA structure - Response structure including the epSOS
     * pivot CDA and the response status structure. The response status
     * structure provides information about the operation results,
     * including possible errors and warning.
     */
    TMResponseStructure transcode(Document friendlyCDA);

    ValueSet translateValueSet(String oid, String targetLanguage);
}
