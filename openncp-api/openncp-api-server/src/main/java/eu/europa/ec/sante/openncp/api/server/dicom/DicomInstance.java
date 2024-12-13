package eu.europa.ec.sante.openncp.api.server.dicom;

import eu.europa.ec.sante.openncp.common.immutables.Domain;

@Domain
public interface DicomInstance {

    String getUID();
}
