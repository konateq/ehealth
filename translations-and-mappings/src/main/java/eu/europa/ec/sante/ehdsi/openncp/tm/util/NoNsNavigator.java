package eu.europa.ec.sante.ehdsi.openncp.tm.util;

import org.jaxen.dom.DocumentNavigator;

public class NoNsNavigator extends DocumentNavigator {

    private static final long serialVersionUID = -6556761231178922564L;

    @Override
    public String getElementNamespaceUri(Object obj) {
        return null;
    }

}
