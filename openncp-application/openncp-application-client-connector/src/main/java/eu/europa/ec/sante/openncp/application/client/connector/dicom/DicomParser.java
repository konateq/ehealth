package eu.europa.ec.sante.openncp.application.client.connector.dicom;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class DicomParser {

    public static Map<Integer, String> extractAttributeValues(File file) throws IOException {
        Map<Integer, String> attributes = new HashMap<>();
        DicomInputStream dis = new DicomInputStream(file);
        Attributes dcmObj = dis.readDataset(-1, -1);
        attributes.put(Tag.StudyInstanceUID, dcmObj.getString(Tag.StudyInstanceUID));
        attributes.put(Tag.SeriesInstanceUID, dcmObj.getString(Tag.SeriesInstanceUID));
        attributes.put(Tag.ReferencedSOPInstanceUID, dcmObj.getString(Tag.SOPInstanceUID));
        return attributes;
    }
}
