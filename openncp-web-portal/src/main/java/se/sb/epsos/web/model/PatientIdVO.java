package se.sb.epsos.web.model;

import java.io.Serializable;

public class PatientIdVO implements Serializable {

    private static final long serialVersionUID = -8950333817890598641L;
    private String label;
    private String domain;
    private String value;
    private Integer max;
    private Integer min;

    public PatientIdVO() {
        super();
    }

    public PatientIdVO(String label, String domain, String value) {
        this.label = label;
        this.domain = domain;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    public Integer getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

    @Override
    public String toString() {
        String result = "";
        result = result + " label: " + label;
        result = result + " domain: " + domain;
        result = result + " value: " + value;
        result = result + " max: " + max;
        result = result + " min: " + min;

        return result;
    }
}
