package ch.admin.bag.covidcertificate.backend.delivery.ws.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class BaseSecurity extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // TODO csrf disable
        http.csrf().disable().authorizeRequests().antMatchers("/v1/**").permitAll();
    }
}
