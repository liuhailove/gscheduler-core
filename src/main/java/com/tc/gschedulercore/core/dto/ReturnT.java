package com.tc.gschedulercore.core.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * common return
 *
 * @param <T>
 * @author honggang.liu
 */
public class ReturnT<T> implements Serializable {
    public static final long serialVersionUID = 42L;

    public static final int SUCCESS_CODE = 200;
    public static final boolean SUCCESS_FLAG = true;
    public static final int PROCESSING_CODE = 201;
    public static final int FAIL_CODE = 500;

    public static final ReturnT<String> SUCCESS = new ReturnT<>(null);
    public static final ReturnT<String> FAIL = new ReturnT<>(FAIL_CODE, null);
    public static final ReturnT<String> FAIL_FOR_NOT_AUTH = new ReturnT<>(FAIL_CODE, "未授权");


    private int code;
    private boolean success;
    private String msg;
    private T content;


    public T getDod() {
        return dod;
    }

    public void setDod(T dod) {
        this.dod = dod;
    }

    private T dod;  //为什么一定变量名要是dod才可以反序列化成功，Dod、DoD都不可以


    public ReturnT() {
    }

    public ReturnT(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public ReturnT(int code, boolean flag, String msg) {
        this.code = code;
        this.success = flag;
        this.msg = msg;
    }

    public ReturnT(T content) {
        this.code = SUCCESS_CODE;
        this.content = content;
        this.success = true;
    }

    public static <R> ReturnT<R> ofSuccess() {
        ReturnT<R> returnT = new ReturnT<R>();
        returnT.setSuccess(true);
        returnT.setMsg("success");
        returnT.setCode(SUCCESS_CODE);
        returnT.setContent(null);
        return returnT;
    }

    public static <R> ReturnT<R> ofSuccess(R data) {
        ReturnT<R> returnT = new ReturnT<R>();
        returnT.setSuccess(true);
        returnT.setMsg("success");
        returnT.setCode(SUCCESS_CODE);
        returnT.setContent(data);
        return returnT;
    }

    public static <R> ReturnT<R> ofSuccessMsg(String msg) {
        ReturnT<R> returnT = new ReturnT<R>();
        returnT.setSuccess(true);
        returnT.setMsg(msg);
        returnT.setCode(SUCCESS_CODE);
        returnT.setContent(null);
        return returnT;
    }

    public static <R> ReturnT<R> ofFail() {
        ReturnT<R> result = new ReturnT<>();
        result.setSuccess(false);
        result.setCode(FAIL_CODE);
        result.setMsg(null);
        return result;
    }

    public static <R> ReturnT<R> ofFail(int code, String msg) {
        ReturnT<R> result = new ReturnT<>();
        result.setSuccess(false);
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }

    public static <R> ReturnT<R> ofFail(String msg) {
        ReturnT<R> result = new ReturnT<>();
        result.setSuccess(false);
        result.setCode(FAIL_CODE);
        result.setMsg(msg);
        return result;
    }

    public static <R> Map<String, Object> ofMap(List<R> data, int count) {
        // package result
        Map<String, Object> maps = new HashMap<>(4);
        // 分页列表
        maps.put("data", data);
        // 消息
        maps.put("msg", "success");
        maps.put("count", count);
        maps.put("code", 200);
        return maps;
    }

    public static <R> ReturnT<R> ofThrowable(int code, Throwable throwable) {
        ReturnT<R> result = new ReturnT<>();
        result.setSuccess(false);
        result.setCode(code);
        result.setMsg(throwable.getClass().getName() + ", " + throwable.getMessage());
        return result;
    }


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

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "ReturnT [code=" + code + ", msg=" + msg + ", content=" + content + "]";
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

}
