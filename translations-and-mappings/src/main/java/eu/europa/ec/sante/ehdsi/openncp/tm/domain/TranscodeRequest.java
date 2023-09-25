package eu.europa.ec.sante.ehdsi.openncp.tm.domain;

import org.w3c.dom.Document;

public class TranscodeRequest {

    private Document friendlyCDA;

    public Document getFriendlyCDA() {
        return friendlyCDA;
    }

    public void setFriendlyCDA(Document friendlyCDA) {
        this.friendlyCDA = friendlyCDA;
    }
}
