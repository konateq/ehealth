package eu.europa.ec.sante.openncp.common.util;

import eu.europa.ec.sante.openncp.common.immutables.Domain;

import java.util.Optional;

/**
 * Container abstraction to hold relevant data regarding certificates.
 * Used to circumvent all the {@link eu.europa.ec.sante.openncp.common.configuration.util.Constants} accesses in places
 * that have no {@link eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager} created by spring.
 */
@Domain
public interface CertificatesDataHolder {
    CertificateData getTrustoreData();

    Optional<CertificateData> getServiceProviderData();

    Optional<CertificateData> getServiceConsumerData();

    Optional<CertificateData> getSignatureData();

    @Domain
    interface CertificateData {
        /**
         * Returns the physical location of the keystore
         * @return the physical location of the keystore
         */
        String getKeystorePath();

        /**
         * Returns the password to open the keystore
         * @return the password to open the keystore
         */
        String getKeystorePassword();

        /**
         * Returns the alias to fetch the private key entry from the keystore specified in the {@link #getKeystorePath()} method
         * @return the alias of the private key entry
         */
        Optional<String> getPrivateKeyAlias();

        /**
         * Returns the password to open the private key entry, identified by the alias from the {@link #getPrivateKeyAlias()} and the keystore specified in the {@link #getKeystorePath()} method
         * @return the password of the private key entry
         */
        Optional<String> getPrivateKeyPassword();

        static ImmutableCertificateData.KeystorePathBuildStage builder() {
            return ImmutableCertificateData.builder();
        }
    }

    static ImmutableCertificatesDataHolder.TrustoreDataBuildStage builder() {
        return ImmutableCertificatesDataHolder.builder();
    }
}
