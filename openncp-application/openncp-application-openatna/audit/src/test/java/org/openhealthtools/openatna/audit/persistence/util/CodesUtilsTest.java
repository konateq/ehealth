package org.openhealthtools.openatna.audit.persistence.util;

import org.junit.jupiter.api.Test;
import org.openhealthtools.openatna.audit.persistence.model.codes.CodeEntity;
import org.openhealthtools.openatna.audit.persistence.model.codes.ParticipantCodeEntity;

import static org.assertj.core.api.Assertions.*;

public class CodesUtilsTest {
    @Test
    void testEquivalenceDoesNotThrowNPE() {
        // CodeEntities with null codes
        CodeEntity codeEntity1 = new ParticipantCodeEntity();
        CodeEntity codeEntity2 = new ParticipantCodeEntity();

        assertThat(codeEntity1.getCode()).isEqualTo(null);
        assertThat(codeEntity2.getCode()).isEqualTo(null);

        assertThat(CodesUtils.equivalent(codeEntity1,codeEntity2)).isEqualTo(true);
    }
}
