package eu.europa.ec.sante.openncp.api.server.dicom;

import eu.europa.ec.sante.openncp.common.immutables.Domain;

import java.util.Date;
import java.util.List;

@Domain
public interface DicomStudy {

    String getUID();

    String getModality();

    Date getStudyDate();

    Date getStudyTime();

    String getStudyDescription();

    String getAccessionNumber();

    List<DicomSeries> getSeries();


}
