package se.sb.epsos.web;

import org.apache.wicket.Request;
import org.apache.wicket.authentication.AuthenticatedWebSession;
import org.apache.wicket.authorization.strategies.role.Roles;
import org.apache.wicket.injection.web.InjectorHolder;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.jaas.JaasGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import se.sb.epsos.web.auth.AuthenticatedUser;
import se.sb.epsos.web.auth.jaas.JaasGrantedAuthorityPrincipal;
import se.sb.epsos.web.model.BreadCrumbVO;
import se.sb.epsos.web.service.NcpServiceException;
import se.sb.epsos.web.service.NcpServiceFacade;
import se.sb.epsos.web.util.Feature;
import se.sb.epsos.web.util.FeatureFlagsManager;
import se.sb.epsos.web.util.MasterConfigManager;

import javax.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EpsosAuthenticatedWebSession extends AuthenticatedWebSession {

    private static final long serialVersionUID = -8046060728516010324L;
    private static final Logger LOGGER = LoggerFactory.getLogger(EpsosAuthenticatedWebSession.class);
    private List<BreadCrumbVO<?>> breadcrumbList = new ArrayList<>();

    @SpringBean(name = "authenticationManager")
    private AuthenticationManager authenticationManager;

    @SpringBean(name = "serviceFacade")
    private NcpServiceFacade serviceFacade;

    private AuthenticatedUser authenticatedUser;

    public EpsosAuthenticatedWebSession(Request request) {

        super(request);
        injectDependencies();
        ensureDependenciesNotNull();

        String locale = MasterConfigManager.get("ApplicationConfigManager.locale");

        Cookie cookie = ((WebRequest) request).getCookie("epSOSlocale");
        if (cookie != null) {
            locale = cookie.getValue();
        }

        if (locale != null && locale.contains("_")) {
            String[] localeSplit = locale.split("_");
            if (localeSplit.length == 2) {
                setLocale(new Locale(localeSplit[0], localeSplit[1]));
            } else {
                setLocale(Locale.ENGLISH);
            }
        } else {
            setLocale(Locale.ENGLISH);
        }
        LOGGER.info("ServiceFacade initialized with: '{}'", serviceFacade.about());
    }

    private void ensureDependenciesNotNull() {
        if (authenticationManager == null) {
            throw new IllegalStateException("AdminSession requires an authenticationManager.");
        }
    }

    private void injectDependencies() {
        InjectorHolder.getInjector().inject(this);
    }

    @Override
    public boolean authenticate(String username, String password) {
        boolean authenticated;
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            authenticated = authentication.isAuthenticated();
        } catch (AuthenticationException e) {
            LOGGER.warn(String.format("User '%s' failed to login. Reason: %s", username, e.getMessage()), e);
            authenticated = false;
        }
        return authenticated;
    }

    @Override
    public Roles getRoles() {
        Roles roles = new Roles();
        getRolesIfSignedIn(roles);
        return roles;
    }

    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public AuthenticatedUser getUserDetails() {

        Authentication auth = getAuthentication();
        if (FeatureFlagsManager.check(Feature.ENABLE_SWEDISH_JAAS)) {
            if (auth != null) {
                if (authenticatedUser == null) {
                    JaasGrantedAuthority jaasGrantedAuthority = null;
                    for (GrantedAuthority grantedAuthority : auth.getAuthorities()) {
                        jaasGrantedAuthority = (JaasGrantedAuthority) grantedAuthority;
                        LOGGER.info("grantedAuthority:{}", jaasGrantedAuthority.getPrincipal().getName());
                        LOGGER.info("grantedAuthority.getAuthority():{}", jaasGrantedAuthority.getAuthority());
                        JaasGrantedAuthorityPrincipal authorityPrincipal = new JaasGrantedAuthorityPrincipal(jaasGrantedAuthority.getPrincipal());
                        authenticatedUser = new AuthenticatedUser(authorityPrincipal.getId(), null);
                        List<GrantedAuthority> authorities = new ArrayList<>();
                        GrantedAuthority granted = new JaasGrantedAuthority(jaasGrantedAuthority.getAuthority(), jaasGrantedAuthority.getPrincipal());
                        authorities.add(granted);
                        authenticatedUser.setAuthorities(authorities);
                        authenticatedUser.setFamilyName(authorityPrincipal.getLastName());
                        authenticatedUser.setGivenName(authorityPrincipal.getFirstName());
                        authenticatedUser.setUserId(authorityPrincipal.getOrganizationId());
                        authenticatedUser.setOrganizationName(authorityPrincipal.getOrganizationName());
                        authenticatedUser.setOrganizationId(authorityPrincipal.getOrganizationId());
                        authenticatedUser.setCommonName(authorityPrincipal.getCommonName());
                        authenticatedUser.setPrmiaryRole(jaasGrantedAuthority.getAuthority());
                    }
                    getServiceFacade().bindToSession(this.getId());
                    try {
                        getServiceFacade().initServices(authenticatedUser);
                    } catch (NcpServiceException e) {
                        LOGGER.error("NcpServiceException: '{}'", e.getMessage(), e);
                    }
                }
                return authenticatedUser;
            }
            return null;
        } else {
            return auth != null ? (AuthenticatedUser) auth.getPrincipal() : null;
        }
    }

    private void getRolesIfSignedIn(Roles roles) {
        if (isSignedIn()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            addRolesFromAuthentication(roles, authentication);
        }
    }

    private void addRolesFromAuthentication(Roles roles, Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            roles.add(authority.getAuthority());
        }
    }

    public NcpServiceFacade getServiceFacade() {
        return serviceFacade;
    }

    /**
     * Used by test to set a mockito mocked facade.
     *
     * @param serviceFacade
     */
    public void setServiceFacade(NcpServiceFacade serviceFacade) {
        this.serviceFacade = serviceFacade;
    }

    public void removeAllAfterBc(BreadCrumbVO<?> bcVo) {
        for (int i = 0; i < breadcrumbList.size(); i++) {
            if (breadcrumbList.get(i).getTitle().equals(bcVo.getTitle())) {
                if (breadcrumbList.size() > i + 1) {
                    breadcrumbList.subList(i + 1, breadcrumbList.size()).clear();
                }
                break;
            }
        }
    }

    public void addToBreadCrumbList(BreadCrumbVO<?> bcVo) {
        if (bcVo.getWebPage() != null) {
            if (!isClassInBreadCrumbList(bcVo.getWebPage().getClass()) && !isTitleInBreadCrumbList(bcVo.getTitle())) {
                breadcrumbList.add(bcVo);
            }
        } else if (!isTitleInBreadCrumbList(bcVo.getTitle())) {
            breadcrumbList.add(bcVo);
        }
    }

    public List<BreadCrumbVO<?>> getBreadCrumbList() {
        return breadcrumbList;
    }

    public boolean isTitleInBreadCrumbList(String title) {
        for (BreadCrumbVO<?> aBreadcrumbList : breadcrumbList) {
            if (aBreadcrumbList.getTitle().equals(title)) {
                return true;
            }
        }
        return false;
    }

    public boolean isClassInBreadCrumbList(Class<? extends WebPage> page) {
        for (BreadCrumbVO<?> aBreadcrumbList : breadcrumbList) {
            if (aBreadcrumbList.getWebPage() != null && aBreadcrumbList.getWebPage().getClass() == page) {
                return true;
            }
        }
        return false;
    }

    public void clearBreadCrumbList() {
        breadcrumbList.clear();
    }

    public void removePostFromBreadCrumbList(String title) {
        for (int i = 0; i < breadcrumbList.size(); i++) {
            if (breadcrumbList.get(i).getTitle().equals(title)) {
                breadcrumbList.remove(i);
            }
        }
    }

    public void removeAllAfterTitle(String title) {
        for (int i = 0; i < breadcrumbList.size(); i++) {
            if (breadcrumbList.get(i).getTitle().equals(title)) {
                if (breadcrumbList.size() > i + 1) {
                    breadcrumbList.subList(i + 1, breadcrumbList.size()).clear();
                }
                break;
            }
        }
    }

    public void disableBreadCrumbClickability(String title) {
        for (BreadCrumbVO<?> aBreadcrumbList : breadcrumbList) {
            if (aBreadcrumbList.getTitle().equals(title)) {
                aBreadcrumbList.setClickable(false);
            }
        }
    }
}
