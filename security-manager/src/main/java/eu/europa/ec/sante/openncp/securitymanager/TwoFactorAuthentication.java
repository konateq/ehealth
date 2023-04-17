package eu.europa.ec.sante.openncp.securitymanager;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.opensaml.saml.saml2.core.AuthnContext.*;

public enum TwoFactorAuthentication {

    TWO_FACTOR_MTFU_AUTHN_CTX(MTFU_AUTHN_CTX),
    TWO_FACTOR_MTFC_AUTHN_CTX(MTFC_AUTHN_CTX),
    TWO_FACTOR_X509_AUTHN_CTX(X509_AUTHN_CTX),
    TWO_FACTOR_SPKI_AUTHN_CTX(SPKI_AUTHN_CTX),
    TWO_FACTOR_SMARTCARD_PKI_AUTHN_CTX(SMARTCARD_PKI_AUTHN_CTX),
    TWO_FACTOR_SOFTWARE_PKI_AUTHN_CTX(SOFTWARE_PKI_AUTHN_CTX),
    TWO_FACTOR_TLS_CLIENT_AUTHN_CTX(TLS_CLIENT_AUTHN_CTX);

    private final String authType;

    TwoFactorAuthentication(final String authType) {
        this.authType = authType;
    }

    public static List<String> getAuthTypeValues() {
        return Arrays.stream(TwoFactorAuthentication.values()).map(Object::toString).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return authType;
    }
}
