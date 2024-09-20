package com.tc.gschedulercore.core.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 *
 * @author honggang.liu
 */
public class StringUtils {
    public static final String CHINESE_PATTERN="[\u4E00-\u9FA5|\\！|\\，|\\。|\\（|\\）|\\《|\\》|\\“|\\”|\\？|\\：|\\；|\\【|\\】]";
    /**
     * 字符串是否包含中文
     *
     * @param str 待校验字符串
     * @return true 包含中文字符 false 不包含中文字符
     */
    public static boolean isContainChinese(String str) {

        if (str == null || str.trim().length() == 0) {
            return false;
        }
        Pattern p = Pattern.compile(CHINESE_PATTERN);
        Matcher m = p.matcher(str);
        if (m.find()) {
            return true;
        }
        return false;
    }
}
