package eu.europa.ec.sante.openncp.common;
public enum NcpSide {

    NCP_A("NCP-A"),
    NCP_B("NCP-B"),
    OFFICER("OFFICER");

    private String name;

    NcpSide(String s) {
        name = s;
    }

    public String getName() {
        return name;
    }
}