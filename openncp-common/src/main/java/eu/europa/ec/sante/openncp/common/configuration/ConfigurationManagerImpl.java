package eu.europa.ec.sante.openncp.common.configuration;

import eu.europa.ec.dynamicdiscovery.DynamicDiscoveryBuilder;
import eu.europa.ec.dynamicdiscovery.core.fetcher.impl.DefaultURLFetcher;
import eu.europa.ec.dynamicdiscovery.core.security.impl.DefaultProxy;
import eu.europa.ec.dynamicdiscovery.exception.ConnectionException;
import eu.europa.ec.sante.openncp.common.Constant;
import eu.europa.ec.sante.openncp.common.property.Property;
import eu.europa.ec.sante.openncp.common.property.PropertyService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * OpenNCP Configuration Manager class responsible for the properties management.
 */
@Component
@Transactional
public class ConfigurationManagerImpl implements ConfigurationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationManagerImpl.class);
    private final Map<String, String> properties = new HashMap<>();

    private final PropertyService propertyService;

    public ConfigurationManagerImpl(final PropertyService propertyService) {
        Validate.notNull(propertyService, "propertyService must not be null!");
        this.propertyService = propertyService;
    }

    @Override
    public String getProperty(final String key) {
        Validate.notNull(key, "key must not be null!");
        return getProperty(key, true);
    }

    @Override
    public String getProperty(final Constant constant) {
        Validate.notNull(constant, "constant must not be null!");
        return getProperty(constant.getKey());
    }

    /**
     * @param key      - OpenNCP property key.
     * @param checkMap - boolean value if the cache should be checked or not.
     * @return Value of the property requested.
     */
    public String getProperty(final String key, final boolean checkMap) {
        Validate.notNull(key, "key must not be null!");
        return findProperty(key, checkMap).orElseThrow(() -> new PropertyNotFoundException("Property '" + key + "' not found!"));
    }

    /**
     * Returns a Map of all system properties.
     *
     * @return Map of all the system properties as a key and value pairs.
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public boolean getBooleanProperty(final String key) {
        return Boolean.parseBoolean(getProperty(key));
    }

    @Override
    public int getIntegerProperty(final String key) {
        return Integer.parseInt(getProperty(key));
    }

    @Override
    public void setProperty(final String key, final String value) {
        final Property property = Property.of(key, value);
        properties.put(key, value);
        propertyService.createOrUpdate(property);
    }

    /**
     * This method is currently only used by the webmanager application.
     * Since the webmanager defines it's own datasource and entity factory instead of using the one from openncp-commons
     * we need to provide a static factory method for the DynamicDiscoveryBuilder without any DB calls.
     *
     * @param proxySettings the specific proxy settings
     * @return
     */
    public static DynamicDiscoveryBuilder initializeDynamicDiscoveryFetcher(final ProxySettings proxySettings) {
        //TODO: [Specification] Is it necessary to use the nonProxyHosts feature from DynamicDiscovery module.
        try {
            final DynamicDiscoveryBuilder discoveryBuilder = DynamicDiscoveryBuilder.newInstance();

            if (proxySettings.isEnabled()) {
                final boolean proxyAuthenticated = proxySettings.isAuthenticated();
                final String proxyHost = proxySettings.getHost().orElseThrow();
                final int proxyPort = proxySettings.getPort().orElseThrow();

                if (proxyAuthenticated) {
                    final String proxyUsername = proxySettings.getUsername().orElseThrow();
                    final String proxyPassword = proxySettings.getPassword().orElseThrow();
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Configuring access through Authenticated Proxy '{}:{}' with Credentials: '{}/{}'",
                                proxyHost, proxyPort, proxyUsername, StringUtils.isNoneBlank(proxyPassword) ? "XXXXXX" : "No Password provided");
                    }
                    discoveryBuilder.fetcher(new DefaultURLFetcher(new DefaultProxy(proxyHost, proxyPort, proxyUsername, proxyPassword)));

                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Configuring access through Proxy '{}:{}'", proxyHost, proxyPort);
                    }
                    discoveryBuilder.fetcher(new DefaultURLFetcher(new DefaultProxy(proxyHost, proxyPort)));
                }
            }
            return discoveryBuilder;
        } catch (final ConnectionException e) {
            throw new ConfigurationManagerException("An internal error occurred while trying to connect the Proxy", e);
        }

    }

    /**
     * Initializes the SML/SMP Dynamic Discovery Client.
     *
     * @return Dynamic Discovery Client initialized.
     */
    public DynamicDiscoveryBuilder initializeDynamicDiscoveryFetcher() {
        final Boolean isProxyEnabled = findProperty(StandardProperties.HTTP_PROXY_USED, true).map(Boolean::parseBoolean).orElse(false);

        final ProxySettings proxySettings;
        if (isProxyEnabled) {
            proxySettings = ImmutableProxySettings.builder()
                    .enabled(true)
                    .authenticated(getBooleanProperty(StandardProperties.HTTP_PROXY_USED))
                    .host(getProperty(StandardProperties.HTTP_PROXY_HOST))
                    .port(getIntegerProperty(StandardProperties.HTTP_PROXY_PORT))
                    .username(Optional.ofNullable(getProperty(StandardProperties.HTTP_PROXY_USERNAME)))
                    .password(Optional.ofNullable(getProperty(StandardProperties.HTTP_PROXY_PASSWORD)))
                    .build();
        } else {
            proxySettings = ProxySettings.none();
        }
        return initializeDynamicDiscoveryFetcher(proxySettings);
    }

    /**
     * Sets information related to Endpoint into OpenNCP properties database.
     *
     * @param countryCode - ISO Country code of the Service Provider.
     * @param serviceName - Interoperable service name used.
     * @param url         - URL of the endpoint.
     */
    public void setServiceWSE(final String countryCode, final String serviceName, final String url) {
        setProperty(countryCode + "." + serviceName + ".WSE", url);
    }

    /**
     * Returns application properties including a check into the cache mechanism.
     *
     * @param key      - OpenNCP property key.
     * @param checkMap - boolean value if the cache should be checked or not.
     * @return OpenNCP property.
     */
    private Optional<String> findProperty(final String key, final boolean checkMap) {
        if (checkMap) {
            final String value = properties.get(key);
            if (value != null) {
                return Optional.of(value);
            }
        }

        final Optional<Property> propertyEntity = propertyService.findByKey(key);
        return propertyEntity.map(property -> {
            final String propertyValue = property.getValue();
            properties.put(property.getKey(), propertyValue);
            return propertyValue;
        });
    }
}
