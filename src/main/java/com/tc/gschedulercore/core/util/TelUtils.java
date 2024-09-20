package com.tc.gschedulercore.core.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 电话单元类
 *
 * @author honggang.liu
 */
public class TelUtils {
    /**
     * 电话的正则表达式
     */
    private static final String TEL_REGEX = "^1[3456789]\\d{9}$";

    public static boolean isValidPhoneNumber(String phoneNumber) {
        // 使用正则表达式匹配手机号码格式
        Pattern pattern = Pattern.compile(TEL_REGEX);
        Matcher matcher = pattern.matcher(phoneNumber);
        // 如果匹配成功，返回true；否则，返回false
        return matcher.matches();
    }
}
