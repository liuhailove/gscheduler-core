package com.tc.gschedulercore.controller.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * OAuth 用户服务类,
 * 这个类封装了 OAuth2User 类的实例，
 * 在 OAuth 认证成功后，Spring OAuth 会传递这个实例。
 * 这里的 getName() 和 getEmail() 方法分别返回用户名和电子邮件。
 *
 * @author honggang.liu
 */
public class CustomOAuth2User implements OAuth2User {
    /**
     * OAuth2User
     */
    private OAuth2User oauth2User;

    public CustomOAuth2User(OAuth2User oauth2User) {
        this.oauth2User = oauth2User;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return oauth2User.getAttribute("name");
    }

    public String getEmail() {
        return oauth2User.getAttribute("email");
    }
}
