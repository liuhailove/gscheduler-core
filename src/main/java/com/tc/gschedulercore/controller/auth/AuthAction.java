package com.tc.gschedulercore.controller.auth;

import java.lang.annotation.*;

/**
 * @author honggang.liu
 * @since 1.7.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ElementType.METHOD})
public @interface AuthAction {
    /**
     * @return the target name to control
     */
    String targetName() default "app";

    /**
     * @return the message when permission is denied
     */
    String message() default "Permission denied";
}
