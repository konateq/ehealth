package eu.europa.ec.sante.openncp.core.server.ihe;

import eu.europa.ec.sante.openncp.core.common.fhir.context.EuRequestDetails;
import eu.europa.ec.sante.openncp.core.common.fhir.services.DispatchingService;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.InsufficientRightsException;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.DiscardDispenseDetails;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.*;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NIException;
import eu.europa.ec.sante.openncp.core.server.api.ihe.xca.DocumentSearchInterface;
import eu.europa.ec.sante.openncp.core.server.api.ihe.xcpd.PatientSearchInterface;
import eu.europa.ec.sante.openncp.core.server.api.ihe.xcpd.PatientSearchInterfaceWithDemographics;
import eu.europa.ec.sante.openncp.core.server.api.ihe.xdr.DocumentSubmitInterface;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opensaml.core.xml.io.MarshallingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * Provides a fallback mechanism to load service implementations using the deprecated {@link ServiceLoader}
 * mechanism when no corresponding Spring bean is found.
 * <p>
 * This fallback is necessary due to the transition in wave 8 from the ServiceLoader mechanism to Spring-based
 * dependency injection. Member states that have not updated their national implementations to use Spring
 * would otherwise encounter errors, such as missing bean exceptions.
 * <p>
 * If no implementation is found through the ServiceLoader, a default anonymous implementation is returned that
 * throws UnsupportedOperationException for all methods, indicating that no
 * NI implementation is available for the requested service.
 */
@Configuration
public class ServicesConfiguration {
    private final Logger LOGGER = LoggerFactory.getLogger(ServicesConfiguration.class);

    /**
     * The PatientSearchInterfaceWithDemographics should take precedence over the regular PatientSearchInterface since it is a more concrete implementation.
     * <p>
     * The bean is loaded dynamically using a {@link ServiceLoader} mechanism if no spring
     * implementation for PatientSearchInterfaceWithDemographics is found.
     * <p>
     * In case there is no bean found using the {@link ServiceLoader} then it will attempt to find a {@link PatientSearchInterface} bean using the {@link #patientSearchInterface()} method.
     *
     * @return a PatientSearchInterfaceWithDemographics implementation if available, or null otherwise.
     */
    @Bean
    public PatientSearchInterface patientSearchInterfaceWithDemographics() {
        return loadBeanViaServiceLoader(PatientSearchInterfaceWithDemographics.class)
                .map(patientSearchInterfaceWithDemographics -> (PatientSearchInterface) patientSearchInterfaceWithDemographics)
                .orElseGet(() -> {
                    LOGGER.info("No bean of PatientSearchInterfaceWithDemographics found, falling back to PatientSearchInterface beans");
                    return patientSearchInterface();
                });
    }

    /**
     * This method provides a fallback bean for the PatientSearchInterface.
     * <p>
     * If no implementation of PatientSearchInterface (including PatientSearchInterfaceWithDemographics) is found in the Spring context, this method attempts to load the interface using the
     * {@link ServiceLoader}, which follows the now deprecated wave 7 mechanism.
     * <p>
     * If no implementation is loaded, a default anonymous implementation is returned. This fallback
     * implementation throws UnsupportedOperationException for all methods, signaling that no NI
     * (National Infrastructure) implementation is available.
     * <p>
     * This design allows the system to continue functioning in the absence of a proper
     * PatientSearchInterface implementation. However, any attempt to call its methods will result in
     * an exception, clearly indicating the missing functionality.
     *
     * @return a PatientSearchInterface implementation, either dynamically loaded or a fallback
     * implementation that throws UnsupportedOperationException for all methods.
     */
    private PatientSearchInterface patientSearchInterface() {
        return loadWithFallBack(PatientSearchInterface.class, () -> Optional.of(new PatientSearchInterface() {
            @Override
            public String getPatientId(final String citizenNumber) throws NIException, InsufficientRightsException {
                throw new UnsupportedOperationException("No NI implementation found for PatientSearchInterface");
            }

            @Override
            public List<PatientDemographics> getPatientDemographics(final List<PatientId> idList) throws NIException, InsufficientRightsException, MarshallingException {
                throw new UnsupportedOperationException("No NI implementation found for PatientSearchInterface");
            }

            @Override
            public void setSOAPHeader(final Element shElement) {
                throw new UnsupportedOperationException("No NI implementation found for PatientSearchInterface");
            }
        }));
    }

    @Bean
    public DocumentSearchInterface documentSearchInterface() {
        return loadWithFallBack(DocumentSearchInterface.class, () -> Optional.of(new DocumentSearchInterface() {
            @Override
            public DocumentAssociation<PSDocumentMetaData> getPSDocumentList(final SearchCriteria searchCriteria) throws NIException, InsufficientRightsException {
                throw new UnsupportedOperationException("No NI implementation found for DocumentSearchInterface");
            }

            @Override
            public List<DocumentAssociation<EPDocumentMetaData>> getEPDocumentList(final SearchCriteria searchCriteria) throws NIException, InsufficientRightsException {
                throw new UnsupportedOperationException("No NI implementation found for DocumentSearchInterface");
            }

            @Override
            public List<OrCDDocumentMetaData> getOrCDLaboratoryResultsDocumentList(final SearchCriteria searchCriteria) throws NIException, InsufficientRightsException {
                throw new UnsupportedOperationException("No NI implementation found for DocumentSearchInterface");
            }

            @Override
            public List<OrCDDocumentMetaData> getOrCDHospitalDischargeReportsDocumentList(final SearchCriteria searchCriteria) throws NIException, InsufficientRightsException {
                throw new UnsupportedOperationException("No NI implementation found for DocumentSearchInterface");
            }

            @Override
            public List<OrCDDocumentMetaData> getOrCDMedicalImagingReportsDocumentList(final SearchCriteria searchCriteria) throws NIException, InsufficientRightsException {
                throw new UnsupportedOperationException("No NI implementation found for DocumentSearchInterface");
            }

            @Override
            public List<OrCDDocumentMetaData> getOrCDMedicalImagesDocumentList(final SearchCriteria searchCriteria) throws NIException, InsufficientRightsException {
                throw new UnsupportedOperationException("No NI implementation found for DocumentSearchInterface");
            }

            @Override
            public EPSOSDocument getDocument(final SearchCriteria searchCriteria) throws NIException, InsufficientRightsException {
                throw new UnsupportedOperationException("No NI implementation found for DocumentSearchInterface");
            }

            @Override
            public void setSOAPHeader(final Element shElement) {
                throw new UnsupportedOperationException("No NI implementation found for DocumentSearchInterface");
            }
        }));
    }

    @Bean
    public DocumentSubmitInterface documentSubmitInterface() {
        return loadWithFallBack(DocumentSubmitInterface.class, () -> Optional.of(new DocumentSubmitInterface() {

            @Override
            public void submitDispensation(final EPSOSDocument dispensationDocument) throws NIException, InsufficientRightsException {
                throw new UnsupportedOperationException("No NI implementation found for DocumentSubmitInterface");
            }

            @Override
            public void cancelDispensation(final DiscardDispenseDetails discardDispenseDetails, final EPSOSDocument dispensationToDiscard) throws NIException, InsufficientRightsException {
                throw new UnsupportedOperationException("No NI implementation found for DocumentSubmitInterface");
            }

            @Override
            public void setSOAPHeader(final Element shElement) {
                throw new UnsupportedOperationException("No NI implementation found for DocumentSubmitInterface");
            }
        }));
    }

    @Bean
    public DispatchingService dispatchingService() {
        return loadWithFallBack(DispatchingService.class, () -> Optional.of(new DispatchingService() {

            @Override
            public <T extends IBaseResource> T dispatchSearch(final EuRequestDetails requestDetails, final String JWTToken) {
                throw new UnsupportedOperationException("No NI implementation found for the FHIR DispatchingService");
            }

            @Override
            public <T extends IBaseResource> T dispatchRead(final EuRequestDetails requestDetails, final String JWTToken) {
                throw new UnsupportedOperationException("No NI implementation found for the FHIR DispatchingService");
            }
        }));
    }

    /**
     * Attempts to load a bean implementation using the deprecated {@link ServiceLoader} mechanism.
     * <p>
     * If no implementation is found via the ServiceLoader, it falls back to a default bean supplier.
     * If neither the ServiceLoader nor the default supplier provides a bean, an exception is thrown.
     *
     * @param <B>                 the type of the bean interface
     * @param beanInterface       the class type of the bean interface to be loaded
     * @param defaultBeanSupplier a supplier for the default bean implementation, used as a fallback
     * @return the loaded bean implementation, or the result of the fallback supplier if no bean is found
     * @throws RuntimeException if no implementation is found from either the ServiceLoader or the fallback supplier
     */
    private <B> B loadWithFallBack(final Class<B> beanInterface, final Supplier<Optional<B>> defaultBeanSupplier) {
        return loadBeanViaServiceLoader(beanInterface)
                .or(() -> {
                    LOGGER.warn("No bean of [{}] found through the Service Loader mechanism, falling back to the default bean supplier.", beanInterface.getSimpleName());
                    return defaultBeanSupplier.get();
                })
                .orElseThrow(() -> new RuntimeException(String.format("No bean implementation found for interface [%s]", beanInterface.getSimpleName())));
    }

    private static <B> Optional<B> loadBeanViaServiceLoader(final Class<B> beanInterface) {
        final ServiceLoader<B> serviceLoader = ServiceLoader.load(beanInterface);
        return serviceLoader.findFirst();
    }
}
