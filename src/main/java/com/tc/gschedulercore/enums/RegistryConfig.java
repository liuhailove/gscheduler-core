package com.tc.gschedulercore.enums;

/**
 * Created by honggang.liu on 17/5/10.
 */
public class RegistryConfig {

    public static final int BEAT_TIMEOUT = 10;
    public static final int DEAD_TIMEOUT = BEAT_TIMEOUT * 3;

    public enum RegistType{ EXECUTOR, ADMIN }

}
