package eu.europa.ec.sante.openncp.core.server.api.ihe.xcpd;

import eu.europa.ec.sante.openncp.core.common.ihe.NationalConnectorInterface;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.InsufficientRightsException;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NIException;
import org.opensaml.core.xml.io.MarshallingException;

import java.util.List;

/**
 * This interface describes the National Connector API regarding Patient Identification Service.
 *
 * @author Konstantin Hypponen<code> - Konstantin.Hypponen@kela.fi</code>
 * @author Luís Pinto<code> - luis.pinto@iuz.pt</code>
 */
public interface PatientSearchInterface extends NationalConnectorInterface {

    /**
     * Translates a National citizen number in an eHDSI id.
     *
     * @param citizenNumber a valid citizen identifier
     * @return the citizen eHDSI identifier
     */
    String getPatientId(String citizenNumber) throws NIException, InsufficientRightsException;

    /**
     * Searches the NI for all the patients that relates to the given <code>idList</code>.
     *
     * @param idList A set of patient's eHDSI identifiers
     * @return A set of patient demographics
     */
    List<PatientDemographics> getPatientDemographics(List<PatientId> idList) throws NIException, InsufficientRightsException, MarshallingException;
}
