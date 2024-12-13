package eu.europa.ec.sante.openncp.api.server.dicom;

import eu.europa.ec.sante.openncp.common.immutables.Domain;

import java.util.List;

@Domain
public interface DicomSeries {

    String getUID();

    List<DicomInstance> getInstances();
}
