package eu.europa.ec.sante.openncp.core.server.ihe.xca;

import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.ClassificationType;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.ObjectFactory;

import java.util.UUID;

public class ClassificationBuilder {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    public static ClassificationType build(final String classificationScheme, final String classifiedObject, final String nodeRepresentation, final String value, final String name) {

        final var classificationType = build(classificationScheme, classifiedObject, nodeRepresentation);
        classificationType.getSlots().add(SlotBuilder.build("codingScheme", value));

        classificationType.setName(OBJECT_FACTORY.createInternationalStringType());
        classificationType.getName().getLocalizedStrings().add(OBJECT_FACTORY.createLocalizedString());
        classificationType.getName().getLocalizedStrings().get(0).setValue(name);
        return classificationType;
    }

    public static ClassificationType build(final String classificationScheme, final String classifiedObject, final String nodeRepresentation) {

        final String uuid = Constants.UUID_PREFIX + UUID.randomUUID();
        final var classificationType = OBJECT_FACTORY.createClassificationType();
        classificationType.setId(uuid);
        classificationType.setNodeRepresentation(nodeRepresentation);
        classificationType.setClassificationScheme(classificationScheme);
        classificationType.setClassifiedObject(classifiedObject);
        return classificationType;
    }
}
