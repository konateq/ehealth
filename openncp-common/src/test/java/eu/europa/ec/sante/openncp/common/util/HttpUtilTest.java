package eu.europa.ec.sante.openncp.common.util;

import org.junit.Assert;
import org.junit.Test;

public class HttpUtilTest {

    @Test
    public void testGetCommonNameFromServerCertificate() {
        Assert.assertEquals("www.google.com", HttpUtil.getCommonNameFromServerCertificate("https://www.google.com"));
    }
}
