package com.tc.gschedulercore.core.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * ES告警参数
 *
 * @author honggang.liu
 */
public class ESParam implements Serializable {

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getParamsJsonArrayStr() {
        return paramsJsonArrayStr;
    }

    public void setParamsJsonArrayStr(String paramsJsonArrayStr) {
        this.paramsJsonArrayStr = paramsJsonArrayStr;
    }

    @SerializedName("sql")
    private String sql;

    @SerializedName("params_json_array_str")
    private String paramsJsonArrayStr;


    public static ESParam buildESParam(String sql, String paramsJsonArrayStr) {
        ESParam esParam = new ESParam();
        esParam.setParamsJsonArrayStr(paramsJsonArrayStr);
        esParam.setSql(sql);
        return esParam;
    }

    public static class ESResponse {

        public ESResponse(int respCode, String respMsg, String request_id, Result result) {
            this.respCode = respCode;
            this.respMsg = respMsg;
            this.request_id = request_id;
            this.result = result;
        }

        @SerializedName("code")
        public int respCode;

        @SerializedName("msg")
        public String respMsg;

        @SerializedName("request_id")
        public String request_id;

        @SerializedName("result")
        public Result result;

        public int getRespCode() {
            return respCode;
        }

        public void setRespCode(int respCode) {
            this.respCode = respCode;
        }

        public String getRespMsg() {
            return respMsg;
        }

        public void setRespMsg(String respMsg) {
            this.respMsg = respMsg;
        }

        public String getRequest_id() {
            return request_id;
        }

        public void setRequest_id(String request_id) {
            this.request_id = request_id;
        }

        public Result getResult() {
            return result;
        }

        public void setResult(Result result) {
            this.result = result;
        }


    }

    // Inner class for Result
    public static class Result {
        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public CommonResult getCommon_result() {
            return common_result;
        }

        public void setCommon_result(CommonResult common_result) {
            this.common_result = common_result;
        }

        @SerializedName("data")
        private String data;
        @SerializedName("common_result")
        private CommonResult common_result;


    }

    // Inner class for CommonResult
    public static class CommonResult {


        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        private int code;

        private String msg;


    }
}



