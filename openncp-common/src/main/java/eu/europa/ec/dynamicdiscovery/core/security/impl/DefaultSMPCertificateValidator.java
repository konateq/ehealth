package eu.europa.ec.dynamicdiscovery.core.security.impl;

import eu.europa.ec.dynamicdiscovery.core.security.ISMPCertificateValidator;
import eu.europa.ec.dynamicdiscovery.exception.SignatureException;
import eu.europa.ec.dynamicdiscovery.exception.TechnicalException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: This class needs to be removed as soon as the issue reported is fixed by the dynamic discovery team
 * Override of the class of the same name in dinamycdiscovery because of https://citnet.tech.ec.europa.eu/CITnet/jira/browse/EHEALTH-12803
 */

public class DefaultSMPCertificateValidator implements ISMPCertificateValidator {
    static final Logger LOG = LoggerFactory.getLogger(DefaultSMPCertificateValidator.class);
    protected KeyStore trustStore;
    protected Pattern regexCertificateSubjectValidation;

    public DefaultSMPCertificateValidator(KeyStore trustStore, String regexCertificateSubjectValidation) throws TechnicalException {
        this.trustStore = trustStore;
        this.regexCertificateSubjectValidation = StringUtils.isBlank(regexCertificateSubjectValidation) ? null : Pattern.compile(regexCertificateSubjectValidation);
        if (this.trustStore == null) {
            throw new SignatureException("TrustStore must be not null for signature validation.");
        }
    }

    public void validateSMPCertificate(X509Certificate certificate) throws CertificateException {
        String certName = certificate.getSubjectX500Principal().getName();
        LOG.debug("Validate Certificate [{}]", certName);
        certificate.checkValidity();
        this.verifyTrust(certificate);
        this.verifyCertificateSubject(certificate);
        LOG.debug("Certificate % is valid and trusted", certName);
    }

    private void verifyCertificateSubject(X509Certificate signerCertificate) throws CertificateException {
        if (this.regexCertificateSubjectValidation != null) {
            String subject = signerCertificate.getSubjectX500Principal().toString();
            Matcher matcher = this.regexCertificateSubjectValidation.matcher(subject);
            if (!matcher.matches()) {
                throw new CertificateException(String.format("Given certificate: %s does not match configured regex: %s.", subject, this.regexCertificateSubjectValidation.pattern()));
            }

            LOG.debug("Given certificate: %s  match the configured regex: %s.", subject, this.regexCertificateSubjectValidation.pattern());
        } else {
            LOG.debug("Null regular expression for subject verification!");
        }

    }

    private void verifyTrust(X509Certificate signerCertificate) throws CertificateException {
        try {
            for(String alias : Collections.list(this.trustStore.aliases())) {
                if (this.isAliasCertificateTrustAnchor(signerCertificate, alias)) {
                    LOG.debug("Certificate with alias [{}] is trust anchor of the certificate [{}]!", alias, signerCertificate.getSubjectDN().getName());
                    return;
                }
            }

            throw new CertificateException("TrustStore does not contain trusted direct Issuer or the Certificate.");
        } catch (KeyStoreException | RuntimeException exc) {
            throw new CertificateException("Runtime exception:" + ((Exception)exc).getMessage(), exc);
        }
    }

    protected boolean isAliasCertificateTrustAnchor(X509Certificate signerCertificate, String alias) throws CertificateException {
        String certName = signerCertificate.getSubjectX500Principal().getName();

        try {
            this.trustStore.getEntry(alias, (KeyStore.ProtectionParameter)null);
            if (!this.trustStore.entryInstanceOf(alias, KeyStore.TrustedCertificateEntry.class)) {
                LOG.warn("Certificate with alias [{}] is not Trusted certificate entry!", alias);
                return false;
            }

            KeyStore.TrustedCertificateEntry certificateEntry = (KeyStore.TrustedCertificateEntry)this.trustStore.getEntry(alias, (KeyStore.ProtectionParameter)null);
            Certificate trustedCertificate = certificateEntry.getTrustedCertificate();
            if (!(trustedCertificate instanceof X509Certificate)) {
                LOG.warn("Certificate with alias [{}] is not X509Certificate! Only X509Certificate type is supported!", alias);
                return false;
            }

            X509Certificate x509TrustedCertificate = (X509Certificate)certificateEntry.getTrustedCertificate();
            if (signerCertificate.equals(trustedCertificate)) {
                LOG.debug("Certificate with alias [{}] is direct trust anchor of the certificate [{}]!", alias, certName);
                return true;
            }

            if (this.isSignedBy(signerCertificate, x509TrustedCertificate, certName, alias)) {
                LOG.debug("Certificate with alias [{}] is 'chain' trust anchor of the certificate [{}]!", alias, certName);
                return true;
            }

            x509TrustedCertificate.checkValidity();
        }  catch (CertificateExpiredException e) {
            throw new CertificateException("Certificate with alias " + alias + " is expired!");
        }  catch (KeyStoreException | UnrecoverableEntryException | NoSuchAlgorithmException exc) {
            throw new CertificateException("Truststore exception occurred when  accessing certificate with alias:" + alias + ". Error message:" + ((GeneralSecurityException)exc).getMessage(), exc);
        }

        LOG.debug("Certificate with alias [{}] is not trust anchor of the certificate [{}]!", alias, certName);
        return false;
    }

    private boolean isSignedBy(Certificate signed, Certificate signer, String signedCertificateName, String alias) throws CertificateException {
        try {
            signed.verify(signer.getPublicKey());
            LOG.debug("Certificate [{}] is signed by certificate with alias [{}] from truststore.", signedCertificateName, alias);
            return true;
        } catch (InvalidKeyException | NoSuchProviderException | java.security.SignatureException | NoSuchAlgorithmException e) {
            LOG.error("Error occurred while verifying signature of the certificate [" + signedCertificateName + "] with certificate from truststore with alias [" + alias + "].", e);
            return false;
        }
    }
}
