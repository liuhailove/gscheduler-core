package com.tc.gschedulercore.core.thread;


import com.tc.gschedulercore.core.conf.JobAdminConfig;
import com.tc.gschedulercore.core.cron.CronExpression;
import com.tc.gschedulercore.core.dto.ESParam;
import com.tc.gschedulercore.core.model.*;
import com.tc.gschedulercore.core.scheduler.ScheduleTypeEnum;
import com.tc.gschedulercore.enums.RetryType;
import com.tc.gschedulercore.util.JobRemotingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Invocable;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tc.gschedulercore.core.thread.JobCheckHelper.SCRIPT_ENGINE;

/**
 * @author honggang.liu 2019-05-21
 */
public class JobESHelper {
    private static Logger logger = LoggerFactory.getLogger(JobESHelper.class.getSimpleName());

    private static JobESHelper instance = new JobESHelper();

    public static JobESHelper getInstance() {
        return instance;
    }

    // pre read
    public static final long PRE_READ_MS = 5000;

    private Thread scheduleESThread;
    private Thread ringThread;
    private volatile boolean scheduleESThreadToStop = false;

    public static final Pattern GS_GET_DATA_FROM_ES_PATTERN = Pattern.compile("(?<=gsGetDataFromES\\(\")(.*?)(?=\",\")(.*?)(?=\"\\))");

    public static final Pattern GS_NOW_TIME_STAMP_PATTERN = Pattern.compile("(?<=gsNowTimeStamp\\s{0,20}\\()[^)]+");
    public static final Pattern GS_NOW_TIME_PATTERN = Pattern.compile("(?<=gsNowTime\\s{0,20}\\()[^)]+");
    private static final int MAX_USED_MEMORY = 80;

    private volatile boolean ringThreadToStop = false;
    private volatile static Map<Integer, List<Long>> ringData = new ConcurrentHashMap<>();

    public void start() {
        // schedule thread
        scheduleESThread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    TimeUnit.MILLISECONDS.sleep(5000 - System.currentTimeMillis() % 1000);
                } catch (InterruptedException e) {
                    if (!scheduleESThreadToStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
                logger.info(" init admin scheduler success.");

                // pre-read count: threadpool-size * trigger-qps (each trigger cost 50ms, qps = 1000/50 = 20)
                int preReadCount = (JobAdminConfig.getAdminConfig().getTriggerPoolFastMax() + JobAdminConfig.getAdminConfig().getTriggerPoolSlowMax()) * 20;

                while (!scheduleESThreadToStop) {

                    // Scan Job
                    long start = System.currentTimeMillis();

                    Connection conn = null;
                    Boolean connAutoCommit = null;
                    PreparedStatement preparedStatement = null;

                    boolean preReadSuc = true;
                    try {
                        conn = JobAdminConfig.getAdminConfig().getDataSource().getConnection();
                        connAutoCommit = conn.getAutoCommit();
                        conn.setAutoCommit(false);

                        preparedStatement = conn.prepareStatement("select * from gs_job_lock where lock_name = 'schedule_es_lock' for update");
                        preparedStatement.execute();

                        // tx start

                        // 1、pre read
                        long nowTime = System.currentTimeMillis();
//                        List<JobInfo> scheduleList = JobAdminConfig.getAdminConfig().getJobInfoDao().scheduleJobQuery(nowTime + PRE_READ_MS, preReadCount);
                        List<AlarmScript> alarmScriptList = JobAdminConfig.getAdminConfig().getAlarmScriptDao().queryByTime(nowTime, preReadCount);

                        if (alarmScriptList != null && !alarmScriptList.isEmpty()) {
                            // 2、push time-ring

                            for (AlarmScript script : alarmScriptList) {
                                double memoryUsedPercentage = isHeapMemoryUsageHigh();
                                //需要判断这个任务是否开启。
                                AlarmRule alarmRule = JobAdminConfig.getAdminConfig().getAlarmRuleDao().load(script.getAlarmRuleId());
                                //如果没有开启，跳过
                                if (!alarmRule.getOpen()){
                                    //如果没有开启，就更新执行时间，跳过这一次，防止一直在刷新
                                    logger.info("JobScriptRetryMonitorHelper es,this alarm rule is closed and skip it,alarmRuleId:{}, scriptRetryLogId={}", alarmRule.getId(), script.getId());
                                    refreshNextValidTime(script, new Date());
                                    continue;
                                }
                                if (memoryUsedPercentage > MAX_USED_MEMORY) {
                                    logger.error("JobScriptRetryMonitorHelper es,used heap memory:{} greater than 80%,delay 1min to process, scriptRetryLogId={}", memoryUsedPercentage, script.getId());
                                    script.setTriggerLastTime(System.currentTimeMillis() + 60 * 1000);
                                    int ret = JobAdminConfig.getAdminConfig().getAlarmScriptDao().updateTriggerNextTime(script);
                                    if (ret <= 0) {
                                        logger.error("JobESHelper memory overload 80,delay es data execute time, scriptRetryLogId={}", script.getId());
                                    }
                                } else {
                                    if (nowTime > script.getTriggerNextTime() + PRE_READ_MS) {
                                        logger.warn(">> schedule es misfire, scriptId = {}", script.getId());
                                        //任务过期，立刻执行
                                        fireESScriptCheck(script);
                                        // 2、fresh next
                                        refreshNextValidTime(script, new Date());
                                    } else if (nowTime > script.getTriggerNextTime()) {
                                        // 2.2、trigger-expire < 5s：direct-trigger && make next-trigger-time

                                        // 1、trigger
                                        fireESScriptCheck(script);
                                        logger.debug(">>schedule push trigger : jobId = {}", script.getId());

                                        // 2、fresh next
                                        refreshNextValidTime(script, new Date());

                                        // next-trigger-time in 5s, pre-read again
                                        if (script.getRetryType() == 4 && nowTime + PRE_READ_MS > script.getTriggerNextTime()) {

                                            // 1、make ring second
                                            int ringSecond = (int) ((script.getTriggerNextTime() / 1000) % 60);

                                            // 2、push time ring
                                            pushTimeRing(ringSecond, script.getId());

                                            // 3、fresh next
                                            refreshNextValidTime(script, new Date(script.getTriggerNextTime()));
                                        }

                                    } else {
                                        // 2.3、trigger-pre-read：time-ring trigger && make next-trigger-time

                                        // 1、make ring second
                                        int ringSecond = (int) ((script.getTriggerNextTime() / 1000) % 60);

                                        // 2、push time ring
                                        pushTimeRing(ringSecond, script.getId());

                                        // 3、fresh next
                                        refreshNextValidTime(script, new Date(script.getTriggerNextTime()));

                                    }

                                }
                            }

                            // 3、update trigger info
                            for (AlarmScript script : alarmScriptList) {
                                JobAdminConfig.getAdminConfig().getAlarmScriptDao().updateTriggerTimeById(script.getId(), script.getTriggerLastTime(), script.getTriggerNextTime());
                            }

                        } else {
                            preReadSuc = false;
                        }

                        // tx stop


                    } catch (Exception e) {
                        if (!scheduleESThreadToStop) {
                            logger.error(">>JobScheduleHelper#scheduleThread error:", e);
                        }
                    } finally {

                        // commit
                        if (conn != null) {
                            try {
                                conn.commit();
                            } catch (SQLException e) {
                                if (!scheduleESThreadToStop) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                            try {
                                conn.setAutoCommit(connAutoCommit);
                            } catch (SQLException e) {
                                if (!scheduleESThreadToStop) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                            try {
                                conn.close();
                            } catch (SQLException e) {
                                if (!scheduleESThreadToStop) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                        }

                        // close PreparedStatement
                        if (null != preparedStatement) {
                            try {
                                preparedStatement.close();
                            } catch (SQLException e) {
                                if (!scheduleESThreadToStop) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                        }
                    }
                    long cost = System.currentTimeMillis() - start;


                    // Wait seconds, align second
                    if (cost < 1000) {  // scan-overtime, not wait
                        try {
                            // pre-read period: success > scan each second; fail > skip this period;
                            TimeUnit.MILLISECONDS.sleep((preReadSuc ? 1000 : PRE_READ_MS) - System.currentTimeMillis() % 1000);
                        } catch (InterruptedException e) {
                            if (!scheduleESThreadToStop) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                    }

                }

                logger.info(">>JobScheduleESHelper#scheduleThread stop");
            }
        });
        scheduleESThread.setDaemon(true);
        scheduleESThread.setName("JobScheduleESHelper#scheduleThread");
        scheduleESThread.start();

        ringThread = new Thread(new Runnable() {
            @Override
            public void run() {

                // align second
                try {
                    TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
                } catch (InterruptedException e) {
                    if (!ringThreadToStop) {
                        logger.error(e.getMessage(), e);
                    }
                }

                while (!ringThreadToStop) {
                    try {
                        // second data
                        List<Long> ringItemData = new ArrayList<>();
                        int nowSecond = Calendar.getInstance().get(Calendar.SECOND);   // 避免处理耗时太长，跨过刻度，向前校验一个刻度；
                        for (int i = 0; i < 2; i++) {
                            List<Long> tmpData = ringData.remove((nowSecond + 60 - i) % 60);
                            if (tmpData != null) {
                                ringItemData.addAll(tmpData);
                            }
                        }

                        // ring trigger
                        logger.debug(">>time-ring beat:{} = {}", nowSecond, Arrays.asList(ringItemData));
                        if (!ringItemData.isEmpty()) {
                            // do trigger
                            for (Long scriptId : ringItemData) {
                                // do trigger
                                AlarmScript alarmScript=JobAdminConfig.getAdminConfig().getAlarmScriptDao().load(scriptId);
                                fireESScriptCheck(alarmScript);
                            }
                            // clear
                            ringItemData.clear();
                        }
                    } catch (Exception e) {
                        if (!ringThreadToStop) {
                            logger.error(">>JobScheduleESHelper#ringThread error:", e);
                        }
                    }

                    // next second, align second
                    try {
                        TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
                    } catch (InterruptedException e) {
                        if (!ringThreadToStop) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
                logger.info(">>JobScheduleESHelper#ringThread stop");
            }
        });
        ringThread.setDaemon(true);
        ringThread.setName("JobScheduleESHelper#ringThread");
        ringThread.start();

    }

    private void refreshNextValidTime(AlarmScript script, Date fromTime) throws Exception {
        Date nextValidTime = generateNextValidTime(script, fromTime);
        if (nextValidTime != null) {
            script.setTriggerLastTime(script.getTriggerNextTime());
            script.setTriggerNextTime(nextValidTime.getTime());
        } else {
            script.setTriggerLastTime(0);
            script.setTriggerNextTime(0);
            logger.warn(">>refreshESNextValidTime fail for job: scriptId={}, scheduleConf={}",
                    script.getId(), script.getCronExp());
        }
    }

    public void fireESScriptCheck(AlarmScript alarmScript) {
        //1、执行es 脚本
        Map<String, Object> valPair = checkJSScriptResult(alarmScript);

        // 2、校验未通过，触发告警
        if (Boolean.FALSE.equals(valPair.get("result"))) {
            AlarmRule alarmRule = JobAdminConfig.getAdminConfig().getAlarmRuleDao().load(alarmScript.getAlarmRuleId());
            // 触发告警
            NotifyInfo notifyInfo = new NotifyInfo();
//            notifyInfo.setApp(jobInfo.getAppName());
            notifyInfo.setAlarmRuleId(alarmScript.getAlarmRuleId());
            notifyInfo.setAlarmName(alarmRule.getAlarmName());
            notifyInfo.setAlarmType(RetryType.CRON_TYPE);
            notifyInfo.setAlarmLevel(alarmRule.getAlarmLevel());
            notifyInfo.setNotifyContent(valPair.get("alarmMsg").toString());
            notifyInfo.setApp("esMonitorAlarm");
            notifyInfo.setNotifyUsers(org.springframework.util.StringUtils
                    .hasLength(alarmRule.getNotifyUsers()) ? alarmRule.getNotifyUsers() : "");
            notifyInfo.setNotifyUrl(alarmRule.getNotifyUrl());
            notifyInfo.setGmtCreate(new Date());
            JobAdminConfig.getAdminConfig().getNotifyInfoDao().save(notifyInfo);
            JobAdminConfig.getAdminConfig().getJobAlarmer().doNotifyAlarm(notifyInfo);
        } else {
            logger.info("check es result is true,scriptId={}", alarmScript.getId());
        }
    }


    private Map<String, Object> checkJSScriptResult(AlarmScript alarmScript) {

        Map<String, Object> valPair = new HashMap<>(4);

        valPair.put("jobLogList", new ArrayList<Long>());

        valPair.put("hasError", false);

        valPair.put("errorMsg", "");

        valPair.put("alarmMsg", "");

        try {


            Matcher gsTimeESMatcher = GS_NOW_TIME_PATTERN.matcher(alarmScript.getAlarmCheckExp());

            Matcher gsTimeStampESMatcher = GS_NOW_TIME_STAMP_PATTERN.matcher(alarmScript.getAlarmCheckExp());


            LocalDateTime now = LocalDateTime.now();
            long currentTimeMillis = System.currentTimeMillis();

            //提供时间方法，供业务使用
            while (gsTimeESMatcher.find()) {
                String group = gsTimeESMatcher.group().trim();
                int num = Integer.parseInt(group);
                LocalDateTime adjustedDate = now.plusMinutes(num);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String date=adjustedDate.format(formatter);
                String replaceData=alarmScript.getAlarmCheckExp().replaceAll("gsNowTime\\s{0,20}\\(\\s{0,20}"+ group + "\\s{0,20}\\)",  "\\\""+date+"\\\"");
                alarmScript.setAlarmCheckExp(replaceData);
            }

            while (gsTimeStampESMatcher.find()) {
                String group = gsTimeStampESMatcher.group().trim();
                long number = Long.parseLong(group);
                long currentLong= currentTimeMillis/1000+number;
                String replaceData=alarmScript.getAlarmCheckExp().replaceAll("gsNowTimeStamp\\s{0,20}\\(\\s{0,20}"+ group + "\\s{0,20}\\)",  currentLong+"");
                alarmScript.setAlarmCheckExp(replaceData);
            }
            //顺序很重要，要先替换时间，再执行这个，否则就会替换不生效
            Matcher gsSQLESMatcher = GS_GET_DATA_FROM_ES_PATTERN.matcher(alarmScript.getAlarmCheckExp());
            //先全部校验sql和参数是否合法，校验完成后再发起请求。优点是：减少无效调用。

            while (gsSQLESMatcher.find()) {

                String group = gsSQLESMatcher.group();

                String[] param = group.split("\",\"\\[");

                String sql = param[0];

                //校验sql是否合法，即是否select limit 格式

                if (!validateSQL(sql)) {

                    valPair.put("hasError", true);

                    valPair.put("errorMsg", "sql invalid");

                    valPair.put("result", false);

                    valPair.put("alarmMsg", "sql invalid");

                    return valPair;

                }
            }
            gsSQLESMatcher.reset(); // 重置 Matcher 对象
            String msg="esReturnData:";
            while (gsSQLESMatcher.find()) {

                String group = gsSQLESMatcher.group();

                String[] param = group.split("\",\"\\[");//用","做分隔符，用, ;都会有无法完全分隔的可能

                String sql = param[0];

                String params_json_array_str ="["+ param[1];

                ESParam esParam = ESParam.buildESParam(sql, params_json_array_str);
                String cug=JobAdminConfig.getAdminConfig().getCugESDomain();
//                ESParam.ESResponse esResult = JobRemotingUtil.postBody4ES(cug, null, JobAdminConfig.getAdminConfig().getProxyAddr(), 30, sql,params_json_array_str, ESParam.ESResponse.class, null);
                ESParam.ESResponse esResult = JobRemotingUtil.postBody4ES(cug, null, JobAdminConfig.getAdminConfig().getProxyAddr(), 30, esParam, ESParam.ESResponse.class, null);
                //
                if (esResult.respCode!=0 || esResult.result.getCommon_result().getCode()!=0){
                    logger.error("checkJSScriptResult post es proxy error:scriptIt:{}, respCode:{},respMsg:{},requestId:{}",alarmScript.getId(),esResult.getRespCode(),esResult.getRespMsg(),esResult.getRequest_id());
                    throw new RuntimeException("The request to the ES service failed. Please contact the on-call developer to handle it");
                }
                // 构建正则表达式
                String regex = "gsGetDataFromES(\"" + group + "\");";

                // 打印正则表达式
                String replaceData=alarmScript.getAlarmCheckExp().replace(regex,  esResult.result.getData());
                msg = esResult.result.getData();
                alarmScript.setAlarmCheckExp(replaceData);
            }

            SCRIPT_ENGINE.eval(alarmScript.getAlarmCheckExp());
            // 计算结果。msg、key、value都用不到，因为是通过sql统计实际执行情况
            valPair.put("result", ((Invocable) SCRIPT_ENGINE).invokeFunction("alarmCheck"));

            SCRIPT_ENGINE.eval(alarmScript.getAlarmMsgExp());
            Object msgResult;
            msgResult = ((Invocable) SCRIPT_ENGINE).invokeFunction("alarmMsg",msg);
            valPair.put("alarmMsg",msgResult.toString());

        } catch (Exception e) {

            logger.error("checkResult error,alarmScriptId={},error={}", alarmScript.getId(), e.getMessage());

            valPair.put("hasError", true);

            valPair.put("errorMsg", e.getMessage());
            valPair.put("alarmMsg", e.getMessage());
            valPair.put("result", false);

        }

        return valPair;

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

    private void pushTimeRing(int ringSecond, Long scriptId) {
        // push async ring
        List<Long> ringItemData = ringData.computeIfAbsent(ringSecond, item -> new ArrayList<>());
        ringItemData.add(scriptId);
        logger.debug(">>schedule push time-ring : {}={}", ringSecond, Arrays.asList(ringItemData));
    }

    public void toStop() {
        // 1、stop schedule
        scheduleESThreadToStop = true;
        try {
            TimeUnit.SECONDS.sleep(1);  // wait
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        if (scheduleESThread.getState() != Thread.State.TERMINATED) {
            // interrupt and wait
            scheduleESThread.interrupt();
            try {
                scheduleESThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }

        // if has ring data
        boolean hasRingData = false;
        if (!ringData.isEmpty()) {
            for (int second : ringData.keySet()) {
                List<Long> tmpData = ringData.get(second);
                if (tmpData != null && !tmpData.isEmpty()) {
                    hasRingData = true;
                    break;
                }
            }
        }
        if (hasRingData) {
            try {
                TimeUnit.SECONDS.sleep(8);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }

        // stop ring (wait job-in-memory stop)
        ringThreadToStop = true;
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        if (ringThread.getState() != Thread.State.TERMINATED) {
            // interrupt and wait
            ringThread.interrupt();
            try {
                ringThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }

        logger.info(">>JobScheduleHelper stop");
    }


    // ---------------------- tools ----------------------
    public static Date generateNextValidTime(AlarmScript script, Date fromTime) throws Exception {
        return new CronExpression(script.getCronExp()).getNextValidTimeAfter(fromTime);
    }

    public static Date generateNextValidTime(JobGroup jobGroup, Date fromTime) throws Exception {
        return new CronExpression(jobGroup.getScheduleConf()).getNextValidTimeAfter(fromTime);
    }

    /**
     * 从起点时间到终点时间计算出有效的时间列表
     *
     * @param jobInfo  任务配置
     * @param fromTime 开始时间[包含]
     * @param toTime   截至时间[不包含]
     * @return 有效时间列表
     */
    public static List<Date> generateValidTimeList(JobInfo jobInfo, Date fromTime, Date toTime) {
        if (fromTime == null || toTime == null) {
            return new ArrayList<>(0);
        }
        if (fromTime.after(toTime)) {
            return new ArrayList<>(0);
        }
        List<Date> validTimeList = new ArrayList<>();
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
        Date validTime;
        if (ScheduleTypeEnum.CRON == scheduleTypeEnum) {
            while (fromTime.before(toTime)) {
                try {
                    validTime = new CronExpression(jobInfo.getScheduleConf()).getNextValidTimeAfter(fromTime);
                    if (validTime.after(toTime)) {
                        break;
                    }
                    validTimeList.add(validTime);
                    fromTime = validTime;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } else if (ScheduleTypeEnum.FIX_RATE == scheduleTypeEnum
            /*|| ScheduleTypeEnum.FIX_DELAY == scheduleTypeEnum*/) {
            while (fromTime.before(toTime)) {
                validTime = new Date(fromTime.getTime() + Integer.parseInt(jobInfo.getScheduleConf()) * 1000L);
                if (validTime.after(toTime)) {
                    break;
                }
                validTimeList.add(validTime);
                fromTime = validTime;
            }
        }
        return validTimeList;
    }
}
