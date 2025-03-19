package eu.europa.ec.sante.openncp.core.server.nc.mock.xdr.impl;

import eu.europa.ec.sante.openncp.common.configuration.util.OpenNCPConstants;
import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.common.util.XMLUtil;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.DiscardDispenseDetails;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.EPSOSDocument;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NIException;
import eu.europa.ec.sante.openncp.core.server.api.ihe.exception.NationalInfrastructureException;
import eu.europa.ec.sante.openncp.core.server.api.ihe.xdr.DocumentSubmitInterface;
import eu.europa.ec.sante.openncp.core.server.nc.mock.common.NationalConnectorGateway;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.xml.transform.TransformerException;

/**
 * Mock implementation of the DocumentSubmitInterface, to be replaced nationally.
 */
@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DocumentSubmitMockImpl extends NationalConnectorGateway implements DocumentSubmitInterface {

    private final Logger logger = LoggerFactory.getLogger(DocumentSubmitMockImpl.class);
    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");

    /**
     * Stores a dispensation in the national infrastructure
     *
     * @param dispensationDocument eDispensation document in epSOS pivot (CDA) form
     */
    @Override
    public void submitDispensation(EPSOSDocument dispensationDocument) throws NIException {

        if (logger.isInfoEnabled()) {
            logger.info("[National Infrastructure Mock] Submit Dispense Document for Patient: '{}'", dispensationDocument.getPatientId());
        }
        String dispensation;
        try {
            dispensation = XMLUtil.prettyPrint(dispensationDocument.getDocument().getFirstChild());
        } catch (TransformerException e) {
            logger.error("TransformerException while submitDispensation(): '{}'", e.getMessage(), e);
            throw new NationalInfrastructureException(OpenNCPErrorCode.ERROR_EP_ALREADY_DISPENSED);
        }
        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
            loggerClinical.debug("eDispensation document content: '{}'", dispensation);
        }

        if (StringUtils.isEmpty(dispensation)) {
            throw new NationalInfrastructureException(OpenNCPErrorCode.ERROR_EP_ALREADY_DISPENSED);
        }

        if (StringUtils.contains(dispensation, "NO_MATCHING_EP")) {

            logger.error("Tried to submit dispensation with no matching ePrescription.");
            throw new NationalInfrastructureException(OpenNCPErrorCode.ERROR_EP_NOT_MATCHING);
        }

        if (StringUtils.contains(dispensation, "INVALID_DISPENSE")) {

            logger.error("Tried to submit already dispensed ePrescription.");
            throw new NationalInfrastructureException(OpenNCPErrorCode.ERROR_EP_ALREADY_DISPENSED);
        }
    }

    /**
     * Discards a previously submitted dispensation
     *
     * @param dispensationToDiscard Id of the dispensation to be discarded
     */
    @Override
    public void cancelDispensation(DiscardDispenseDetails discardDispenseDetails, EPSOSDocument dispensationToDiscard) {

        if (logger.isInfoEnabled()) {
            logger.info("[National Infrastructure Mock] Submit Discard Dispense Document");
        }
        logger.info("eDispensation to be discarded: '{}' for Patient: '{}'", dispensationToDiscard.getClassCode(), dispensationToDiscard.getPatientId());
        logger.info("Discard Dispense ID: '{}' for ePrescription ID: '{}' operation executed...\n'{}'",
                discardDispenseDetails.getDiscardId(), discardDispenseDetails.getDispenseId(), discardDispenseDetails);
    }
}
