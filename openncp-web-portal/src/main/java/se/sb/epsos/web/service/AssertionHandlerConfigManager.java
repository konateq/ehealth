package se.sb.epsos.web.service;

import se.sb.epsos.web.util.MasterConfigManager;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AssertionHandlerConfigManager implements Serializable {

    public static final String CONFIG_PREFIX = "AssertionHandlerConfigManager.";
    public static final String ROLE_PERMISSIONS_PREFIX = "RolePermissions.";
    public static final String XSPA_LOCALITY = "XspaLocality";
    private static final long serialVersionUID = -968106064536729654L;
    private static final String PERMISSIONS_PREFIX = "PermissionsPrefix";
    private static final String FACILITY_TYPE = "FacilityType";
    private static final String PURPOSE_OF_USE = "PurposeOfUse";

    public static String getPermissionsPrefix() {
        return MasterConfigManager.get(CONFIG_PREFIX + ROLE_PERMISSIONS_PREFIX + PERMISSIONS_PREFIX);
    }

    public static String getFacilityType(String role) {
        return MasterConfigManager.get(CONFIG_PREFIX + ROLE_PERMISSIONS_PREFIX + role + "[@workPlace]");
    }

    public static String getPurposeOfUse() {
        return MasterConfigManager.get(CONFIG_PREFIX + PURPOSE_OF_USE);
    }

    public static String getXSPALocality() {
        return MasterConfigManager.get(CONFIG_PREFIX + XSPA_LOCALITY);
    }

    public static String getRoleDisplayName(String role) {
        return MasterConfigManager.get(CONFIG_PREFIX + ROLE_PERMISSIONS_PREFIX + role + "[@displayName]");
    }

    public static Set<String> getPermissions(String role) {

        String permissionsString = MasterConfigManager.get(CONFIG_PREFIX + ROLE_PERMISSIONS_PREFIX + role);
        Set<String> permissions = new HashSet<>();
        if (permissionsString != null) {
            String[] splittedPermissions = permissionsString.split(",");
            permissions.addAll(Arrays.asList(splittedPermissions));
        }
        return permissions;
    }

}
