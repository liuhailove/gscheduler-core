package com.tc.gschedulercore.controller;

import com.tc.gschedulercore.core.model.JobUser;
import com.tc.gschedulercore.core.util.CookieUtil;
import com.tc.gschedulercore.dao.JobUserDao;
import com.tc.gschedulercore.service.LoginService;
import com.tc.gschedulercore.util.JwtUtils;
import com.tc.gschedulercore.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


@Controller
public class LoginController {
    @Resource
    private JobUserDao jobUserDao;

    private LoginService loginService;
    /**
     * 默认角色
     */
    public static final String DEFAULT_ROLE_NAME = "TEST_USER_ROLE";

    /**
     * cookie唯一标识
     */
    public static final String LOGIN_IDENTITY_KEY = "gscheduler_IDENTITY";

    public static final String WEB_SESSION_KEY = "session_gscheduler_admin";

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class.getSimpleName());

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @RequestMapping(value = "/toLogin", method = {RequestMethod.GET})
    public String toLogin(HttpServletRequest request, HttpServletResponse response) {
        // token can be revoked here if needed
        new SecurityContextLogoutHandler().logout(request, null, null);
        CookieUtil.removeAll(request, response);
        return "login";
    }

    @RequestMapping(value = "login", method = {RequestMethod.POST})
    public String login(HttpServletResponse response, HttpServletRequest request, @RequestParam String username, @RequestParam String password) {
        if (org.apache.commons.lang.StringUtils.isEmpty(username)) {
            LOGGER.error("Login failed: Invalid username or password, username={}", username);
            // 设置cookie为空
            return "login";
        }
        JobUser jobUser = jobUserDao.loadByUserName(username);
        if (jobUser == null) {
            return "login";
        }
        String s1 = jobUser.getPwd();
        String s2 = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!StringUtils.hasLength(s1) || !s1.equals(s2)) {
            return "login";
        }
        // do login
        jobUser.setAuthToken(JwtUtils.createToken(jobUser.getUsername()));
        request.getSession().setAttribute(WEB_SESSION_KEY, jobUser);

        return "redirect:/index";
    }

    @RequestMapping(value = "logout", method = RequestMethod.GET)
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        // token can be revoked here if needed
        request.getSession().invalidate();
        return "login";
    }

    @PostMapping(value = "/check")
    @ResponseBody
    public Result<?> check(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Object seaUserObj = session.getAttribute(WEB_SESSION_KEY);
        JobUser jobUser = null;
        if (seaUserObj instanceof JobUser) {
            jobUser = (JobUser) seaUserObj;
        }
        if (jobUser == null) {
            return Result.ofFail(-1, "Not logged in");
        }
        JobUser authUserDB = loginService.load(jobUser.getId());
        authUserDB.setAuthToken(jobUser.getAuthToken());
        return Result.ofSuccess(authUserDB);
    }

    /**
     * 创建token
     *
     * @param userName 根据用户名称创建token
     * @return token
     */
    @GetMapping(value = "/createToken")
    @ResponseBody
    public String createToken(@RequestParam("userName") String userName) {
        return JwtUtils.createToken(userName);
    }

}
