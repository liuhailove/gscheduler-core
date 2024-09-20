package com.tc.gschedulercore.core.alarm;

import com.tc.gschedulercore.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class JobAlarmer implements ApplicationContextAware, InitializingBean {
    private static Logger logger = LoggerFactory.getLogger(JobAlarmer.class.getSimpleName());

    /**
     * 环境信息
     */
    @Value("${spring.profiles.active}")
    private String env;

    private ApplicationContext applicationContext;
    /**
     * 告警实现列表
     */
    private List<JobAlarm> jobAlarmList;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, JobAlarm> serviceBeanMap = applicationContext.getBeansOfType(JobAlarm.class);
        if (serviceBeanMap.size() > 0) {
            jobAlarmList = new ArrayList<>(serviceBeanMap.values());
        }
    }

    /**
     * job alarm
     *
     * @param info   task任务定义
     * @param jobLog job执行日志
     * @return 告警成功或者失败
     */
    public boolean alarm(JobInfo info, JobLog jobLog) {
        boolean result = false;
        if (jobAlarmList != null && !jobAlarmList.isEmpty()) {
            // success means all-success
            result = true;
            for (JobAlarm alarm : jobAlarmList) {
                boolean resultItem = false;
                try {
                    resultItem = alarm.doAlarm(info, jobLog, env);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                if (!resultItem) {
                    result = false;
                }
            }
        }
        return result;
    }

//    public static String sendPostRequest(String url, String requestBody, String sdu, String servicekey) {
//        HttpURLConnection connection = null;
//        try {
//            URL postUrl = new URL(url);
//            String proxyAddr = JobAdminConfig.getAdminConfig().getProxyAddr();
//            if (!StringUtils.isEmpty(proxyAddr)) {
//                String[] arr = proxyAddr.split(":");
//                InetSocketAddress addr = new InetSocketAddress(arr[0], arr.length == 2 ? Integer.parseInt(arr[1]) : 80);
//                // http 代理
//                Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);
//                connection = (HttpURLConnection) postUrl.openConnection(proxy);
//            } else {
//                connection = (HttpURLConnection) postUrl.openConnection();
//            }
//            connection.setRequestMethod("POST");
//            connection.setDoOutput(true);
//            connection.setRequestProperty("Content-Type", "application/json");
//            connection.setRequestProperty("x-sp-sdu", sdu);
//            connection.setRequestProperty("x-sp-servicekey", servicekey);
//
//            // 设置请求体数据
//            byte[] requestBodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
//            try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
//                out.write(requestBodyBytes);
//            }
//
//            int responseCode = connection.getResponseCode();
//            String xSpErrmsg = connection.getHeaderField("X-SP-Errmsg");
//            String xSpError = connection.getHeaderField("X-SP-Error");
//
//            // 打印特定字段的值
//            logger.info("Voice Alarm responseCode: {}, requestBody: {}, xSpErrmsg: {}, X-SP-Error: {}", responseCode, requestBody, xSpErrmsg, xSpError);
//
//            if (responseCode == HttpURLConnection.HTTP_OK) {
//                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
//                    StringBuilder response = new StringBuilder();
//                    String line;
//                    while ((line = reader.readLine()) != null) {
//                        response.append(line);
//                    }
//                    return response.toString();
//                }
//            }
//        } catch (IOException e) {
//            logger.error("Voice Alarm responseCode: {}, requestBody: {}", requestBody, e);
//        } finally {
//            if (connection != null) {
//                connection.disconnect();
//            }
//        }
//        return url;
//    }

    /**
     * jobLog 保存报错告警
     *
     * @param info   task任务定义
     * @param jobLog job执行日志
     * @param msg    错误消息
     * @return 告警成功或者失败
     */
    public boolean saveLogFailAlarm(JobInfo info, JobLog jobLog, String msg) {
        boolean result = false;
        if (jobAlarmList != null && !jobAlarmList.isEmpty()) {
            // success means all-success
            result = true;
            for (JobAlarm alarm : jobAlarmList) {
                boolean resultItem = false;
                try {
                    resultItem = alarm.doSaveLogFailAlarm(info, jobLog, msg, env);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                if (!resultItem) {
                    result = false;
                }
            }
        }
        return result;
    }

    /**
     * job 执行超过阈值告警
     *
     * @param info   task任务定义
     * @param jobLog job执行日志
     * @return 告警成功或者失败
     */
    public boolean thresholdAlarm(JobInfo info, JobLog jobLog) {
        boolean result = false;
        if (jobAlarmList != null && !jobAlarmList.isEmpty()) {
            // success means all-success
            result = true;
            for (JobAlarm alarm : jobAlarmList) {
                boolean resultItem = false;
                try {
                    resultItem = alarm.doThresholdAlarm(info, jobLog, env);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                if (!resultItem) {
                    result = false;
                }
            }
        }
        return result;
    }

    /**
     * job 执行次数异常告警
     *
     * @param info task任务定义
     * @return 告警成功或者失败
     */
    public boolean jobExecTimesExceptionAlarm(JobInfo info, int jobGroupId, int jobExpectedTimes, int jobActualTimes) {
        boolean result = false;
        if (jobAlarmList != null && !jobAlarmList.isEmpty()) {
            // success means all-success
            result = true;
            for (JobAlarm alarm : jobAlarmList) {
                boolean resultItem = false;
                try {
                    resultItem = alarm.doJobExecTimesExceptionAlarm(info, jobGroupId, jobExpectedTimes, jobActualTimes, env);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                if (!resultItem) {
                    result = false;
                }
            }
        }
        return result;
    }


    /**
     * 执行器下线告警
     *
     * @param groupList 执行器列表
     * @return 执行器下线告警
     */
    public boolean groupOfflineAlarm(List<JobGroup> groupList) {
        boolean result = false;
        if (jobAlarmList != null && !jobAlarmList.isEmpty()) {
            // success means all-success
            result = true;
            for (JobAlarm alarm : jobAlarmList) {
                boolean resultItem = false;
                try {
                    resultItem = alarm.doGroupOfflineAlarm(groupList, env);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                if (!resultItem) {
                    result = false;
                }
            }
        }
        return result;
    }

    /**
     * 执行失败统计告警
     *
     * @param alarmList 告警数据
     * @return 执行失败统计告警
     */
    public boolean logFailCountAlarm(List<Map<String, Object>> alarmList) {
        boolean result = false;
        if (jobAlarmList != null && !jobAlarmList.isEmpty()) {
            result = true;
            for (JobAlarm alarm : jobAlarmList) {
                boolean resultItem = false;
                try {
                    resultItem = alarm.doLogFailCountAlarmAlarm(alarmList, env);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                if (!resultItem) {
                    result = false;
                }
            }
        }
        return result;
    }

    /**
     * job 开始执行超过阈值告警
     *
     * @param info     task任务定义
     * @param delayLog 延迟执行日志
     * @return 告警成功或者失败
     */
    public boolean runTaskDelayAlarm(JobInfo info, DelayLog delayLog) {
        boolean result = false;
        if (jobAlarmList != null && !jobAlarmList.isEmpty()) {
            // success means all-success
            result = true;
            for (JobAlarm alarm : jobAlarmList) {
                boolean resultItem = false;
                try {
                    resultItem = alarm.doRunTaskDelayAlarm(info, delayLog, env);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                if (!resultItem) {
                    result = false;
                }
            }
        }
        return result;
    }

    /**
     * job alarm
     *
     * @param info   task任务定义
     * @param jobLog job执行日志
     * @return 告警成功或者失败
     */
    public boolean compensateAlarm(JobInfo info, JobLog jobLog) {
        boolean result = false;
        if (jobAlarmList != null && !jobAlarmList.isEmpty()) {
            // success means all-success
            result = true;
            for (JobAlarm alarm : jobAlarmList) {
                boolean resultItem = false;
                try {
                    resultItem = alarm.doCompensateAlarm(info, jobLog, "任务补偿", env);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                if (!resultItem) {
                    result = false;
                }
            }
        }
        return result;
    }

    /**
     * 告警规则触发告警
     *
     * @param notifyInfo 告警消息
     * @return 成功返回真，否则返回假
     */
    public boolean doNotifyAlarm(NotifyInfo notifyInfo) {
        boolean result = false;
        if (jobAlarmList != null && !jobAlarmList.isEmpty()) {
            // success means all-success
            result = true;
            for (JobAlarm alarm : jobAlarmList) {
                boolean resultItem = false;
                try {
                    resultItem = alarm.doNotifyAlarm(notifyInfo, env);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                if (!resultItem) {
                    result = false;
                }
            }
        }
        return result;
    }
}
