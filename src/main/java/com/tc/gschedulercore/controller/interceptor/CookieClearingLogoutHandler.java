package com.tc.gschedulercore.controller.interceptor;

import com.tc.gschedulercore.core.util.CookieUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

import static com.tc.gschedulercore.controller.LoginController.LOGIN_IDENTITY_KEY;


/**
 * cookie清理
 *
 * @author honggang.liu
 */
public final class CookieClearingLogoutHandler implements LogoutHandler {
    private final List<String> cookieToClear;

    public CookieClearingLogoutHandler(String... cookieToClear) {
        this.cookieToClear = Arrays.asList(cookieToClear);
    }

    @Override
    public void logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {
        for (String cookieName : cookieToClear) {
            Cookie cookie = new Cookie(cookieName, null);
            String cookiePath = httpServletRequest.getContextPath();
            if (!StringUtils.hasLength(cookiePath)) {
                cookiePath = "/";
            }
            cookie.setPath(cookiePath);
            cookie.setMaxAge(0);
            httpServletResponse.addCookie(cookie);
        }
        CookieUtil.remove(httpServletRequest, httpServletResponse, "/",LOGIN_IDENTITY_KEY);
        CookieUtil.remove(httpServletRequest, httpServletResponse, "/","JSESSIONID");
        CookieUtil.remove(httpServletRequest, httpServletResponse, "/xxl-job-admin",LOGIN_IDENTITY_KEY);
        CookieUtil.remove(httpServletRequest, httpServletResponse, "/xxl-job-admin","JSESSIONID");
    }
}
