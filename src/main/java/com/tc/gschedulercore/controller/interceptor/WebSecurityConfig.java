package com.tc.gschedulercore.controller.interceptor;

import com.tc.gschedulercore.controller.auth.CustomOAuth2UserService;
import com.tc.gschedulercore.controller.auth.RefererRedirectionAuthenticationSuccessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

import javax.annotation.Resource;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private static Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class.getSimpleName());

    /**
     * auth服务
     */
    @Resource
    private CustomOAuth2UserService oauthUserService;

    @Resource
    private RefererRedirectionAuthenticationSuccessHandler refererRedirectionAuthenticationSuccessHandler;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().authorizeRequests().anyRequest().permitAll()
                .and().httpBasic().disable()
                .headers().frameOptions().disable().and()
                .logout().invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "XXL_JOB_LOGIN_IDENTITY")
                .logoutUrl("/logout")
                .logoutSuccessUrl("/toLogin")
                .addLogoutHandler(new CookieClearingLogoutHandler("JSESSIONID", "XXL_JOB_LOGIN_IDENTITY"))
                .and().oauth2Login()
                .loginPage("/toLogin")
                .userInfoEndpoint()
                .userService(oauthUserService).and()
                .successHandler(refererRedirectionAuthenticationSuccessHandler)
                .failureHandler((httpServletRequest, httpServletResponse, e) -> {
                    logger.error("+++++++++++++++++++oauthUserService failed", e);
                    e.printStackTrace();
                }).and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

}
