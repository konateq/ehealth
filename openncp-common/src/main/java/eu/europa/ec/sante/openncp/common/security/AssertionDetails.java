package eu.europa.ec.sante.openncp.common.security;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import eu.europa.ec.sante.openncp.common.immutables.Domain;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import net.shibboleth.utilities.java.support.xml.XMLParserException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.xmlsec.keyinfo.KeyInfoSupport;
import org.opensaml.xmlsec.signature.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Domain
public interface AssertionDetails {
    static final Logger LOGGER = LoggerFactory.getLogger(AssertionDetails.class);

    AssertionType getAssertionType();

    default Element getElement() {
        return getAssertion().getDOM();
    }

    Assertion getAssertion();

    default List<X509Certificate> getCertificates() {
        final Signature signature = getAssertion().getSignature();
        try {
            return KeyInfoSupport.getCertificates(signature.getKeyInfo());
        } catch (final CertificateException e) {
            LOGGER.error(String.format("Error fetching the certificates: %s", e.getMessage()), e);
            return Collections.emptyList();
        }
    }

    default Optional<String> getCountryCode() {
        return getCertificates().stream().findFirst().map(certificate -> {
            final String certificateDN = certificate.getSubjectDN().getName();
            return certificateDN.substring(certificateDN.indexOf("C=") + 2, certificateDN.indexOf("C=") + 4);
        });
    }

    static Optional<AssertionDetails> of(final AssertionType assertionType, final DecodedJWT jwt) {
        Validate.notNull(assertionType, "assertionType must not be null");
        Validate.notNull(jwt, "Decoded JWT token must not be null");

        final Claim claim = jwt.getClaim(assertionType.name());
        // StringUtils.isBlank(claim.asString()) should be replaced claim.isMissing() from version 4 and onwards, see EHEALTH-13780
        if (claim == null || claim.isNull() || StringUtils.isBlank(claim.asString())) {
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
            assertion = SAML.assertionFromElement(samlElement);
        } catch (final UnmarshallingException ex) {
            throw new AuthenticationException(Msg.code(333) + ex.getMessage());
        }

        return Optional.of(ImmutableAssertionDetails.builder()
                .assertionType(assertionType)
                .assertion(assertion)
                .build()
        );
    }

    static AssertionDetails of(final Assertion assertion) {
        Validate.notNull(assertion, "assertion must not be null");

        final AssertionType assertionType;
        switch (assertion.getIssuer().getNameQualifier()) {
            case "urn:ehdsi:assertions:hcp":
                assertionType = AssertionType.HCP;
                break;
            case "urn:ehdsi:assertions:trc":
                assertionType = AssertionType.TRC;
                break;
            case "urn:ehdsi:assertions:nok":
                assertionType = AssertionType.NOK;
                break;
            default:
                throw new RuntimeException("Unsupported assertion type: " + assertion.getIssuer().getNameQualifier());
        }


//        // Get the OpenSAML MarshallerFactory
//        final MarshallerFactory marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory();
//        final Marshaller marshaller = marshallerFactory.getMarshaller(assertion);
//        if (marshaller == null) {
//            throw new RuntimeException("No marshaller found for Assertion");
//        }
//
//        final Element element;
//        try {
//            element = marshaller.marshall(assertion);
//        } catch (final MarshallingException e) {
//            throw new RuntimeException(String.format("Error when marshalling the Assertion into a DOM element: [%s]", e.getMessage()), e);
//        }

        return ImmutableAssertionDetails.builder()
                .assertionType(assertionType)
                .assertion(assertion)
                .build();
    }
}
