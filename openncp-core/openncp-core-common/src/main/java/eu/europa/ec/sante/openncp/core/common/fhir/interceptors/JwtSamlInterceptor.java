package eu.europa.ec.sante.openncp.core.common.fhir.interceptors;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;
import com.auth0.jwt.interfaces.DecodedJWT;
import eu.europa.ec.sante.openncp.common.security.exception.SMgrException;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditSecurityInfo;
import eu.europa.ec.sante.openncp.core.common.fhir.context.JwtToken;
import eu.europa.ec.sante.openncp.core.common.fhir.security.TokenProvider;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.InsufficientRightsException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.InvalidFieldException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.MissingFieldException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.XSDValidationException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.saml.SAML2Validator;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import net.shibboleth.utilities.java.support.xml.XMLParserException;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.Optional;

public class JwtSamlInterceptor extends InterceptorAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtSamlInterceptor.class);

    private final TokenProvider tokenProvider;

    private final SAML2Validator saml2Validator;

    public JwtSamlInterceptor(final TokenProvider tokenProvider, final SAML2Validator saml2Validator) {
        this.tokenProvider = tokenProvider;
        this.saml2Validator = saml2Validator;
    }

    @Override
    public boolean incomingRequestPreProcessed(final HttpServletRequest theRequest, final HttpServletResponse theResponse) {
        LOGGER.info("Validating the incoming JWT bearer token.");
        final Optional<JwtToken> jwtToken = JwtToken.extractFrom(theRequest);
        if (jwtToken.isEmpty()) {
            throw new AuthenticationException("A bearer token is mandatory to initiate a request.");
        }

        final DecodedJWT jwt = tokenProvider.verifyToken(jwtToken.get().getToken());
        final String saml = jwt.getClaim("saml").asString();

        final Base64.Decoder decoder = Base64.getDecoder();
        final AuditSecurityInfo auditSecurityInfo;
        try {
            auditSecurityInfo = validateSaml(new String(decoder.decode(saml)));
        } catch (final Exception e) {
            LOGGER.error("Invalid SAML token", e);
            throw new AuthenticationException("Invalid SAML token.");
        }

        if (auditSecurityInfo != null) {
            addAssertionToSecurityContext(AuditSecurityInfo.from(auditSecurityInfo.getAssertion(), auditSecurityInfo.getSamlAsRoot()));
        } else {
            throw new AuthenticationException("Invalid SAML token: empty assertion.");
        }

        return true;
    }


    private AuditSecurityInfo validateSaml(final String saml) throws AuthenticationException, InitializationException {

        Assertion hcpIdentityAssertion = null;

        org.opensaml.core.config.InitializationService.initialize();

        LOGGER.info("SAML token: {}", saml);

        if (saml != null && !saml.isEmpty()) {
            try {
                final BasicParserPool ppMgr = new BasicParserPool();
                ppMgr.setNamespaceAware(true);
                if (!ppMgr.isInitialized()) {
                    ppMgr.initialize();
                }

                final InputStream in = new ByteArrayInputStream(saml.getBytes());
                Document samlas = null;
                samlas = ppMgr.parse(in);
                final Element samlasRoot = samlas.getDocumentElement();
                // Get apropriate unmarshaller

                final UnmarshallerFactory unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
                final Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(samlasRoot);
                // Unmarshall using the document root element, an EntitiesDescriptor in this case
                hcpIdentityAssertion = (Assertion) unmarshaller.unmarshall(samlasRoot);

                saml2Validator.validateXCPDHeader(hcpIdentityAssertion);

                return AuditSecurityInfo.from(hcpIdentityAssertion, samlasRoot);

            } catch (final UnmarshallingException | XMLParserException | ComponentInitializationException ex) {
                throw new AuthenticationException(Msg.code(333) + ex.getMessage());
            } catch (final MissingFieldException e) {
                throw new RuntimeException(e);
            } catch (final InsufficientRightsException e) {
                throw new RuntimeException(e);
            } catch (final InvalidFieldException e) {
                throw new RuntimeException(e);
            } catch (final SMgrException e) {
                throw new RuntimeException(e);
            } catch (final XSDValidationException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public void addAssertionToSecurityContext(final AuditSecurityInfo auditSecurityInfo) {
        final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(auditSecurityInfo.getAssertion().getSubject().getNameID().getValue(), auditSecurityInfo.getAssertion().getIssuer().getValue(), null);
        authentication.setDetails(auditSecurityInfo);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
