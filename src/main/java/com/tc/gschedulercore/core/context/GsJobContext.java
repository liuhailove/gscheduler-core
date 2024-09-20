package com.tc.gschedulercore.core.context;

/**
 * go-scheduler context
 *
 * @author honggang.liu 2020-05-21
 */
public class GsJobContext {

    public static final int HANDLE_COCE_SUCCESS = 200;
    public static final int HANDLE_COCE_FAIL = 500;
    public static final int HANDLE_COCE_TIMEOUT = 502;

    // ---------------------- base info ----------------------

    /**
     * job id
     */
    private final long jobId;

    /**
     * job param
     */
    private final String jobParam;

    // ---------------------- for log ----------------------

    /**
     * job log filename
     */
    private final String jobLogFileName;

    // ---------------------- for shard ----------------------

    /**
     * shard index
     */
    private final int shardIndex;

    /**
     * shard total
     */
    private final int shardTotal;

    // ---------------------- for handle ----------------------

    /**
     * handleCode：The result status of job execution
     * <p>
     * 200 : success
     * 500 : fail
     * 502 : timeout
     */
    private int handleCode;

    /**
     * handleMsg：The simple log msg of job execution
     */
    private String[] handleMsg;


    public GsJobContext(long jobId, String jobParam, String jobLogFileName, int shardIndex, int shardTotal) {
        this.jobId = jobId;
        this.jobParam = jobParam;
        this.jobLogFileName = jobLogFileName;
        this.shardIndex = shardIndex;
        this.shardTotal = shardTotal;
        // default success
        this.handleCode = HANDLE_COCE_SUCCESS;
    }

    public long getJobId() {
        return jobId;
    }

    public String getJobParam() {
        return jobParam;
    }

    public String getJobLogFileName() {
        return jobLogFileName;
    }

    public int getShardIndex() {
        return shardIndex;
    }

    public int getShardTotal() {
        return shardTotal;
    }

    public void setHandleCode(int handleCode) {
        this.handleCode = handleCode;
    }

    public int getHandleCode() {
        return handleCode;
    }

    public void setHandleMsg(String[] handleMsg) {
        this.handleMsg = handleMsg;
    }

    public String[] getHandleMsg() {
        return handleMsg;
    }

    // ---------------------- tool ----------------------

    /**
     * // support for child thread of job handler
     */
    private static InheritableThreadLocal<GsJobContext> contextHolder = new InheritableThreadLocal<>();

    public static void setGsJobContext(GsJobContext gsJobContext) {
        contextHolder.set(gsJobContext);
    }

    public static GsJobContext getGsJobContext() {
        return contextHolder.get();
    }

}