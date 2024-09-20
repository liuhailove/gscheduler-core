package com.tc.gschedulercore.core.thread;

import com.google.common.collect.Sets;
import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.model.*;
import com.tc.gschedulercore.enums.RetryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 脚本告警检查重试任务
 *
 * @author honggang.liu 2015-9-1 18:05:56
 */
public class JobScriptRetryMonitorHelper {
    private static Logger LOGGER = LoggerFactory.getLogger(JobScriptRetryMonitorHelper.class.getSimpleName());

    /**
     * 脚本引擎
     */
    private static ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    /**
     * 脚本引擎
     */
    public static ScriptEngine SCRIPT_ENGINE = scriptEngineManager.getEngineByName("javascript");

    /**
     * gsSum模式匹配
     */
    public static final Pattern GS_SUM_PATTERN = Pattern.compile("(?<=gsSum\\s{0,20}\\()[^)]+");
    // gsCount 模式匹配
    public static final Pattern GS_COUNT_PATTERN = Pattern.compile("(?<=gsCount\\s{0,20}\\()[^)]+");


    public static final Set<String> blacklist = Sets.newHashSet(
            // Java 全限定类名
            "java.io.File", "java.io.RandomAccessFile", "java.io.FileInputStream", "java.io.FileOutputStream",
            "java.lang.Class", "java.lang.ClassLoader", "java.lang.Runtime", "java.lang.System", "System.getProperty",
            "java.lang.Thread", "java.lang.ThreadGroup", "java.lang.reflect.AccessibleObject", "java.net.InetAddress",
            "java.net.DatagramSocket", "java.net.DatagramSocket", "java.net.Socket", "java.net.ServerSocket",
            "java.net.MulticastSocket", "java.net.MulticastSocket", "java.net.URL", "java.net.HttpURLConnection",
            "java.security.AccessControlContext", "java.lang.ProcessBuilder",
            //反射关键字
            "invoke", "newinstance",
            // JavaScript 方法
            "eval", "new function",
            //引擎特性
            "Java.type", "importPackage", "importClass", "JavaImporter"
    );

    private static final String MSG_FIELD = "msg";

    private static final String KEY1_FIELD = "key1";

    private static final String VALUE1_FIELD = "value1";

    private static final String KEY2_FIELD = "key2";

    private static final String VALUE2_FIELD = "value2";

    private static final String KEY3_FIELD = "key3";

    private static final String VALUE3_FIELD = "value3";

    /**
     * 可供选择的列表
     */
    public static final List<String> FIELD_LIST = Arrays.asList(MSG_FIELD, KEY1_FIELD, VALUE1_FIELD, KEY2_FIELD, VALUE2_FIELD, KEY3_FIELD, VALUE3_FIELD);

    private static JobScriptRetryMonitorHelper instance = new JobScriptRetryMonitorHelper();

    public static JobScriptRetryMonitorHelper getInstance() {
        return instance;
    }

    // ---------------------- monitor ----------------------

    private Thread monitorThread;
    private volatile boolean toStop = false;

    /**
     * 预处理个数
     */
    private static final int PRE_READ_COUNT = 50;
    private static final int MAX_USED_MEMORY = 80;


    public void start() {
        monitorThread = new Thread(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(5000 - System.currentTimeMillis() % 1000);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
            long nowTime = 0;
            // monitor
            while (!toStop) {

                Connection conn = null;
                Boolean connAutoCommit = null;
                PreparedStatement preparedStatement = null;
                try {
                    conn = JobAdminConfig.getAdminConfig().getDataSource().getConnection();
                    connAutoCommit = conn.getAutoCommit();
                    conn.setAutoCommit(false);

                    preparedStatement = conn.prepareStatement("select * from gs_job_lock where lock_name = 'report_lock' for update");
                    preparedStatement.execute();
                    // 预加载5s的数据
                    // 1、pre read
                    nowTime = System.currentTimeMillis();
                    List<Integer> jobGroupIdes = JobAdminConfig.getAdminConfig().getJobService().findAllGroupIdCached();
                    for (int jobGroup : jobGroupIdes) {
                        List<Long> scriptRetryLogIds = JobAdminConfig.getAdminConfig().getJobLogScriptDao().findScriptRetryIds(jobGroup, nowTime, PRE_READ_COUNT);
                        if (scriptRetryLogIds != null && !scriptRetryLogIds.isEmpty()) {
                            for (long scriptRetryLogId : scriptRetryLogIds) {
                                LOGGER.info("JobScriptRetryMonitorHelper scriptRetryLogId={}", scriptRetryLogId);
                                JobLogScript log = JobAdminConfig.getAdminConfig().getJobLogScriptDao().load(jobGroup, scriptRetryLogId);
                                // 当前时间>要执行的时间+预读时间或者>要执行的时间，说明已经错过，那么立刻执行
                                if (nowTime >= log.getScriptCheckTriggerTime()) {
                                    double memoryUsedPercentage = isHeapMemoryUsageHigh();
                                    if (memoryUsedPercentage > MAX_USED_MEMORY) {
                                        //如果堆内存使用率超过80%，就延后1min处理。
                                        LOGGER.error("JobScriptRetryMonitorHelper,used heap memory:{} greater than 80%,delay 1min to process, scriptRetryLogId={}", memoryUsedPercentage, scriptRetryLogId);
                                        int ret = JobAdminConfig.getAdminConfig().getJobLogScriptDao().updateScriptTriggerTime(log.getId(), log.getJobGroup(), System.currentTimeMillis() + 60 * 1000);
                                        if (ret <= 0) {
                                            LOGGER.error("JobScriptRetryMonitorHelper,update log script error, scriptRetryLogId={}", scriptRetryLogId);
                                        }
                                    } else {
                                        fireScriptCheck(log);
                                    }
                                }
                                // 休眠
                                TimeUnit.MILLISECONDS.sleep(10 - System.currentTimeMillis() % 10);
                            }
                        }
                        // 休眠
                        TimeUnit.MILLISECONDS.sleep(100 - System.currentTimeMillis() % 100);
                    }

                    //获取type=4的cron表达式数据，共用一个线程，不再单独启动一个线程了。可能会有时延，但是es 本身就有时延，所以应该还可以。todo 后面有时间搞一个单独线程
//                    long nt = System.currentTimeMillis();
//                    List<AlarmScript> alarmScriptList = JobAdminConfig.getAdminConfig().getAlarmScriptDao().queryByTime(nt, PRE_READ_COUNT);
//                    for (AlarmScript as : alarmScriptList) {
//                        double memoryUsedPercentage = isHeapMemoryUsageHigh();
//                        if (memoryUsedPercentage > MAX_USED_MEMORY) {
//                            LOGGER.error("JobScriptRetryMonitorHelper es,used heap memory:{} greater than 80%,delay 1min to process, scriptRetryLogId={}", memoryUsedPercentage, as.getId());
//                            as.setTriggerLastTime(System.currentTimeMillis() + 60 * 1000);
//                            int ret = JobAdminConfig.getAdminConfig().getAlarmScriptDao().updateTriggerNextTime(as);
//                            if (ret <= 0) {
//                                LOGGER.error("JobScriptRetryMonitorHelper es,update log script error, scriptRetryLogId={}", as.getId());
//                            }
//                        } else {
//                            fireESScriptCheck(as);
//                            //todo 更新下一次执行时间
//                        }
//                    }

                } catch (Exception e) {
                    if (!toStop) {
                        LOGGER.error("JobScriptRetryMonitorHelper monitor thread error:", e);
                    }
                } finally {
                    // commit
                    if (conn != null) {
                        try {
                            conn.commit();
                        } catch (SQLException e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                        try {
                            conn.setAutoCommit(connAutoCommit);
                        } catch (SQLException e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                        try {
                            conn.close();
                        } catch (SQLException e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    }

                    // close PreparedStatement
                    if (null != preparedStatement) {
                        try {
                            preparedStatement.close();
                        } catch (SQLException e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    }
                }
                try {
                    long cost = System.currentTimeMillis() - nowTime;
                    // Wait seconds, align second
                    if (cost < 2000) {
                        TimeUnit.MILLISECONDS.sleep(2000 - System.currentTimeMillis() % 1000);
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
            LOGGER.info("job fail monitor thread stop");

        });
        monitorThread.setDaemon(true);
        monitorThread.setName("JobScriptRetryMonitorHelper");
        monitorThread.start();


    }

    /**
     * 触发脚本检查
     *
     * @param jobLogScript log
     */
    public void fireScriptCheck(JobLogScript jobLogScript) {
        LOGGER.info("fireScriptCheck,{}", jobLogScript);
        // script重试次数大于0时才可以检查
        long retryInterval = 0;
        if (jobLogScript.getRetryType() == RetryType.FIX_RATE_TYPE) {
            try {
                retryInterval = Long.parseLong(jobLogScript.getScriptRetryConf());
            } catch (NumberFormatException e) {
                LOGGER.error("scriptId={}，配置{}重试配置不能转换为数字", jobLogScript.getId(), jobLogScript.getScriptRetryConf());
                // 设置为默认值
                retryInterval = 10;
            }
        } else if (jobLogScript.getRetryType() == RetryType.CUSTOMER_TYPE) {
            // 计算下次触发时间，并更新
            String[] confArr = jobLogScript.getScriptRetryConf().split(",");
            // 配置的数组长度<重试次数，全部设置为默认10s
            try {
                // 此处有可能出现索引越界，比如中途修改了jobInfo的失败重试次数，加入原先jobInfo配置为4，生成日志为log的retry也是4，
                // 如果重试开始前修改为2，则2-4索引越界，所以会出错，此种场景只需要按照默认10s重试就可以了
                String conf = confArr[confArr.length - jobLogScript.getScriptRetryCount()];
                retryInterval = Long.parseLong(conf);
            } catch (Exception e) {
                LOGGER.error("job={}，配置{}脚本重试配置不能转换为数字", jobLogScript.getJobId(), jobLogScript.getScriptRetryConf(), e);
                // 设置为默认值
                retryInterval = 10;
            }
        }
        // 更新log，从retryCount更新为retryCount-1，如果失败，则直接continue
        // 备注，因为logLog可能被多个node同时加载，所以此处的更新是一个并发控制
        int ret = JobAdminConfig.getAdminConfig().getJobLogScriptDao().updateScriptRetryCount(jobLogScript.getId(), jobLogScript.getJobGroup(), jobLogScript.getScriptRetryCount(), jobLogScript.getScriptRetryCount() - 1, System.currentTimeMillis() + retryInterval * 1000);
        if (ret <= 0) {
            LOGGER.info("updateScriptRetryCount failed,scriptId={},logId={},groupId={}", jobLogScript.getId(), jobLogScript.getJobLogId(), jobLogScript.getJobGroup());
            return;
        }
        // 查看告警规则配置
        List<AlarmRule> alarmRuleList = JobAdminConfig.getAdminConfig().getAlarmRuleService().findByJobGroupAndJobId(jobLogScript.getJobGroup(), jobLogScript.getJobId());
        if (CollectionUtils.isEmpty(alarmRuleList)) {
            LOGGER.info("no alarmRuleList,scriptId={},logId={},groupId={}", jobLogScript.getId(), jobLogScript.getJobLogId(), jobLogScript.getJobGroup());
            return;
        }
        for (AlarmRule alarmRule : alarmRuleList) {
            // 1.查询关联的告警脚本
            List<AlarmScriptItem> alarmScriptItemList = JobAdminConfig.getAdminConfig().getAlarmRuleService().queryItemsByAlarmRuleAndJob(alarmRule.getId(), jobLogScript.getJobId());
            if (CollectionUtils.isEmpty(alarmScriptItemList)) {
                LOGGER.info("no alarmScriptItemList,scriptId={},logId={},groupId={}", jobLogScript.getId(), jobLogScript.getJobLogId(), jobLogScript.getJobGroup());
                continue;
            }
            // 2.查询任务关联的log
            for (AlarmScriptItem alarmScript : alarmScriptItemList) {
                LogMetric latestLogMetric = JobAdminConfig.getAdminConfig().getLogMetricDao().findLatestBy(jobLogScript.getJobLogId(), jobLogScript.getJobId());
                if (latestLogMetric == null) {
                    LOGGER.error("no latestLogMetric,scriptId={},logId={},groupId={},jobId={}", jobLogScript.getId(), jobLogScript.getJobLogId(), jobLogScript.getJobGroup(), jobLogScript.getJobId());
                    continue;
                }
                JobInfo jobInfo = JobAdminConfig.getAdminConfig().getJobService().loadByIdCached(jobLogScript.getJobId());
                JobLog jobLog = JobAdminConfig.getAdminConfig().getJobLogDao().load(jobLogScript.getJobGroup(), jobLogScript.getJobLogId());
                Map<String, Object> valPair = checkResult(jobInfo, alarmScript, jobLog, latestLogMetric);
                // 校验未通过，触发告警
                if (Boolean.FALSE.equals(valPair.get("result"))) {
                    // 发送
                    sendAlarmMsg(alarmRule, jobInfo, alarmScript, latestLogMetric, (List<Long>) valPair.get("jobLogList"), (Boolean) valPair.get("hasError"), (String) valPair.get("errorMsg"));
                } else {
                    LOGGER.info("check result is true,scriptId={},logId={},groupId={}", jobLogScript.getId(), jobLogScript.getJobLogId(), jobLogScript.getJobGroup());
                }
            }

        }

    }

//    public void fireESScriptCheck(AlarmScript alarmScript) {
//        Map<String, Object> valPair = checkJSScriptResult(alarmScript);
//        // 校验未通过，触发告警
//        if (Boolean.FALSE.equals(valPair.get("result"))) {
//            AlarmRule alarmRule = JobAdminConfig.getAdminConfig().getAlarmRuleDao().load(alarmScript.getAlarmRuleId());
//            // 触发告警
//            NotifyInfo notifyInfo = new NotifyInfo();
////            notifyInfo.setApp(jobInfo.getAppName());
//            notifyInfo.setAlarmRuleId(alarmScript.getAlarmRuleId());
//            notifyInfo.setAlarmName(alarmRule.getAlarmName());
//            notifyInfo.setAlarmType(AlarmItem.LOG_PROCESS_SCRIPT);
//            notifyInfo.setAlarmLevel(alarmRule.getAlarmLevel());
//            notifyInfo.setNotifyContent(alarmScript.toString());
//            notifyInfo.setNotifyUsers(org.springframework.util.StringUtils
//                    .hasLength(alarmRule.getNotifyUsers()) ? alarmRule.getNotifyUsers() : "");
//            notifyInfo.setNotifyUrl(alarmRule.getNotifyUrl());
//            notifyInfo.setGmtCreate(new Date());
//            JobAdminConfig.getAdminConfig().getNotifyInfoDao().save(notifyInfo);
//            JobAdminConfig.getAdminConfig().getJobAlarmer().doNotifyAlarm(notifyInfo);
//        } else {
//            LOGGER.info("check es result is true,scriptId={}", alarmScript.getId());
//        }
//    }

    public void toStop() {
        toStop = true;
        // interrupt and wait
        monitorThread.interrupt();
        try {
            monitorThread.join();
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * sendAlarmMsg 发送告警消息
     *
     * @param alarmRule            告警规则
     * @param jobInfo              任务信息
     * @param alarmScript          告警脚本
     * @param jobLogListByInstance 根据instance查询的log列表
     * @param latestLogMetric      最近metric
     */
    private void sendAlarmMsg(AlarmRule alarmRule, JobInfo jobInfo, AlarmScriptItem alarmScript, LogMetric latestLogMetric, List<Long> jobLogListByInstance, Boolean hasError, String errorMsg) {
        Object msgResult;
        if (Boolean.FALSE.equals(hasError)) {
            try {
                Matcher gsSumMatcherMsg = GS_SUM_PATTERN.matcher(alarmScript.getAlarmMsgExp());
                Matcher gsCountMatcherMsg = GS_COUNT_PATTERN.matcher(alarmScript.getAlarmMsgExp());
                while (gsSumMatcherMsg.find()) {
                    String group = gsSumMatcherMsg.group();
                    String[] parts = group.split(",");//约定数据第一个元素是Field_list中一员
                    if (!FIELD_LIST.contains(parts[0].trim())) {
                        LOGGER.info("gsSum()第一个入参只能操作[msg,key1,value1,key2,value2,key3,value3]集合中,实际入参为[{}],jobId={}", group, jobInfo.getId());
                        continue;
                    }
                    double sum = getSumResult(jobInfo, jobLogListByInstance, group, parts);
                    String reg="gsSum\\s{0,20}\\(" + group + "\\)";
                    alarmScript.setAlarmMsgExp(alarmScript.getAlarmMsgExp().replaceAll(reg, sum + ""));
                }

                while (gsCountMatcherMsg.find()) {
                    String group = gsCountMatcherMsg.group();
                    String[] parts = group.split("=");//约定入参=左边元素是Field_list中一员
                    if (!FIELD_LIST.contains(parts[0].trim())) {
                        LOGGER.info("gsCount()入参=左边只能操作[msg,key1,value1,key2,value2,key3,value3]集合中,实际入参为[{}],jobId={}", group, jobInfo.getId());
                        continue;
                    }
                    double count = getCountResult(jobInfo, jobLogListByInstance, group, parts);
                    alarmScript.setAlarmMsgExp(alarmScript.getAlarmMsgExp().replaceAll("gsCount\\s{0,20}\\(" + group + "\\)", count + ""));
                }
                SCRIPT_ENGINE.eval(alarmScript.getAlarmMsgExp());
                // 计算结果
                msgResult = ((Invocable) SCRIPT_ENGINE).invokeFunction("alarmMsg", latestLogMetric.getMsg(), latestLogMetric.getKey1(), latestLogMetric.getValue1(), latestLogMetric.getKey2(), latestLogMetric.getValue2(), latestLogMetric.getKey3(), latestLogMetric.getValue3());
            } catch (Exception e) {
                LOGGER.info("sendAlarmMsg error,alarmScriptId={},error={}", alarmScript.getId(), e.getMessage());
                msgResult = e.getMessage();
            }
        } else {
            msgResult = errorMsg.length() > 2048 ? errorMsg.substring(0, 2048) : errorMsg;
        }
        // 触发告警
        NotifyInfo notifyInfo = new NotifyInfo();
        notifyInfo.setApp(jobInfo.getAppName());
        notifyInfo.setAlarmRuleId(alarmRule.getId());
        notifyInfo.setAlarmName(alarmRule.getAlarmName());
        notifyInfo.setAlarmType(AlarmItem.LOG_PROCESS_SCRIPT);
        notifyInfo.setAlarmLevel(alarmRule.getAlarmLevel());
        notifyInfo.setNotifyContent(msgResult.toString());
        notifyInfo.setNotifyUsers(StringUtils
                .hasLength(alarmRule.getNotifyUsers()) ? alarmRule.getNotifyUsers() : "");
        notifyInfo.setNotifyUrl(alarmRule.getNotifyUrl());
        notifyInfo.setLogId(latestLogMetric.getJobLogId());
        notifyInfo.setGmtCreate(new Date());
        JobAdminConfig.getAdminConfig().getNotifyInfoDao().save(notifyInfo);
        JobAdminConfig.getAdminConfig().getJobAlarmer().doNotifyAlarm(notifyInfo);
    }

    private double getSumResult(JobInfo jobInfo, List<Long> jobLogListByInstance, String group, String[] parts) {
        double sum = 0;
        if (parts.length == 1) {
            sum = gsSum(parts[0].trim(), "", jobLogListByInstance);
        } else if (parts.length == 2) {
            sum = gsSum(parts[0].trim(), parts[1].trim(), jobLogListByInstance);
        } else {
            LOGGER.error("gsSum(),gsCount()只能操作[msg,key1,value1,key2,value2,key3,value3]集合中,实际入参为[{}],jobId={}", group, jobInfo.getId());
        }
        return sum;
    }

    private long getCountResult(JobInfo jobInfo, List<Long> jobLogListByInstance, String group, String[] parts) {
        long count = 0;
        if (parts.length == 2) {
            count = gsCount(parts[0].trim(), parts[1].trim(), jobLogListByInstance);
        } else {
            LOGGER.error("gsSum(),gsCount()只能操作[msg,key1,value1,key2,value2,key3,value3]集合中,实际入参为[{}],jobId={}", group, jobInfo.getId());
        }

        return count;
    }

    /**
     * checkResult 结果检查
     *
     * @param jobInfo         任务信息
     * @param alarmScriptItem 告警脚本
     * @param latestJobLog    最近日志
     * @param latestLogMetric 最近metric
     */
    private Map<String, Object> checkResult(JobInfo jobInfo, AlarmScriptItem alarmScriptItem, JobLog latestJobLog, LogMetric latestLogMetric) {


        Map<String, Object> valPair = new HashMap<>(4);
        valPair.put("jobLogList", new ArrayList<Long>());
        valPair.put("hasError", false);
        valPair.put("errorMsg", "");
        // gsSum匹配
        try {
            //判断是否有恶意代码
            String removeComment = org.apache.commons.lang3.StringUtils.replacePattern(alarmScriptItem.getAlarmCheckExp(), "(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)", "");
            String finalCode = org.apache.commons.lang3.StringUtils.replacePattern(removeComment, "\\s+", " ");
            Set<String> insecure = blacklist.stream().filter(s -> org.apache.commons.lang3.StringUtils.containsIgnoreCase(finalCode, s))
                    .collect(Collectors.toSet());
            if (!CollectionUtils.isEmpty(insecure)) {
                LOGGER.error("Warning! input string is insecure,jobId:{},latestLogMetric:{}", jobInfo.getId(), latestLogMetric.getId());
                throw new Exception("Warning! input string is insecure");

            }

            Matcher gsSumMatcher = GS_SUM_PATTERN.matcher(alarmScriptItem.getAlarmCheckExp());
            Matcher gsCountMatcher = GS_COUNT_PATTERN.matcher(alarmScriptItem.getAlarmCheckExp());
            while (gsSumMatcher.find()) {
                String group = gsSumMatcher.group();
                String[] parts = group.split(",");//约定数据第一个元素是Field_list中一员
                if (!FIELD_LIST.contains(parts[0].trim())) {
                    LOGGER.info("gsSum()第一个入参只能操作[msg,key1,value1,key2,value2,key3,value3]集合中,实际入参为[{}],jobId={}", group, jobInfo.getId());
                    continue;
                }
                List<Long> jobLogList = JobAdminConfig.getAdminConfig().getJobLogDao().queryLogIdes(latestJobLog.getInstanceId(), jobInfo.getId(), jobInfo.getJobGroup());
                double sum = getSumResult(jobInfo, jobLogList, group, parts);
                alarmScriptItem.setAlarmCheckExp(alarmScriptItem.getAlarmCheckExp().replaceAll("gsSum\\s{0,20}\\(" + group + "\\)", sum + ""));
                valPair.put("jobLogList", jobLogList);
            }
            while (gsCountMatcher.find()) {
                String group = gsCountMatcher.group();
                String[] parts = group.split("=");//约定数据第一个元素是Field_list中一员，如果是排除指定errcode，怎么写？
                if (!FIELD_LIST.contains(parts[0].trim())) {
                    LOGGER.info("gsCount()入参=左边只能操作[msg,key1,value1,key2,value2,key3,value3]集合中,实际入参为[{}],jobId={}", group, jobInfo.getId());
                    continue;
                }
                List<Long> jobLogList = JobAdminConfig.getAdminConfig().getJobLogDao().queryLogIdes(latestJobLog.getInstanceId(), jobInfo.getId(), jobInfo.getJobGroup());
                long count = getCountResult(jobInfo, jobLogList, group, parts);
                alarmScriptItem.setAlarmCheckExp(alarmScriptItem.getAlarmCheckExp().replaceAll("gsCount\\s{0,20}\\(" + group + "\\)", count + ""));
                valPair.put("jobLogList", jobLogList);
            }

            SCRIPT_ENGINE.eval(alarmScriptItem.getAlarmCheckExp());
            // 计算结果
            valPair.put("result", ((Invocable) SCRIPT_ENGINE).invokeFunction("alarmCheck", latestLogMetric.getMsg(), latestLogMetric.getKey1(), latestLogMetric.getValue1(), latestLogMetric.getKey2(), latestLogMetric.getValue2(), latestLogMetric.getKey3(), latestLogMetric.getValue3()));
        } catch (Exception e) {
            LOGGER.info("checkResult error,alarmScriptId={},error={}", alarmScriptItem.getAlarmScriptId(), e.getMessage());
            valPair.put("hasError", true);
            valPair.put("errorMsg", e.getMessage());
            valPair.put("result", false);
        }
        return valPair;
    }

//    private Map<String, Object> checkESResult(AlarmScriptItem alarmScriptItem) {
//
//
//        Map<String, Object> valPair = new HashMap<>(4);
//        valPair.put("jobLogList", new ArrayList<Long>());
//        valPair.put("hasError", false);
//        valPair.put("errorMsg", "");
//        // gsSum匹配
//        try {
//            //判断是否有恶意代码
//            String removeComment = org.apache.commons.lang3.StringUtils.replacePattern(alarmScriptItem.getAlarmCheckExp(), "(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)", "");
//            String finalCode = org.apache.commons.lang3.StringUtils.replacePattern(removeComment, "\\s+", " ");
//            Set<String> insecure = blacklist.stream().filter(s -> org.apache.commons.lang3.StringUtils.containsIgnoreCase(finalCode, s))
//                    .collect(Collectors.toSet());
//            if (!CollectionUtils.isEmpty(insecure)) {
////                LOGGER.error("Warning! input string is insecure,jobId:{},latestLogMetric:{}", jobInfo.getId(), latestLogMetric.getId());
//                throw new Exception("Warning! input string is insecure");
//
//            }
//
//            Matcher gsESCountMatcher = GS_GET_DATA_FROM_ES_PATTERN.matcher(alarmScriptItem.getAlarmCheckExp());
//
//            while (gsESCountMatcher.find()) {
//                String group = gsESCountMatcher.group();
//                String[] parts = group.split("\",\"");//用",做分隔符，用, ;都会有无法完全分隔的可能
//                if (!validateSQL(parts[0])) {
//                    LOGGER.error("gsGetDataFromES() sql不合法:{}", group);
//                    continue;
//                }
////                List<Long> jobLogList = JobAdminConfig.getAdminConfig().getJobLogDao().queryLogIdes(latestJobLog.getInstanceId(), jobInfo.getId(), jobInfo.getJobGroup());
////                long count = getCountResult(jobInfo, jobLogList, group, parts);
////                alarmScriptItem.setAlarmCheckExp(alarmScriptItem.getAlarmCheckExp().replaceAll("gsCount\\s{0,20}\\(" + group + "\\)", count + ""));
////                valPair.put("jobLogList", jobLogList);
//            }
//
//            SCRIPT_ENGINE.eval(alarmScriptItem.getAlarmCheckExp());
//            // 计算结果
////            valPair.put("result", ((Invocable) SCRIPT_ENGINE).invokeFunction("alarmCheck", latestLogMetric.getMsg(), latestLogMetric.getKey1(), latestLogMetric.getValue1(), latestLogMetric.getKey2(), latestLogMetric.getValue2(), latestLogMetric.getKey3(), latestLogMetric.getValue3()));
//        } catch (Exception e) {
//            LOGGER.info("checkResult error,alarmScriptId={},error={}", alarmScriptItem.getAlarmScriptId(), e.getMessage());
//            valPair.put("hasError", true);
//            valPair.put("errorMsg", e.getMessage());
//            valPair.put("result", false);
//        }
//        return valPair;
//    }

    private double gsSum(String field, String value, List<Long> jobLoges) {
        if (CollectionUtils.isEmpty(jobLoges)) {
            return 0L;
        }
        int pageSize = 1000;
        int pageNum = 1;
        BigDecimal sum = BigDecimal.ZERO;

        while (true) {
            int offset = (pageNum - 1) * pageSize;
            List<LogMetric> pageLogMetrics = JobAdminConfig.getAdminConfig().getLogMetricDao().findByLoges(jobLoges, offset, pageSize);

            pageNum++;

            for (LogMetric logMetric : pageLogMetrics) {
                //如果value 不为空，说明业务需要根据value 获取指定的上报数据msg，so 需要比较logMetric 的Msg 是否和value 一致
                //如果一致，则进行sum 计算，否则不计算
                if (value.isEmpty()) {
                    sum = getBigDecimal(field, sum, logMetric);
                } else {
                    String[] list = value.split("=");
                    if (list.length != 2) {
                        break;
                    }
                    Map<String, String> fieldMap = new HashMap<>();
                    fieldMap.put(MSG_FIELD, logMetric.getMsg());
                    fieldMap.put(KEY1_FIELD, logMetric.getKey1());
                    fieldMap.put(VALUE1_FIELD, logMetric.getValue1());
                    fieldMap.put(KEY2_FIELD, logMetric.getKey2());
                    fieldMap.put(VALUE2_FIELD, logMetric.getValue2());
                    fieldMap.put(KEY3_FIELD, logMetric.getKey3());
                    fieldMap.put(VALUE3_FIELD, logMetric.getValue3());

                    String logMetricsFieldValue = fieldMap.get(list[0]);//得到db 存储的value 值
                    String inputFieldValue = list[1]; //得到脚本输入的value 值
                    if (StringUtils.hasLength(logMetricsFieldValue) && StringUtils.hasLength(inputFieldValue) && inputFieldValue.equals(logMetricsFieldValue)) {
                        sum = sum.add(new BigDecimal(fieldMap.get(field)));
                    }

                }
            }
            if (pageLogMetrics.size() < pageSize) {
                break;
            }
        }
        return sum.doubleValue();
    }

    private long gsCount(String field, String value, List<Long> jobLoges) {
        if (CollectionUtils.isEmpty(jobLoges)) {
            return 0;
        }
        int pageSize = 1000;
        int pageNum = 1;
        long count = 0;

        while (true) {
            int offset = (pageNum - 1) * pageSize;
            List<LogMetric> pageLogMetrics = JobAdminConfig.getAdminConfig().getLogMetricDao().findByLoges(jobLoges, offset, pageSize);
            pageNum++;

            for (LogMetric logMetric : pageLogMetrics) {
                count += getCountValue(field, value, logMetric);
            }
            if (pageLogMetrics.size() < pageSize) {
                break;
            }
        }
        return count;
    }

    private BigDecimal getBigDecimal(String field, BigDecimal sum, LogMetric logMetric) {
        if (MSG_FIELD.equals(field)) {
            if (StringUtils.hasLength(logMetric.getMsg())) {
                sum = sum.add(new BigDecimal(logMetric.getMsg()));
            }
        } else if (KEY1_FIELD.equals(field)) {
            if (StringUtils.hasLength(logMetric.getKey1())) {
                sum = sum.add(new BigDecimal(logMetric.getKey1()));
            }
        } else if (VALUE1_FIELD.equals(field)) {
            if (StringUtils.hasLength(logMetric.getValue1())) {
                sum = sum.add(new BigDecimal(logMetric.getValue1()));
            }
        } else if (KEY2_FIELD.equals(field)) {
            if (StringUtils.hasLength(logMetric.getKey2())) {
                sum = sum.add(new BigDecimal(logMetric.getKey2()));
            }
        } else if (VALUE2_FIELD.equals(field)) {
            if (StringUtils.hasLength(logMetric.getValue2())) {
                sum = sum.add(new BigDecimal(logMetric.getValue2()));
            }
        } else if (KEY3_FIELD.equals(field)) {
            if (StringUtils.hasLength(logMetric.getKey3())) {
                sum = sum.add(new BigDecimal(logMetric.getKey3()));
            }
        } else if (VALUE3_FIELD.equals(field)) {
            if (StringUtils.hasLength(logMetric.getValue3())) {
                sum = sum.add(new BigDecimal(logMetric.getValue3()));
            }
        }
        return sum;
    }

    private long getCountValue(String field, String value, LogMetric logMetric) {
        Map<String, String> fieldMap = new HashMap<>();
        fieldMap.put(MSG_FIELD, logMetric.getMsg());
        fieldMap.put(KEY1_FIELD, logMetric.getKey1());
        fieldMap.put(VALUE1_FIELD, logMetric.getValue1());
        fieldMap.put(KEY2_FIELD, logMetric.getKey2());
        fieldMap.put(VALUE2_FIELD, logMetric.getValue2());
        fieldMap.put(KEY3_FIELD, logMetric.getKey3());
        fieldMap.put(VALUE3_FIELD, logMetric.getValue3());

        String fieldValue = fieldMap.get(field);
        if (StringUtils.hasLength(fieldValue) && StringUtils.hasLength(value) && value.equals(fieldValue)) {
            return 1;
        }
        return 0;
    }


    public static boolean validateSQL(String sql) {
        // 正则表达式匹配模式
        String pattern = "(?i).*\\bLIMIT\\s+(\\d+)\\b.*";

        // 编译正则表达式
        Pattern p = Pattern.compile(pattern);

        // 匹配输入的SQL
        Matcher m = p.matcher(sql);

        // 如果匹配成功，进一步校验LIMIT后的数字
        if (m.find()) {
            String limitValue = m.group(1);  // 获取LIMIT后的数字
            int limit = Integer.parseInt(limitValue);  // 转换为整数
            return limit <= 10000;  // 判断数字是否小于等于10000
        }

        return false;  // 不包含LIMIT关键字或数字不符合条件
    }

    public static double isHeapMemoryUsageHigh() {
        // 获取内存管理的MXBean
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        // 获取堆内存使用情况
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();

        // 获取已使用内存和最大内存
        long usedMemory = heapMemoryUsage.getUsed();
        long maxMemory = heapMemoryUsage.getMax();

        // 返回内存使用率
        return ((double) usedMemory / maxMemory) * 100;
    }

}
