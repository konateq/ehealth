package eu.europa.ec.sante.openncp.common.audit.ssl;

import eu.europa.ec.sante.openncp.common.immutables.Domain;
import org.immutables.value.Value;

import java.util.Optional;
import java.util.Set;

@Domain
public interface KeystoreDetails {
    String getKeystoreLocation();

    String getKeystorePassword();

    Optional<String> getAlias();

    Optional<String> getKeyPassword();

    /**
     * combination of host (domain or IP) and port separated by a colon.
     *
     * @return
     */
    Optional<String> getAuthority();

    Set<String> getAuthorizedDNs();

    @Value.Default
    default String getKeystoreType() {
        return "JKS";
    }

    @Value.Default
    default String getAlgType() {
        return "SunX509";
    }

    /*
     * create a KeystoreDetails for accessing a certificate
     *
     * @param keystoreLocation
     * @param keystorePassword
     * @param alias
     * @param keyPassword
     */
    static KeystoreDetails of(final String keystoreLocation, final String keystorePassword, final String alias, final String keyPassword) {
        return ImmutableKeystoreDetails.builder()
                .keystoreLocation(keystoreLocation)
                .keystorePassword(keystorePassword)
                .alias(alias)
                .keyPassword(keyPassword)
                .build();
    }

    static KeystoreDetails of(final String keystoreLocation, final String keystorePassword, final String alias) {
        return ImmutableKeystoreDetails.builder()
                .keystoreLocation(keystoreLocation)
                .keystorePassword(keystorePassword)
                .alias(alias)
                .keyPassword(keystorePassword)
                .build();
    }
}
