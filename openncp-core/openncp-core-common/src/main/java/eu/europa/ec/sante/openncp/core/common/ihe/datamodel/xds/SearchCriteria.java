package eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds;

import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId;
import org.w3c.dom.Document;

import java.util.Iterator;

/**
 * SearchCriteria class will be used for XCAService interface to allow search of documents with different criteria.
 * <p>
 * The class can be used like:
 * DocumentFactory.createSearchCriteria(Criteria.PATIENT_ID, "PatientId");
 * or
 * DocumentFactory.createSearchCriteria().add(Criteria.PATIENT_ID, "PatientId").add(Criteria.RepositoryId, "REPOSITORY_ID");
 */
public interface SearchCriteria {

	SearchCriteria add(Criteria c, String value);

	SearchCriteria addPatientId(String patientId);

	String getCriteriaValue(Criteria c);

	PatientId getPatientId();

	Iterator<Criteria> getSearchCriteriaKeys();

	Document asXml();

	enum Criteria {
		PATIENT_ID("PatientId"),
		REPOSITORY_ID("RepositoryId"),
		DOCUMENT_ID("DocumentId"),
		MAXIMUM_SIZE("MaximumSize"),
		CREATED_BEFORE("CreatedBefore"),
		CREATED_AFTER("CreatedAfter");

		public final String value;

		Criteria(final String value) {
			this.value = value;
		}
	}
}
