package eu.europa.ec.sante.ehdsi.openncp.tm.util;

import org.jaxen.BaseXPath;
import org.jaxen.JaxenException;

public class NoNsXpath extends BaseXPath {

    private static final long serialVersionUID = 5573866856020883718L;

    public NoNsXpath(String xpathExpr) throws JaxenException {
        super(xpathExpr, new NoNsNavigator());
    }
}
