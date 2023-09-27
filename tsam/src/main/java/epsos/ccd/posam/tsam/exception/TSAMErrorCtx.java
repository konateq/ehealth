package epsos.ccd.posam.tsam.exception;

import eu.europa.ec.sante.ehdsi.constant.error.ITMTSAMError;
import eu.europa.ec.sante.ehdsi.constant.error.TSAMError;

public class TSAMErrorCtx implements ITMTSAMError {

    /**
     * Exception code
     */
    private String code;
    /**
     * Exception description (issue - is English description/constant enough ?)
     */
    private String description;
    private String context;

    public TSAMErrorCtx(TSAMError err, String ctx) {
        this.code = err.getCode();
        this.description = err.getDescription();
        this.context = ctx;
    }

    public TSAMErrorCtx() {}

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description + " [" + context + "]";
    }

    public String getContext() {
        return context;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((code == null) ? 0 : code.hashCode());
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TSAMErrorCtx other = (TSAMErrorCtx) obj;
        if (code == null) {
            if (other.code != null)
                return false;
        } else if (!code.equals(other.code))
            return false;
        if (context == null) {
            return other.context == null;
        } else return context.equals(other.context);
    }

    @Override
    public String toString() {
        return code + ": " + description + " [" + context + "]";
    }
}
