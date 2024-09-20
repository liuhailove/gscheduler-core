package com.tc.gschedulercore.core.model;

import java.io.Serializable;

/**
 * 系统用户
 *
 * @author honggang.liu
 */
public class LoginUser implements Serializable {
    /**
     * 账号
     */
    private String username;
    /**
     * 密码
     */
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
