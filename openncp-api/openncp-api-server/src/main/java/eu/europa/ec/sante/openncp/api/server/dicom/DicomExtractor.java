package eu.europa.ec.sante.openncp.api.server.dicom;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class DicomExtractor {

    public DicomStudy extractDicomStudy(InputStream inputStream) throws IOException {
        final DicomInputStream dis = new DicomInputStream(inputStream);
        final Attributes dcmObj = dis.readDataset(-1, -1);

        final String studyInstanceUID = dcmObj.getString(Tag.StudyInstanceUID);
        final String modality = dcmObj.getString(Tag.Modality);
        final Date studyDate = dcmObj.getDate(Tag.StudyDate);
        final Date studyTime = dcmObj.getDate(Tag.StudyTime);
        final String studyDescription = dcmObj.getString(Tag.StudyDescription);
        final String accessionNumber = dcmObj.getString(Tag.AccessionNumber);
        final Attributes currentRequestedProcedureEvidenceSequence = dcmObj.getNestedDataset(Tag.CurrentRequestedProcedureEvidenceSequence);
        List<DicomSeries> dicomSeries = new ArrayList<>();
        final Sequence referenceSeriesSequence = currentRequestedProcedureEvidenceSequence.getSequence(Tag.ReferencedSeriesSequence);
        for (Attributes series: referenceSeriesSequence) {
            dicomSeries.add(extractDicomSeries(series));
        }

        final DicomStudy dicomStudy = ImmutableDicomStudy.builder()
                .uID(studyInstanceUID)
                .modality(modality)
                .studyDate(studyDate)
                .studyTime(studyTime)
                .studyDescription(studyDescription)
                .accessionNumber(accessionNumber)
                .addAllSeries(dicomSeries)
                .build();
        return dicomStudy;
    }

    private DicomSeries extractDicomSeries(Attributes attributes) {
        final String seriesInstanceUID = attributes.getString(Tag.SeriesInstanceUID);
        final String description = attributes.getString(Tag.SeriesDescription);
        final String modality = attributes.getString(Tag.Modality);
        final String endPoint = attributes.getString(Tag.RetrieveURL);
        final String number = attributes.getString(Tag.SeriesNumber);
        List<DicomInstance> dicomInstances = new ArrayList<>();
        final Sequence referencedSOPSequence = attributes.getSequence(Tag.ReferencedSOPSequence);
        for (Attributes instance: referencedSOPSequence) {
            dicomInstances.add(extractDicomInstance(instance));
        }
        DicomSeries dicomSeries = ImmutableDicomSeries.builder()
                .uID(seriesInstanceUID)
                .description(description)
                .modality(modality)
                .endpoint(endPoint)
                .addAllInstances(dicomInstances)
                .build();
        return dicomSeries;
    }

    private DicomInstance extractDicomInstance(Attributes attributes) {
        DicomInstance dicomInstance = ImmutableDicomInstance.builder()
                .uID(attributes.getString(Tag.ReferencedSOPInstanceUID))
                .classUID(attributes.getString(Tag.ReferencedSOPClassUID))
                .build();
        return dicomInstance;
    }
}
