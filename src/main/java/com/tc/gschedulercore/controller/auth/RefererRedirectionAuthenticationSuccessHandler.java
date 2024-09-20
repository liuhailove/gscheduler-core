package com.tc.gschedulercore.controller.auth;

import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.model.JobUser;
import com.tc.gschedulercore.util.JwtUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * @author honggang.liu
 */
@Component
public class RefererRedirectionAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    public static final String DEFAULT_ROLE_NAME = "TEST_USER_ROLE";

    public RefererRedirectionAuthenticationSuccessHandler() {
        super();
        setUseReferer(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        CustomOAuth2User oauthUser = (CustomOAuth2User) authentication.getPrincipal();
        JobUser jobUser = JobAdminConfig.getAdminConfig().getXxlJobUserDao().loadByUserName(oauthUser.getEmail());
        if (jobUser == null) {
            jobUser = new JobUser();
            jobUser.setRoleName(DEFAULT_ROLE_NAME);
            jobUser.setPermissionPlatforms("");
            jobUser.setUsername(oauthUser.getEmail());
            jobUser.setEmail(oauthUser.getEmail());
            jobUser.setAddTime(new Date());
            jobUser.setUpdateTime(new Date());
            JobAdminConfig.getAdminConfig().getXxlJobUserDao().save(jobUser);
        }
        jobUser.setAuthToken(JwtUtils.createToken(jobUser.getUsername()));
        // do login
        request.getSession().setAttribute("session_xxljob_admin", jobUser);
        response.sendRedirect("/xxl-job-admin/index");
    }

}
