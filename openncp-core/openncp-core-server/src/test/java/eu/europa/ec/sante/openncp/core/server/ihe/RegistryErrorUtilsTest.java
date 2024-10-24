package eu.europa.ec.sante.openncp.core.server.ihe;


import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.core.common.ihe.RegistryErrorSeverity;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.RegistryError;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.RegistryErrorList;
import org.junit.Assert;
import org.junit.Test;

import java.rmi.registry.Registry;

public class RegistryErrorUtilsTest {

    @Test
    public void testAddErrorMessage() {
        try {
            String nullString = null;
            nullString.length();
        } catch(Exception e) {
            final RegistryErrorList registryErrorList = new RegistryErrorList();
            final OpenNCPErrorCode ERROR_GENERIC = OpenNCPErrorCode.ERROR_GENERIC;
            final String codeContext = "It isn't possible to call the length() method on a null object";
            final RegistryErrorSeverity registryErrorSeverity = RegistryErrorSeverity.ERROR_SEVERITY_ERROR;
            RegistryErrorUtils.addErrorMessage(registryErrorList, ERROR_GENERIC, codeContext, e, registryErrorSeverity);
            Assert.assertEquals(1, registryErrorList.getRegistryError().size());
            final RegistryError registryError = registryErrorList.getRegistryError().get(0);
            Assert.assertEquals(ERROR_GENERIC.getCode(), registryError.getErrorCode());
            Assert.assertNotNull(registryError.getLocation());
            Assert.assertEquals(e.getMessage(), registryError.getLocation());
            Assert.assertEquals(codeContext, registryError.getCodeContext());
            Assert.assertEquals(registryErrorSeverity.getText(), registryError.getSeverity());
        }
    }
}
