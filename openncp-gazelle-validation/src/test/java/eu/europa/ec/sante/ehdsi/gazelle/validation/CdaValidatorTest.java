package eu.europa.ec.sante.ehdsi.gazelle.validation;

import eu.epsos.validation.datamodel.common.NcpSide;
import eu.europa.ec.sante.ehdsi.gazelle.config.TestConfiguration;
import eu.europa.ec.sante.ehdsi.gazelle.validation.impl.CdaValidatorImpl;
import eu.europa.ec.sante.ehdsi.gazelle.validation.impl.DefaultGazelleValidatorFactory;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class, loader = AnnotationConfigContextLoader.class)
@Ignore
public class CdaValidatorTest {

    @Autowired
    private HttpClient httpClient;

    private CdaValidator cdaValidator;

    @Before
    public void setUp() {

        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("net.ihe.gazelle.jaxb.cda.sante");

        WebServiceTemplate webServiceTemplate = new WebServiceTemplate(marshaller);
        webServiceTemplate.setDefaultUri(DefaultGazelleValidatorFactory.GAZELLE_CDA_VALIDATOR_URI);
        webServiceTemplate.setMessageSender(new HttpComponentsMessageSender(httpClient));
        cdaValidator = new CdaValidatorImpl(webServiceTemplate);
    }

    @Test
    public void testValidateDocument() throws IOException {
        String result = cdaValidator.validateDocument(IOUtils.toString(new ClassPathResource("/cda/2-4567.xml").getInputStream(),
                StandardCharsets.UTF_8), "eHDSI - ART-DECOR based CDA validation (PIVOT)", NcpSide.NCP_B);
        Assert.assertNotNull(result);
    }
}
