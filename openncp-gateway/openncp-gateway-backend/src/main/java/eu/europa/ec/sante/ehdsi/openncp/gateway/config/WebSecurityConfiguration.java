package eu.europa.ec.sante.ehdsi.openncp.gateway.config;

import eu.europa.ec.sante.ehdsi.openncp.gateway.persistence.repository.UserRepository;
import eu.europa.ec.sante.ehdsi.openncp.gateway.security.DefaultUserDetailsService;
import eu.europa.ec.sante.ehdsi.openncp.gateway.security.JwtAuthenticationFilter;
import eu.europa.ec.sante.ehdsi.openncp.gateway.security.TokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final TokenProvider tokenProvider;

    public WebSecurityConfiguration(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http
                .cors()
                .and()
                .csrf()
                .disable()
                .authorizeRequests()
                .antMatchers("/webjars/**").permitAll()
                .antMatchers("/authenticate").permitAll()
                .antMatchers("/api/user/reset-password/init", "/api/user/reset-password/finish").permitAll()
                .antMatchers("/account", "/api/**").authenticated()
                .and()
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        // @formatter:on
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return new DefaultUserDetailsService(userRepository);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenProvider);
    }
}
