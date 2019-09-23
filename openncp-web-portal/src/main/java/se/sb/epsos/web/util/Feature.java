package se.sb.epsos.web.util;

public enum Feature {

    EXAMPLE_FEATURE(false),
    SHOW_HELP_TEXT_FOR_TEST(false),
    SEND_METRICS_TO_GRAPHITE(false),
    SHOW_PARTIAL_ERROR_MESSAGES(false),
    ENABLE_STATUS_PAGE(false),
    ENABLE_SSL(false),
    ENABLE_SWEDISH_JAAS(false);

    protected boolean defaultOn;

    Feature(boolean defaultOn) {
        this.defaultOn = defaultOn;
    }
}
