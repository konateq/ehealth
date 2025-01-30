package eu.europa.ec.sante.openncp.core.common.fhir.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SimpleServerLoggingInterceptor implements FhirCustomInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleServerLoggingInterceptor.class);


    @Hook(Pointcut.INTERCEPTOR_REGISTERED)
    public void interceptInterceptorRegistered() {
        LOGGER.info("Pointcut [INTERCEPTOR_REGISTERED]");
    }

    @Hook(Pointcut.CLIENT_REQUEST)
    public void interceptClientRequest(final ca.uhn.fhir.rest.client.api.IHttpRequest httpRequest, final ca.uhn.fhir.rest.client.api.IRestfulClient restfulClient) {
        LOGGER.info("Pointcut [CLIENT_REQUEST] with params:  IHttpRequest={}, IRestfulClient={}", httpRequest, restfulClient);
    }

    @Hook(Pointcut.CLIENT_RESPONSE)
    public void interceptClientResponse(final ca.uhn.fhir.rest.client.api.IHttpRequest httpRequest, final ca.uhn.fhir.rest.client.api.IHttpResponse httpResponse, final ca.uhn.fhir.rest.client.api.IRestfulClient restfulClient) {
        LOGGER.info("Pointcut [CLIENT_RESPONSE] with params:  IHttpRequest={}, IHttpResponse={}, IRestfulClient={}", httpRequest, httpResponse, restfulClient);
    }

    @Hook(Pointcut.SERVER_CAPABILITY_STATEMENT_GENERATED)
    public void interceptServerCapabilityStatementGenerated(final org.hl7.fhir.instance.model.api.IBaseConformance conformance, final ca.uhn.fhir.rest.api.server.RequestDetails requestDetails, final ca.uhn.fhir.rest.server.servlet.ServletRequestDetails servletRequestDetails) {
        LOGGER.info("Pointcut [SERVER_CAPABILITY_STATEMENT_GENERATED] with params:  IBaseConformance={}, RequestDetails={}, ServletRequestDetails={}", conformance, requestDetails, servletRequestDetails);
    }

    @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED)
    public void interceptStoragePrestorageResourceCreated(final RequestDetails theRequestDetails, final IBaseResource theResource) {
        LOGGER.info("Pointcut [STORAGE_PRESTORAGE_RESOURCE_CREATED] with params:  RequestDetails={}, IBaseResource={}", theRequestDetails, theResource);
    }

    @Hook(Pointcut.STORAGE_PRECOMMIT_RESOURCE_CREATED)
    public void interceptStoragePrecommitResourceCreated(final RequestDetails theRequestDetails, final IBaseResource theResource) {
        LOGGER.info("Pointcut [STORAGE_PRECOMMIT_RESOURCE_CREATED] with params:  RequestDetails={}, IBaseResource={}", theRequestDetails, theResource);
    }

    @Hook(Pointcut.STORAGE_PRE_DELETE_EXPUNGE)
    public void interceptStoragePreDeleteExpunge(final RequestDetails requestDetails, final IBaseResource resource) {
        LOGGER.info("Pointcut [STORAGE_PRE_DELETE_EXPUNGE] with params:  RequestDetails={}, IBaseResource={}", requestDetails, resource);
    }

    @Hook(Pointcut.STORAGE_PRESTORAGE_EXPUNGE_RESOURCE)
    public void interceptStoragePrestorageExpungeResource(final AtomicInteger counter, final IBaseResource resource) {
        LOGGER.info("Pointcut [STORAGE_PRESTORAGE_EXPUNGE_RESOURCE] with params:  AtomicInteger={}, IBaseResource={}", counter, resource);
    }

    @Hook(Pointcut.STORAGE_PRESHOW_RESOURCES)
    public void interceptStoragePreshowResources(final RequestDetails requestDetails, final IBaseResource resource) {
        LOGGER.info("Pointcut [STORAGE_PRESHOW_RESOURCES] with params:  RequestDetails={}, IBaseResource={}", requestDetails, resource);
    }

    @Hook(Pointcut.SERVER_OUTGOING_WRITER_CREATED)
    public void interceptServerOutgoingWriterCreated(final RequestDetails requestDetails, final IBaseResource resource) {
        LOGGER.info("Pointcut [SERVER_OUTGOING_WRITER_CREATED] with params:  RequestDetails={}, IBaseResource={}", requestDetails, resource);
    }


//    @Hook(Pointcut.STORAGE_INITIATE_BULK_EXPORT)
//    public void interceptStorageInitiateBulkExport(final BulkExportJobParameters jobParameters, final RequestDetails requestDetails, final ServletRequestDetails servletRequestDetails) {
//        LOGGER.info("Pointcut [STORAGE_INITIATE_BULK_EXPORT] with params:  BulkExportJobParameters={}, RequestDetails={}, ServletRequestDetails={}", jobParameters, requestDetails, servletRequestDetails);
//    }

//    @Hook(Pointcut.STORAGE_BULK_EXPORT_RESOURCE_INCLUSION)
//    public void interceptStorageBulkExportResourceInclusion(final BulkExportJobParameters jobParameters, final IBaseResource resource) {
//        LOGGER.info("Pointcut [STORAGE_BULK_EXPORT_RESOURCE_INCLUSION] with params:  BulkExportJobParameters={}, IBaseResource={}", jobParameters, resource);
//    }
//
//    @Hook(Pointcut.STORAGE_PRE_DELETE_EXPUNGE_PID_LIST)
//    public void interceptStoragePreDeleteExpungePidList(final String id, final List<String> pids, final AtomicLong counter, final RequestDetails requestDetails, final ServletRequestDetails servletRequestDetails) {
//        LOGGER.info("Pointcut [STORAGE_PRE_DELETE_EXPUNGE_PID_LIST] with params:  String={}, List={}, AtomicLong={}, RequestDetails={}, ServletRequestDetails={}", id, pids, counter, requestDetails, servletRequestDetails);
//    }

//    @Hook(Pointcut.STORAGE_PREACCESS_RESOURCES)
//    public void interceptStoragePreaccessResources(final IPreResourceAccessDetails accessDetails, final RequestDetails requestDetails, final ServletRequestDetails servletRequestDetails) {
//        LOGGER.info("Pointcut [STORAGE_PREACCESS_RESOURCES] with params:  IPreResourceAccessDetails={}, RequestDetails={}, ServletRequestDetails={}", accessDetails, requestDetails, servletRequestDetails);
//    }

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
    public void interceptServerIncomingRequestPreHandled(final RequestDetails theRequestDetails, final ServletRequestDetails servletRequestDetails, final RestOperationTypeEnum restOperationTypeEnum) {
        LOGGER.info("Pointcut [SERVER_INCOMING_REQUEST_PRE_HANDLED] with params:  RequestDetails={}, ServletRequestDetails={}, RestOperationTypeEnum={}", theRequestDetails, servletRequestDetails, restOperationTypeEnum);
    }

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
    public void interceptServerIncomingRequestPostProcessed(final RequestDetails theRequestDetails, final ServletRequestDetails servletRequestDetails, final HttpServletRequest theRequest, final HttpServletResponse theResponse) {
        LOGGER.info("Pointcut [SERVER_INCOMING_REQUEST_POST_PROCESSED] with params:  RequestDetails={}, ServletRequestDetails={}, HttpServletRequest={}, HttpServletResponse={}", theRequestDetails, servletRequestDetails, theRequest, theResponse);
    }

    @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
    public void interceptServerOutgoingResponse(final RequestDetails theRequestDetails, final IBaseResource theResponseObject) {
        LOGGER.info("Pointcut [SERVER_OUTGOING_RESPONSE] with params:  RequestDetails={}, IBaseResource={}", theRequestDetails, theResponseObject);
    }

    @Hook(Pointcut.SERVER_PROCESSING_COMPLETED_NORMALLY)
    public void interceptServerProcessingCompletedNormally(final RequestDetails theRequestDetails, final ServletRequestDetails servletRequestDetails) {
        LOGGER.info("Pointcut [SERVER_PROCESSING_COMPLETED_NORMALLY] with params:  RequestDetails={}, ServletRequestDetails={}", theRequestDetails, servletRequestDetails);
    }

    @Hook(Pointcut.SERVER_PROCESSING_COMPLETED)
    public void interceptServerProcessingCompleted(final RequestDetails theRequestDetails, final ServletRequestDetails servletRequestDetails) {
        LOGGER.info("Pointcut [SERVER_PROCESSING_COMPLETED] with params:  RequestDetails={}, ServletRequestDetails={}", theRequestDetails, servletRequestDetails);
    }


}
