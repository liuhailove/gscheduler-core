package com.tc.gschedulercore.core.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
    public static String getToday() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }
}
