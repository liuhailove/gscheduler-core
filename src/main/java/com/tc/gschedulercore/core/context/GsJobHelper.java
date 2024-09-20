package com.tc.gschedulercore.core.context;

import com.tc.gschedulercore.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

/**
 * helper for go-scheduler
 *
 * @author honggang.liu
 */
public class GsJobHelper {

    // ---------------------- base info ----------------------

    /**
     * current JobId
     *
     * @return
     */
    public static long getJobId() {
        GsJobContext gsJobContext = GsJobContext.getGsJobContext();
        if (gsJobContext == null) {
            return -1;
        }

        return gsJobContext.getJobId();
    }

    /**
     * current JobParam
     *
     * @return
     */
    public static String getJobParam() {
        GsJobContext xxlJobContext = GsJobContext.getGsJobContext();
        if (xxlJobContext == null) {
            return null;
        }

        return xxlJobContext.getJobParam();
    }

    // ---------------------- for log ----------------------

    /**
     * current JobLogFileName
     *
     * @return
     */
    public static String getJobLogFileName() {
        GsJobContext xxlJobContext = GsJobContext.getGsJobContext();
        if (xxlJobContext == null) {
            return null;
        }

        return xxlJobContext.getJobLogFileName();
    }

    // ---------------------- for shard ----------------------

    /**
     * current ShardIndex
     *
     * @return
     */
    public static int getShardIndex() {
        GsJobContext xxlJobContext = GsJobContext.getGsJobContext();
        if (xxlJobContext == null) {
            return -1;
        }

        return xxlJobContext.getShardIndex();
    }

    /**
     * current ShardTotal
     *
     * @return
     */
    public static int getShardTotal() {
        GsJobContext xxlJobContext = GsJobContext.getGsJobContext();
        if (xxlJobContext == null) {
            return -1;
        }

        return xxlJobContext.getShardTotal();
    }

    // ---------------------- tool for log ----------------------

    private static Logger logger = LoggerFactory.getLogger("go-scheduler logger");

    /**
     * append log with pattern
     *
     * @param appendLogPattern   like "aaa {} bbb {} ccc"
     * @param appendLogArguments like "111, true"
     */
    public static boolean log(String appendLogPattern, Object... appendLogArguments) {

        FormattingTuple ft = MessageFormatter.arrayFormat(appendLogPattern, appendLogArguments);
        String appendLog = ft.getMessage();

        /*appendLog = appendLogPattern;
        if (appendLogArguments!=null && appendLogArguments.length>0) {
            appendLog = MessageFormat.format(appendLogPattern, appendLogArguments);
        }*/

        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        return logDetail(callInfo, appendLog);
    }

    /**
     * append exception stack
     *
     * @param e
     */
    public static boolean log(Throwable e) {

        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String appendLog = stringWriter.toString();

        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        return logDetail(callInfo, appendLog);
    }

    /**
     * append log
     *
     * @param callInfo
     * @param appendLog
     */
    private static boolean logDetail(StackTraceElement callInfo, String appendLog) {
        GsJobContext xxlJobContext = GsJobContext.getGsJobContext();
        if (xxlJobContext == null) {
            return false;
        }

        /*// "yyyy-MM-dd HH:mm:ss [ClassName]-[MethodName]-[LineNumber]-[ThreadName] log";
        StackTraceElement[] stackTraceElements = new Throwable().getStackTrace();
        StackTraceElement callInfo = stackTraceElements[1];*/

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(DateUtil.formatDateTime(new Date())).append(" ")
                .append("[" + callInfo.getClassName() + "#" + callInfo.getMethodName() + "]").append("-")
                .append("[" + callInfo.getLineNumber() + "]").append("-")
                .append("[" + Thread.currentThread().getName() + "]").append(" ")
                .append(appendLog != null ? appendLog : "");
        String formatAppendLog = stringBuffer.toString();

        // appendlog
        String logFileName = xxlJobContext.getJobLogFileName();

//        if (logFileName != null && logFileName.trim().length() > 0) {
//            GsJobFileAppender.appendLog(logFileName, formatAppendLog);
//            return true;
//        } else {
//            logger.info(">> {}", formatAppendLog);
//            return false;
//        }
        return true;
    }

    // ---------------------- tool for handleResult ----------------------

    /**
     * handle success
     *
     * @return
     */
    public static boolean handleSuccess() {
        return handleResult(GsJobContext.HANDLE_COCE_SUCCESS, null);
    }

    /**
     * handle success with log msg
     *
     * @param handleMsg
     * @return
     */
    public static boolean handleSuccess(String[] handleMsg) {
        return handleResult(GsJobContext.HANDLE_COCE_SUCCESS, handleMsg);
    }

    /**
     * handle fail
     *
     * @return
     */
    public static boolean handleFail() {
        return handleResult(GsJobContext.HANDLE_COCE_FAIL, null);
    }

    /**
     * handle fail with log msg
     *
     * @param handleMsg
     * @return
     */
    public static boolean handleFail(String handleMsg) {
        return handleResult(GsJobContext.HANDLE_COCE_FAIL, new String[]{handleMsg});
    }

    /**
     * handle timeout
     *
     * @return
     */
    public static boolean handleTimeout() {
        return handleResult(GsJobContext.HANDLE_COCE_TIMEOUT, null);
    }

    /**
     * handle timeout with log msg
     *
     * @param handleMsg
     * @return
     */
    public static boolean handleTimeout(String handleMsg) {
        return handleResult(GsJobContext.HANDLE_COCE_TIMEOUT, new String[]{handleMsg});
    }

    /**
     * @param handleCode 200 : success
     *                   500 : fail
     *                   502 : timeout
     * @param handleMsg
     * @return
     */
    public static boolean handleResult(int handleCode, String[] handleMsg) {
        GsJobContext xxlJobContext = GsJobContext.getGsJobContext();
        if (xxlJobContext == null) {
            return false;
        }

        xxlJobContext.setHandleCode(handleCode);
        if (handleMsg != null) {
            xxlJobContext.setHandleMsg(handleMsg);
        }
        return true;
    }


}
