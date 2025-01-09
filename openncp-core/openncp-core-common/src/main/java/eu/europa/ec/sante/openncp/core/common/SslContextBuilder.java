package eu.europa.ec.sante.openncp.core.common;

import eu.europa.ec.sante.openncp.common.audit.ssl.KeystoreDetails;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.util.ResourceUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class SslContextBuilder {
    public static SSLContext build(final KeystoreDetails keystore, final KeystoreDetails trustStore) throws NoSuchAlgorithmException, KeyManagementException, IOException,
            CertificateException, KeyStoreException, UnrecoverableKeyException {

        final SSLContextBuilder builder = SSLContextBuilder.create();
        builder.setKeyStoreType(keystore.getKeystoreType());
        builder.setKeyManagerFactoryAlgorithm(keystore.getAlgType());
        builder.loadKeyMaterial(ResourceUtils.getFile(keystore.getKeystoreLocation()),
                keystore.getKeystorePassword().toCharArray(),
                keystore.getKeyPassword()
                        .orElseThrow(() -> new RuntimeException("The keystore password is mandatory to build an SSL context"))
                        .toCharArray());
        builder.loadTrustMaterial(ResourceUtils.getFile(trustStore.getKeystoreLocation()),
                trustStore.getKeystorePassword().toCharArray(), TrustAllStrategy.INSTANCE);

        return builder.build();
    }
}
