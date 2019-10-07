package se.sb.epsos.web.service;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AssertionHandlerConfigManagerTest {

    @Test
    public void testGetPermissionsPrefix() {

    }

    @Test
    public void testGetFacilityType_ROLE_PHARMACIST() {
        String role = AssertionHandlerConfigManager.getFacilityType("ROLE_PHARMACIST");
        assertNotNull(role);
        assertEquals("Pharmacy", role);
    }

    @Test
    public void testGetFacilityType_ROLE_DOCTOR() {
        String role = AssertionHandlerConfigManager.getFacilityType("ROLE_DOCTOR");
        assertNotNull(role);
        assertEquals("Hospital", role);
    }

    @Test
    public void testGetFacilityType_ROLE_NURSE() {
        String role = AssertionHandlerConfigManager.getFacilityType("ROLE_NURSE");
        assertNotNull(role);
        assertEquals("Hospital", role);
    }

    @Test
    public void testGetPurposeOfUse() {
        String s = AssertionHandlerConfigManager.getPurposeOfUse();
        assertEquals("TREATMENT", s);
    }

    @Test
    public void testGetXspaLocality() {

    }

    @Test
    public void testGetRoleDisplayName_ROLE_PHARMACIST() {
        String role = AssertionHandlerConfigManager.getRoleDisplayName("ROLE_PHARMACIST");
        assertNotNull(role);
        assertEquals("pharmacist", role);
    }

    @Test
    public void testGetRoleDisplayName_ROLE_DOCTOR() {
        String role = AssertionHandlerConfigManager.getRoleDisplayName("ROLE_DOCTOR");
        assertNotNull(role);
        assertEquals("physician", role);
    }

    @Test
    public void testGetRoleDisplayName_ROLE_NURSE() {
        String role = AssertionHandlerConfigManager.getRoleDisplayName("ROLE_NURSE");
        assertNotNull(role);
        assertEquals("nurse", role);
    }

    @Test
    public void testGetPersmissions() {
        Set<String> roles = AssertionHandlerConfigManager.getPermissions("ROLE_PHARMACIST");
        assertNotNull(roles);
        assertEquals(4, roles.size());
    }
}
