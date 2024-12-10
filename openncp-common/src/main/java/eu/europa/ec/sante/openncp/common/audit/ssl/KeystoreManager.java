package eu.europa.ec.sante.openncp.common.audit.ssl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.*;

/**
 *
 */
public class KeystoreManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeystoreManager.class);
    private static final String DEFAULT = "default";

    private static X509TrustManager sunTrustManager = null;

    static {
        loadDefaultTrustManager();
    }

    private KeystoreDetails defaultKeyDetails;
    private final HashMap<String, KeystoreDetails> allKeys = new HashMap<>();
    private final HashMap<String, KeystoreDetails> allStores = new HashMap<>();
    private File keysDir;
    private File certsDir;
    private String home;

    public KeystoreManager(final String home) {

        if (home != null) {
            this.home = home;
            loadKeys(this.home);
        }
    }

    private static void loadDefaultTrustManager() {

        try {
            File certs;
            final String definedcerts = System.getProperty("javax.net.ssl.trustStore");
            String pass = System.getProperty("javax.net.ssl.trustStorePassword");
            if (definedcerts != null) {
                certs = new File(definedcerts);
            } else {
                final String common = System.getProperty("java.home") + File.separator + "lib" + File.separator + "security" + File.separator;
                final String cacerts = common + "cacerts";
                final String jssecacerts = common + "jssecacerts";
                certs = new File(jssecacerts);
                if (!certs.exists() || certs.length() == 0) {
                    certs = new File(cacerts);
                }
            }
            if (pass == null) {
                pass = "changeit";
            }
            if (certs.exists()) {
                final KeyStore ks = KeyStore.getInstance("jks");
                try (final FileInputStream inputStream = new FileInputStream(certs)) {
                    ks.load(inputStream, pass.toCharArray());
                    final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509", "SunJSSE");
                    tmf.init(ks);
                    final TrustManager[] tms = tmf.getTrustManagers();
                    for (final TrustManager tm : tms) {
                        if (tm instanceof X509TrustManager) {
                            LOGGER.info("Found default trust manager.");
                            sunTrustManager = (X509TrustManager) tm;
                            break;
                        }
                    }
                }
            }
        } catch (final KeyStoreException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException |
                       IOException e) {
            LOGGER.warn("Exception thrown trying to create default trust manager: '{}'", e.getMessage());
        }
    }

    public static X509TrustManager getDefaultTrustManager() {
        return sunTrustManager;
    }

    private static String trimPort(String host) {

        final int colon = host.indexOf(":");
        if (colon > 0 && colon < host.length() - 1) {
            try {
                final int port = Integer.parseInt(host.substring(colon + 1, host.length()), host.length());
                host = host.substring(0, colon);
                LOGGER.info("KeystoreManager.trimPort up to colon: '{}' and port: '{}'", host, port);

                return host;
            } catch (final NumberFormatException e) {
                LOGGER.error("NumberFormatException: '{}'", e.getMessage());
            }
        }
        return null;
    }

    private static String getAnyPort(String auth) {

        final int star = auth.indexOf("*");
        if (star == auth.length() - 1) {
            final int colon = auth.indexOf(":");
            if (colon == star - 1) {
                auth = auth.substring(0, colon);
                return auth;
            }
        }
        return null;
    }

    private void loadKeys(final String home) {

        final File sec = new File(home);
        if (!sec.exists()) {
            return;
        }

        keysDir = new File(sec, "keys");
        if (!keysDir.exists()) {
            final boolean keyFolderCreated = keysDir.mkdir();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Keys directory creation result: '{}'", keyFolderCreated);
            }
        }
        certsDir = new File(sec, "certs");
        if (!certsDir.exists()) {
            final boolean certFolderCreated = certsDir.mkdir();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Keys directory creation result: '{}'", certFolderCreated);
            }
        }
        File[] keyFiles = keysDir.listFiles();
        if (keyFiles != null) {
            for (final File keyFile : keyFiles) {
                try {
                    final KeystoreDetails kd = load(new FileInputStream(keyFile));
                    kd.getAuthority()
                            .map(String::trim)
                            .filter(authority -> authority.equalsIgnoreCase(DEFAULT))
                            .ifPresent(authority -> defaultKeyDetails = kd);

                    allKeys.put(keyFile.getName(), kd);

                } catch (final IOException e) {
                    LOGGER.info("IOException thrown while loading details from '{}'", keyFile.getAbsolutePath());
                }
            }
        }
        keyFiles = certsDir.listFiles();
        if (keyFiles != null) {
            for (final File keyFile : keyFiles) {
                try {
                    final KeystoreDetails kd = load(new FileInputStream(keyFile));
                    allStores.put(keyFile.getName(), kd);

                } catch (final IOException e) {
                    LOGGER.info("IOException thrown while loading details from '{}'", keyFile.getAbsolutePath());
                }
            }
        }
    }

    public void addKeyDetails(final String fileName, final KeystoreDetails details) throws IOException {
        storeAsKey(details, fileName);
        allKeys.put(fileName, details);
    }

    public void addTrustDetails(final String fileName, final KeystoreDetails details) throws IOException {
        storeAsCert(details, fileName);
        allStores.put(fileName, details);
    }

    public void deleteKeyDetails(final String fileName) {
        allKeys.remove(fileName);
        deleteKey(fileName);
    }

    public void deleteTrustDetails(final String fileName) {
        allStores.remove(fileName);
        deleteCert(fileName);
    }

    public KeystoreDetails getKeyDetails(final String fileName) {
        return allKeys.get(fileName);
    }

    public KeystoreDetails getTrustStoreDetails(final String fileName) {
        return allStores.get(fileName);
    }

    public void setDefaultKeystoreDetails(final KeystoreDetails details) {
        defaultKeyDetails = details;
    }

    public KeystoreDetails getDefaultKeyDetails() {
        return defaultKeyDetails;
    }

    public File getKeysDirectory() {
        return keysDir;
    }

    public File getCertsDirectory() {
        return certsDir;
    }

    public KeystoreDetails getKeyFileDetails(final String fileName) {
        return allKeys.get(fileName);
    }

    public KeystoreDetails getStoreFileDetails(final String fileName) {
        return allStores.get(fileName);
    }

    public String[] getKeyfileNames() {
        return allKeys.keySet().toArray(new String[allKeys.keySet().size()]);
    }

    public String[] getTrustfileNames() {
        return allStores.keySet().toArray(new String[allStores.keySet().size()]);
    }

    public KeystoreDetails getKeyFileForHost(String host) {

        KeystoreDetails def = null;
        for (final KeystoreDetails keystoreDetails : allKeys.values()) {
            LOGGER.info("KeystoreManager.getKeyFileForHost getting next key authority: '{}'", keystoreDetails.getAuthority());
            String auth = keystoreDetails.getAuthority().orElse(null);
            if (auth != null) {
                if (auth.endsWith("*")) {
                    final String s = trimPort(host);
                    if (s != null) {
                        LOGGER.info("KeystoreManager.getKeyFileForHost trimmed port: '{}'", s);
                        final String a = getAnyPort(auth);
                        if (a != null) {
                            LOGGER.info("KeystoreManager.getKeyFileForHost trimmed auth: '{}'", a);
                            auth = a;
                            host = s;
                        }
                    }
                }
                if (StringUtils.equals(auth, host)) {
                    return keystoreDetails;
                } else if (StringUtils.equalsIgnoreCase(auth, DEFAULT)) {
                    def = keystoreDetails;
                }
            }
        }
        return def;
    }

    public KeystoreDetails getTrustFileForHost(String host) {

        KeystoreDetails def = null;
        for (final KeystoreDetails keystoreDetails : allStores.values()) {
            String auth = keystoreDetails.getAuthority().orElse(null);
            if (auth != null) {
                if (auth.endsWith("*")) {
                    final String s = trimPort(host);
                    if (s != null) {
                        final String a = getAnyPort(auth);
                        if (a != null) {
                            auth = a;
                            host = s;
                        }
                    }
                }
                if (auth.equals(host)) {
                    return keystoreDetails;
                } else if (auth.equalsIgnoreCase(DEFAULT)) {
                    def = keystoreDetails;
                }
            }
        }
        return def;
    }

    public KeystoreDetails load(final InputStream in) throws IOException {

        final Properties props = new Properties();
        props.load(in);
        final String keystoreLocation = props.getProperty("keystoreLocation");
        if (keystoreLocation == null || keystoreLocation.length() == 0) {
            throw new IOException("no location defined");
        }
        final String keystorePassword = props.getProperty("keystorePassword");
        if (keystorePassword == null || keystorePassword.length() == 0) {
            throw new IOException("no keystore password defined");
        }
        final String alias = props.getProperty("alias");
        String keyPassword = props.getProperty("keyPassword");
        if (keyPassword == null || keyPassword.length() == 0) {
            keyPassword = keystorePassword;
        }
        String keystoreType = props.getProperty("keystoreType");
        if (keystoreType == null || keystoreType.length() == 0) {
            keystoreType = "JKS";
        }
        String algType = props.getProperty("algType");
        if (algType == null || algType.length() == 0) {
            algType = "SunX509";
        }
        String authority = props.getProperty("authority");
        if (authority == null) {
            authority = "";
        }

        final String dns = props.getProperty("authorizedDNs");
        final List<String> authorizedDNs = new ArrayList<>();
        if (dns != null && dns.length() > 0) {
            final String[] dn = dns.split("&");
            for (final String s : dn) {
                final String decoded = URLDecoder.decode(s, StandardCharsets.UTF_8);
                if (decoded.length() > 0) {
                    authorizedDNs.add(decoded);
                }
            }
        }
        return ImmutableKeystoreDetails.builder()
                .keystoreLocation(keystoreLocation)
                .keystorePassword(keystorePassword)
                .alias(alias)
                .keyPassword(keyPassword)
                .algType(algType)
                .keystoreType(keystoreType)
                .authority(authority)
                .addAllAuthorizedDNs(authorizedDNs)
                .build();
    }

    public void storeAsKey(final KeystoreDetails details, final String name) throws IOException {
        store(details, name, true);
    }

    public void storeAsCert(final KeystoreDetails details, final String name) throws IOException {
        store(details, name, false);
    }

    public boolean deleteKey(final String name) {
        return delete(name, true);
    }

    public boolean deleteCert(final String name) {
        return delete(name, false);
    }

    private boolean delete(final String name, final boolean key) {

        File f = key ? getKeysDirectory() : getCertsDirectory();
        f = new File(f, name);
        return f.delete();
    }

    private void store(final KeystoreDetails details, final String name, final boolean key) throws IOException {

        final Properties props = new Properties();
        props.setProperty("keystoreLocation", details.getKeystoreLocation());
        props.setProperty("keystorePassword", details.getKeystorePassword());
        details.getAlias().ifPresent(alias -> props.setProperty("alias", alias));
        details.getKeyPassword().ifPresent(keyPassword -> props.setProperty("keyPassword", keyPassword));
        props.setProperty("keystoreType", details.getKeystoreType());
        props.setProperty("algType", details.getAlgType());
        details.getAuthority().ifPresent(authority -> props.setProperty("authority", authority));

        final Set<String> authorizedDNs = details.getAuthorizedDNs();
        if (!authorizedDNs.isEmpty()) {
            final StringBuilder sb = new StringBuilder();
            for (final String dn : authorizedDNs) {
                sb.append(URLEncoder.encode(dn, StandardCharsets.UTF_8)).append("&");
            }
            props.setProperty("authorizedDNs", sb.toString());
        }
        File f = key ? getKeysDirectory() : getCertsDirectory();
        f = new File(f, name);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(f);
            props.store(out, "Details for " + details.getAlias() + " keystore access.");
            out.close();
        } catch (final IOException e) {
            throw e;
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
