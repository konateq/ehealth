package eu.europa.ec.sante.openncp.core.server.ihe.xca;


import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.ObjectFactory;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.Slot;

import java.util.List;

public class SlotBuilder {

    private static final eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    public static Slot build(final String name, final String value) {

        final var slotType1 = OBJECT_FACTORY.createSlot();
        slotType1.setName(name);
        slotType1.setValueList(OBJECT_FACTORY.createValueList());
        slotType1.getValueList().getValues().add(value);
        return slotType1;
    }

    public static Slot build(final String name, final List<String> values) {

        final var slotType1 = OBJECT_FACTORY.createSlot();
        slotType1.setName(name);
        slotType1.setValueList(OBJECT_FACTORY.createValueList());
        values.forEach(value -> slotType1.getValueList().getValues().add(value));
        return slotType1;
    }
}
