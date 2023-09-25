package eu.europa.ec.sante.ehdsi.openncp.tm.exception;

import eu.europa.ec.sante.ehdsi.constant.error.ITMTSAMError;
import eu.europa.ec.sante.ehdsi.constant.error.TMError;

public class TMException extends Exception {

    private ITMTSAMError reason;

    public TMException(TMError reason) {
        this.reason = reason;
    }

    public TMException(TmErrorCtx reason) {
        this.reason = reason;
    }


    public ITMTSAMError getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return reason.getCode() + ": " + reason.getDescription();
    }
}