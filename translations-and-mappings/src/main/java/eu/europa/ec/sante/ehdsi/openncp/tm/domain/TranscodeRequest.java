package eu.europa.ec.sante.ehdsi.openncp.tm.domain;

import org.w3c.dom.Document;

public class TranscodeRequest {

    private String friendlyCDA;

    public String getFriendlyCDA() {
        return friendlyCDA;
    }

    public void setFriendlyCDA(String friendlyCDA) {
        this.friendlyCDA = friendlyCDA;
    }
}
