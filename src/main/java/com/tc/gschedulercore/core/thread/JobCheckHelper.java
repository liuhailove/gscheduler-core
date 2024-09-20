package com.tc.gschedulercore.core.thread;

import com.google.common.collect.Sets;
import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.model.*;
import com.tc.gschedulercore.core.route.ExecutorRouteStrategyEnum;
import com.tc.gschedulercore.core.trigger.TriggerTypeEnum;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.util.DateUtil;
import net.sf.ehcache.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * job执行检查，主要是检查实际执行次数与预期执行次数是否一致
 *
 * @author honggang.liu
 */
public class JobCheckHelper {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JobCheckHelper.class.getSimpleName());

    /**
     * 1秒
     */
    private static final long ONE_SECONDS = 1000L;

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
    public static final Pattern GS_COUNT_PATTERN = Pattern.compile("(?<=gsCount\\s{0,20}\\()[^)]+");

    public static final Set<String> BLACKLIST = Sets.newHashSet(
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

    public static final String SLASH_REGEX = "\\s*?//\\s*?";

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


    @SuppressWarnings("PMD.JobWaitParentHelper")
    private ScheduledExecutorService jobCheckPoolExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("jobCheckHelper", true));

    private static JobCheckHelper instance = new JobCheckHelper();

    public static JobCheckHelper getInstance() {
        return instance;
    }

    public void start() {
        jobCheckPoolExecutor.scheduleWithFixedDelay(this::check, 0, 10, TimeUnit.SECONDS);
    }

    /**
     * 检查
     */
    private void check() {
        long currentTimeMills = System.currentTimeMillis();
        // DB锁控制
        Connection conn = null;
        Boolean connAutoCommit = null;
        PreparedStatement preparedStatement = null;
        try {
            conn = JobAdminConfig.getAdminConfig().getDataSource().getConnection();
            connAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            preparedStatement = conn.prepareStatement("select * from gs_job_lock where lock_name = 'job_check_lock' for update");
            preparedStatement.execute();

            List<AlarmRule> alarmRuleList = JobAdminConfig.getAdminConfig().getAlarmRuleDao().findAllOpenAndHasJob();
            if (CollectionUtils.isEmpty(alarmRuleList)) {
                return;
            }
            for (AlarmRule alarmRule : alarmRuleList) {
                if (!StringUtils.hasLength(alarmRule.getJobIdes())) {
                    continue;
                }
                String[] jobIdes = alarmRule.getJobIdes().split(",");
                for (String jobIdStr : jobIdes) {
                    JobInfo jobInfo = JobAdminConfig.getAdminConfig().getJobService().loadByIdCached(Integer.parseInt(jobIdStr));
                    // 没有父任务的情况
                    if (jobInfo == null) {
                        continue;
                    }
                    //如果任务没有打开，认为是子任务。ps：如果一个任务先打开，后关闭，那么这个任务就会排除掉
                    if (jobInfo.getTriggerStatus() != JobInfo.TRIGGER_STATUS_RUNNING) {
                        if (CollectionUtils.isEmpty(jobInfo.getParents())) {
                            continue;
                        }
                        boolean isContinue = true;
                        for (Integer parenJob : jobInfo.getParents()) {
                            JobInfo parenJobInfo = JobAdminConfig.getAdminConfig().getJobService().loadByIdCached(parenJob);
                            if (parenJobInfo != null && parenJobInfo.getTriggerStatus() == JobInfo.TRIGGER_STATUS_RUNNING) {
                                isContinue = false;
                            }
                        }
                        if (isContinue) {
                            continue;
                        }
                    }
                    if (AlarmRule.EXECUTOR_LOG.equals(alarmRule.getResourceType())) {
                        // 任务执行次数检查
                        jobExecuteNumberCheck(currentTimeMills, alarmRule, jobInfo);
                    } else {
//                        // 异步执行日志检查
//                        asyncJobExecuteLogCheck(currentTimeMills, alarmRule, jobInfo);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // commit
            if (conn != null) {
                try {
                    conn.commit();
                } catch (SQLException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                try {
                    conn.setAutoCommit(Boolean.TRUE.equals(connAutoCommit));
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
    }

    /**
     * 任务执行次数检查
     *
     * @param currentTimeMills 当前时间戳
     * @param alarmRule        告警规则
     * @param jobInfo          任务信息
     */
    private void jobExecuteNumberCheck(long currentTimeMills, AlarmRule alarmRule, JobInfo jobInfo) {
        JobCheck jobCheck = JobAdminConfig.getAdminConfig().getJobCheckDao().queryByJob(jobInfo.getJobGroup(), jobInfo.getId());
        if (jobCheck == null) {
            return;
        }
        if (jobCheck.getTriggerFixedRateInMin() <= 0) {
            return;
        }
        long triggerLastTime = jobCheck.getTriggerLastTime();
        if (jobCheck.getTriggerLastTime() <= 0) {
            triggerLastTime = currentTimeMills;
        }
        long triggerNextTime = jobCheck.getTriggerNextTime();
        if (triggerNextTime <= 0) {
            triggerNextTime = currentTimeMills;
        }
        // 如果下次执行时间大于当前时间，说明还不应该开始检查
        if (triggerNextTime > currentTimeMills) {
            return;
        }
        List<Date> dateList = JobScheduleHelper.generateValidTimeList(jobInfo, new Date(triggerLastTime), new Date(triggerNextTime));
        int expectedVal = dateList.size();
        // 此处有可能有误差，因为在策略临时改变时，这个计算是错误的，正确的办法是拿log中的实际分片说进行计算，鉴于发生概率不高，因此可以减少查询，按照如下公式计算
        if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST.name().equals(jobInfo.getExecutorRouteStrategy())) {
            expectedVal = expectedVal * jobInfo.getShardingNum();
        }
        int actualVal = JobAdminConfig.getAdminConfig().getJobLogDao().queryJobExecuteNumber(jobInfo.getJobGroup(), jobInfo.getId(), TriggerTypeEnum.CRON.getValue(),
                new Date(triggerLastTime), new Date(triggerNextTime));
        if (expectedVal != actualVal) {
            // 如果expectedVal > actualVal,有可能是时间向前飘动1S导致的，使triggerNext+1S，重新查询，验证是否一致
            if (expectedVal > actualVal) {
                actualVal = JobAdminConfig.getAdminConfig().getJobLogDao().queryJobExecuteNumber(jobInfo.getJobGroup(), jobInfo.getId(), TriggerTypeEnum.CRON.getValue(),
                        new Date(triggerLastTime), new Date(triggerNextTime + ONE_SECONDS));
            } else {
                // 如果expectedVal < actualVal,有可能是时间向后飘动1S导致的，使triggerNext-1S，重新查询，验证是否一致
                actualVal = JobAdminConfig.getAdminConfig().getJobLogDao().queryJobExecuteNumber(jobInfo.getJobGroup(), jobInfo.getId(), TriggerTypeEnum.CRON.getValue(),
                        new Date(triggerLastTime), new Date(triggerNextTime - ONE_SECONDS));
            }
            if (expectedVal != actualVal) {
                // 发送告警
                NotifyInfo notifyInfo = new NotifyInfo();
                notifyInfo.setApp(jobInfo.getAppName());
                notifyInfo.setJobId(jobInfo.getId());
                notifyInfo.setLogId(-1L);
                notifyInfo.setAlarmRuleId(alarmRule.getId());
                notifyInfo.setAlarmName(alarmRule.getAlarmName());
                notifyInfo.setAlarmType(AlarmItem.LOG_EXPECTED_NOT_MATCH_ACTUAL);
                notifyInfo.setAlarmLevel(alarmRule.getAlarmLevel());
                String notifyContent = I18nUtil.getString("jobinfo_field_id") + ":" + jobInfo.getId() + "\n";
                notifyContent += I18nUtil.getString("jobinfo_field_jobname") + ":" + jobInfo.getJobName() + "\n";
                notifyContent += I18nUtil.getString("expected_run_number") + ":" + expectedVal + "\n";
                notifyContent += I18nUtil.getString("actual_run_number") + ":" + actualVal + "\n";
                notifyContent += I18nUtil.getString("calculator_from") + ":" + DateUtil.formatDateTime(new Date(triggerLastTime)) + "\n";
                notifyContent += I18nUtil.getString("calculator_to") + ":" + DateUtil.formatDateTime(new Date(triggerNextTime)) + "\n";
                notifyInfo.setNotifyContent(notifyContent);
                notifyInfo.setNotifyUsers(alarmRule.getNotifyUsers());
                notifyInfo.setNotifyUrl(alarmRule.getNotifyUrl());
                notifyInfo.setGmtCreate(new Date());
                JobAdminConfig.getAdminConfig().getNotifyInfoDao().save(notifyInfo);
                LOGGER.info("[jobExecuteNumberCheck] trigger alarm,alarmName={},notifyId={}", notifyInfo.getAlarmName(), notifyInfo.getId());

                JobAdminConfig.getAdminConfig().getJobAlarmer().doNotifyAlarm(notifyInfo);
            }
        }
        jobCheck.setTriggerLastTime(currentTimeMills);
        jobCheck.setTriggerNextTime(currentTimeMills + (long) jobCheck.getTriggerFixedRateInMin() * 1000 * 60);
        JobAdminConfig.getAdminConfig().getJobCheckDao().update(jobCheck);
    }

//    /**
//     * 异步执行日志检查
//     *
//     * @param currentTimeMills 当前时间戳
//     * @param alarmRule        告警规则
//     * @param jobInfo          任务信息
//     */
//    private void asyncJobExecuteLogCheck(long currentTimeMills, AlarmRule alarmRule, JobInfo jobInfo) throws ParseException, ScriptException, NoSuchMethodException {
//        // 1.查询关联的告警脚本
//        List<AlarmScriptItem> alarmScriptList = JobAdminConfig.getAdminConfig().getAlarmScriptItemDao().queryByAlarmRuleAndJob(alarmRule.getId(), jobInfo.getId());
//        if (CollectionUtils.isEmpty(alarmScriptList)) {
//            return;
//        }
//        // 2.查询任务关联的最近一条instance
//        for (AlarmScriptItem alarmScript : alarmScriptList) {
//            long triggerLastTime = alarmScript.getTriggerLastTime();
//            if (triggerLastTime <= 0) {
//                triggerLastTime = currentTimeMills;
//            } else {
//                triggerLastTime = alarmScript.getTriggerNextTime();
//            }
//            long triggerNextTime = new CronExpression(alarmScript.getCronExp()).getNextValidTimeAfter(new Date(triggerLastTime)).getTime();
//            // 如果下次执行时间大于当前时间，说明还不应该开始检查
//            if (triggerNextTime > currentTimeMills) {
//                return;
//            }
//            long triggerNextNextTime = new CronExpression(alarmScript.getCronExp()).getNextValidTimeAfter(new Date(triggerNextTime)).getTime();
//            // 说明cron的配置比调度10s间隔更大，这时直接更新下次执行时间
//            if (triggerNextNextTime < currentTimeMills) {
//                triggerNextTime = System.currentTimeMillis();
//            }
//            alarmScript.setTriggerLastTime(triggerLastTime);
//            alarmScript.setTriggerNextTime(triggerNextTime);
//            JobAdminConfig.getAdminConfig().getAlarmScriptItemDao().update(alarmScript);
//            // 3.查找此任务最近一天内的最新一个log
//            JobLog latestJobLog = JobAdminConfig.getAdminConfig().getXxlJobLogDao().findLatestGreatThanLog(jobInfo.getJobGroup(), jobInfo.getId(), DateUtil.addDays(new Date(), -1));
//            if (latestJobLog == null) {
//                continue;
//            }
//            // 已经告警处理过
//            if (latestJobLog.getAlarmStatus() != 0) {
//                continue;
//            }
//
//            LogMetric latestLogMetric = JobAdminConfig.getAdminConfig().getXxlLogMetricDao().findLatestBy(latestJobLog.getId());
//            if (latestLogMetric == null) {
//                continue;
//            }
//            Map<String, Object> valPair = checkResult(jobInfo, alarmScript, latestJobLog, latestLogMetric);
//            // 校验未通过，触发告警
//            if (Boolean.FALSE.equals(valPair.get("result"))) {
//                // 2、fail alarm monitor
//                // 告警状态：0-默认、-1=锁定状态、1-无需告警、2-告警成功、3-告警失败
//                // lock log
//                int lockRet = JobAdminConfig.getAdminConfig().getXxlJobLogDao().updateAlarmStatus(latestJobLog.getId(), latestJobLog.getJobGroup(), 0, -1);
//                if (lockRet < 1) {
//                    return;
//                }
//                // 发送
//                sendAlarmMsg(alarmRule, jobInfo, alarmScript, latestLogMetric, (List<Long>) valPair.get("jobLogList"), (Boolean) valPair.get("hasError"), (String) valPair.get("errorMsg"));
//                // 状态更新
//                JobAdminConfig.getAdminConfig().getXxlJobLogDao().updateAlarmStatus(latestJobLog.getId(), latestJobLog.getJobGroup(), -1, 2);
//            }
//        }
//    }

//    /**
//     * sendAlarmMsg 发送告警消息
//     *
//     * @param alarmRule            告警规则
//     * @param jobInfo              任务信息
//     * @param alarmScript          告警脚本
//     * @param jobLogListByInstance 根据instance查询的log列表
//     * @param latestLogMetric      最近metric
//     */
//    private void sendAlarmMsg(AlarmRule alarmRule, JobInfo jobInfo, AlarmScriptItem alarmScript, LogMetric latestLogMetric, List<Long> jobLogListByInstance, Boolean hasError, String errorMsg) {
//        Object msgResult;
//        if (Boolean.FALSE.equals(hasError)) {
//            try {
//                Matcher gsSumMatcherMsg = GS_SUM_PATTERN.matcher(alarmScript.getAlarmMsgExp());
//                while (gsSumMatcherMsg.find()) {
//                    String group = gsSumMatcherMsg.group();
//                    if (!FIELD_LIST.contains(group.trim())) {
//                        LOGGER.info("gsSum(),gsCount()只能操作[msg,key1,value1,key2,value2,key3,value3]集合中,实际入参为[{}],jobId={}", group, jobInfo.getId());
//                        continue;
//                    }
//                    double sum = gsSum(group.trim(), jobLogListByInstance);
//                    alarmScript.setAlarmMsgExp(alarmScript.getAlarmMsgExp().replaceAll("gsSum\\s{0,20}\\(" + group + "\\)", sum + ""));
//                }
//                SCRIPT_ENGINE.eval(alarmScript.getAlarmMsgExp());
//                // 计算结果
//                msgResult = ((Invocable) SCRIPT_ENGINE).invokeFunction("alarmMsg", latestLogMetric.getMsg(), latestLogMetric.getKey1(), latestLogMetric.getValue1(), latestLogMetric.getKey2(), latestLogMetric.getValue2(), latestLogMetric.getKey3(), latestLogMetric.getValue3());
//            } catch (Exception e) {
//                LOGGER.info("sendAlarmMsg error,alarmScriptId={},error={}", alarmScript.getId(), e.getMessage());
//                msgResult = e.getMessage();
//            }
//        } else {
//            msgResult = errorMsg.length() > 2048 ? errorMsg.substring(0, 2048) : errorMsg;
//        }
//        // 触发告警
//        NotifyInfo notifyInfo = new NotifyInfo();
//        notifyInfo.setApp(jobInfo.getAppName());
//        notifyInfo.setAlarmRuleId(alarmRule.getId());
//        notifyInfo.setAlarmName(alarmRule.getAlarmName());
//        notifyInfo.setAlarmType(AlarmItem.LOG_PROCESS_SCRIPT);
//        notifyInfo.setAlarmLevel(alarmRule.getAlarmLevel());
//        notifyInfo.setNotifyContent(msgResult.toString());
//        notifyInfo.setNotifyUsers(StringUtils
//                .hasLength(alarmRule.getNotifyUsers()) ? alarmRule.getNotifyUsers() : "");
//        notifyInfo.setNotifyUrl(alarmRule.getNotifyUrl());
//        notifyInfo.setGmtCreate(new Date());
//        JobAdminConfig.getAdminConfig().getNotifyInfoDao().save(notifyInfo);
//        JobAdminConfig.getAdminConfig().getJobAlarmer().doNotifyAlarm(notifyInfo);
//    }
//
//    /**
//     * checkResult 结果检查
//     *
//     * @param jobInfo         任务信息
//     * @param alarmScriptItem 告警脚本
//     * @param latestJobLog    最近日志
//     * @param latestLogMetric 最近metric
//     */
//    private Map<String, Object> checkResult(JobInfo jobInfo, AlarmScriptItem alarmScriptItem, JobLog latestJobLog, LogMetric latestLogMetric) {
//        Map<String, Object> valPair = new HashMap<>(4);
//        valPair.put("jobLogList", new ArrayList<Long>());
//        valPair.put("hasError", false);
//        valPair.put("errorMsg", "");
//        // gsSum匹配
//        try {
//            Matcher gsSumMatcher = GS_SUM_PATTERN.matcher(alarmScriptItem.getAlarmCheckExp());
//            while (gsSumMatcher.find()) {
//                String group = gsSumMatcher.group();
//                if (!FIELD_LIST.contains(group.trim())) {
//                    LOGGER.info("gsSum(),gsCount()只能操作[msg,key1,value1,key2,value2,key3,value3]集合中,实际入参为[{}],jobId={}", group, jobInfo.getId());
//                    continue;
//                }
//                List<Long> jobLogList = JobAdminConfig.getAdminConfig().getXxlJobLogDao().queryLogIdes(latestJobLog.getInstanceId(), jobInfo.getId(), jobInfo.getJobGroup());
//                double sum = gsSum(group.trim(), jobLogList);
//                alarmScriptItem.setAlarmCheckExp(alarmScriptItem.getAlarmCheckExp().replaceAll("gsSum\\s{0,20}\\(" + group + "\\)", sum + ""));
//                valPair.put("jobLogList", jobLogList);
//            }
//            SCRIPT_ENGINE.eval(alarmScriptItem.getAlarmCheckExp());
//            // 计算结果
//            valPair.put("result", ((Invocable) SCRIPT_ENGINE).invokeFunction("alarmCheck", latestLogMetric.getMsg(), latestLogMetric.getKey1(), latestLogMetric.getValue1(), latestLogMetric.getKey2(), latestLogMetric.getValue2(), latestLogMetric.getKey3(), latestLogMetric.getValue3()));
//        } catch (Exception e) {
//            LOGGER.info("checkResult error,alarmScriptId={},error={}", alarmScriptItem.getAlarmScriptId(), e.getMessage());
//            valPair.put("hasError", true);
//            valPair.put("errorMsg", e.getMessage());
//            valPair.put("result", false);
//        }
//        return valPair;
//    }
//
//    private double gsSum(String field, List<Long> jobLoges) {
//        if (CollectionUtils.isEmpty(jobLoges)) {
//            return 0L;
//        }
//        List<LogMetric> logMetrics = JobAdminConfig.getAdminConfig().getXxlLogMetricDao().findByLoges(jobLoges);
//        if (CollectionUtils.isEmpty(logMetrics)) {
//            return 0L;
//        }
//        BigDecimal sum = BigDecimal.ZERO;
//        for (LogMetric logMetric : logMetrics) {
//            if (MSG_FIELD.equals(field)) {
//                if (StringUtils.hasLength(logMetric.getMsg())) {
//                    sum = sum.add(new BigDecimal(logMetric.getMsg()));
//                }
//            } else if (KEY1_FIELD.equals(field)) {
//                if (StringUtils.hasLength(logMetric.getKey1())) {
//                    sum = sum.add(new BigDecimal(logMetric.getKey1()));
//                }
//            } else if (VALUE1_FIELD.equals(field)) {
//                if (StringUtils.hasLength(logMetric.getValue1())) {
//                    sum = sum.add(new BigDecimal(logMetric.getValue1()));
//                }
//            } else if (KEY2_FIELD.equals(field)) {
//                if (StringUtils.hasLength(logMetric.getKey2())) {
//                    sum = sum.add(new BigDecimal(logMetric.getKey2()));
//                }
//            } else if (VALUE2_FIELD.equals(field)) {
//                if (StringUtils.hasLength(logMetric.getValue2())) {
//                    sum = sum.add(new BigDecimal(logMetric.getValue2()));
//                }
//            } else if (KEY3_FIELD.equals(field)) {
//                if (StringUtils.hasLength(logMetric.getKey3())) {
//                    sum = sum.add(new BigDecimal(logMetric.getKey3()));
//                }
//            } else if (VALUE3_FIELD.equals(field)) {
//                if (StringUtils.hasLength(logMetric.getValue3())) {
//                    sum = sum.add(new BigDecimal(logMetric.getValue3()));
//                }
//            }
//        }
//        return sum.doubleValue();
//    }


    /**
     * 停止线程
     */
    public void toStop() {
        if (jobCheckPoolExecutor == null) {
            return;
        }
        jobCheckPoolExecutor.shutdown();
        LOGGER.info("JobCheckHelper callbackThreadPool thread  stopped");
    }


//    public static boolean scriptContains(String regex, String source) {
//        Pattern pattern = Pattern.compile(regex);
//        return pattern.matcher(source).find();
//    }

}
