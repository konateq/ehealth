package eu.europa.ec.sante.openncp.core.common.fhir.security;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import net.shibboleth.utilities.java.support.xml.XMLParserException;
import org.apache.commons.lang3.Validate;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.Optional;

@Domain
public interface ClaimDetails {
    AssertionType getAssertionType();

    Claim getClaim();

    Element getElement();

    Assertion getAssertion();

    static Optional<ClaimDetails> of(final AssertionType assertionType, final DecodedJWT jwt) {
        Validate.notNull(assertionType, "assertionType must not be null");
        Validate.notNull(jwt, "Decoded JWT token must not be null");

        final Claim claim = jwt.getClaim(assertionType.name());
        if (claim == null || claim.isNull()) {
            return Optional.empty();
        }

        final Base64.Decoder decoder = Base64.getDecoder();

        final String claimAsString = claim.asString();
        final String decodedSaml = new String(decoder.decode(claimAsString));

        final Element samlElement;
        try {
            final BasicParserPool ppMgr = new BasicParserPool();
            ppMgr.setNamespaceAware(true);
            if (!ppMgr.isInitialized()) {
                ppMgr.initialize();
            }
            final InputStream in = new ByteArrayInputStream(decodedSaml.getBytes());
            final Document samlas;
            samlas = ppMgr.parse(in);
            samlElement = samlas.getDocumentElement();
        } catch (final XMLParserException | ComponentInitializationException ex) {
            throw new AuthenticationException(Msg.code(333) + ex.getMessage());
        }

        final Assertion assertion;
        try {
            final UnmarshallerFactory unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
            final Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(samlElement);
            assertion = (Assertion) unmarshaller.unmarshall(samlElement);
        } catch (final UnmarshallingException ex) {
            throw new AuthenticationException(Msg.code(333) + ex.getMessage());
        }

        return Optional.of(ImmutableClaimDetails.builder()
                .assertionType(assertionType)
                .claim(claim)
                .element(samlElement)
                .assertion(assertion)
                .build());
    }
}
