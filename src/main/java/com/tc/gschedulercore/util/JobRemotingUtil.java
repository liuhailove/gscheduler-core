package com.tc.gschedulercore.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tc.gschedulercore.core.dto.ESParam;
import com.tc.gschedulercore.core.dto.ReturnT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author honggang.liu 2018-11-25 00:55:31
 */
public class JobRemotingUtil {
    private static Logger logger = LoggerFactory.getLogger(JobRemotingUtil.class.getSimpleName());
    public static final String JOB_ACCESS_TOKEN = "go-scheduler-ACCESS-TOKEN";
    // 最大重拾次数
    private static final int MAX_RETRY_NUM = 3;

    // trust-https start
    private static void trustAllHosts(HttpsURLConnection connection) {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLSocketFactory newFactory = sc.getSocketFactory();

            connection.setSSLSocketFactory(newFactory);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        connection.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
    }

    private static final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }
    }};

    public static ReturnT postBody(String url, String accessToken, String proxyAddr, int timeout, Object requestObj, Class returnTargClassOfT) {
        return postBody(url, accessToken, proxyAddr, timeout, requestObj, returnTargClassOfT, null);
    }

    /**
     * post
     *
     * @param url                请求URL
     * @param accessToken        访问Token
     * @param timeout            超时时间
     * @param requestObj         请求体
     * @param returnTargClassOfT 目标类型
     * @return
     */
    public static ReturnT postBody(String url, String accessToken, String proxyAddr, int timeout, Object requestObj, Class returnTargClassOfT, Map<String, String> additionalProperties) {
        HttpURLConnection connection = null;
        BufferedReader bufferedReader = null;
        Boolean printRespHeader = false;

        String message = "";
        for (int i = 0; i < MAX_RETRY_NUM; i++) {
            long start = System.currentTimeMillis();
            try {
                // connection
                URL realUrl = new URL(url);
                if (!StringUtils.isEmpty(proxyAddr)) {
                    String[] arr = proxyAddr.split(":");
                    InetSocketAddress addr = new InetSocketAddress(arr[0], arr.length == 2 ? Integer.parseInt(arr[1]) : 80);
                    // http 代理
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);
                    connection = (HttpURLConnection) realUrl.openConnection(proxy);
                } else {
                    connection = (HttpURLConnection) realUrl.openConnection();
                }
                // trust-https
                boolean useHttps = url.startsWith("https");
                if (useHttps) {
                    HttpsURLConnection https = (HttpsURLConnection) connection;
                    trustAllHosts(https);
                }
                // connection setting
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setUseCaches(false);
                connection.setReadTimeout(timeout * 1000);
                connection.setConnectTimeout(30 * 1000);
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("Keep-Alive", "timeout=5, max=100");
                connection.setRequestProperty("Content-Type", "application/json");
//                connection.setRequestProperty("Accept-Charset", "application/json;charset=UTF-8");

                if (!CollectionUtils.isEmpty(additionalProperties)) {
                    printRespHeader = true;
                    for (Map.Entry<String, String> entry : additionalProperties.entrySet()) {
                        connection.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }
                if (accessToken != null && accessToken.trim().length() > 0) {
                    connection.setRequestProperty(JOB_ACCESS_TOKEN, accessToken);
                }

                // do connection
                connection.connect();

                // write requestBody
                if (requestObj != null) {
                    String requestBody = GsonTool.toJson(requestObj);
                    DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
                    dataOutputStream.write(requestBody.getBytes("UTF-8"));
                    dataOutputStream.flush();
                    dataOutputStream.close();
                }

                // valid StatusCode
                int statusCode = connection.getResponseCode();
                // result
                bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    result.append(line);
                }


                // 打印所有响应头字段
                if (printRespHeader) {
                    Map<String, List<String>> headers = connection.getHeaderFields();

                    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                        String key = entry.getKey();
                        List<String> values = entry.getValue();

                        if (key != null) {
                            if (values != null) {
                                // 如果有多个值，打印所有值
                                for (String value : values) {
                                    logger.info("postBody print resp header,key:{},value:{}", key, value);
                                }
                            } else {
                                // 如果只有一个值
                                logger.info("postBody print resp header,value:{}", connection.getHeaderField(key));
                            }
                        }
                    }
                }


                String resultJson = result.toString();
                if (statusCode != 200) {
                    logger.error("go-scheduler-rpc remoting fail, StatusCode({}) invalid. for url={},resultJson={} ", statusCode, resultJson, url);
                    return new ReturnT<String>(ReturnT.FAIL_CODE, "go-scheduler-rpc remoting fail, StatusCode(" + statusCode + ") invalid. for url : " + url);
                }
                logger.info("postBody resultJson {}", resultJson);
                // parse returnT
                try {
                    return GsonTool.fromJson(resultJson, ReturnT.class, returnTargClassOfT);
                } catch (Exception e) {
                    logger.error("go-scheduler-rpc remoting (url=" + url + ") response content invalid(" + resultJson + ").", e);
                    return new ReturnT<String>(ReturnT.FAIL_CODE, "go-scheduler-rpc remoting (url=" + url + ") response content invalid(" + resultJson + ").");
                }
            } catch (Exception e) {
                long end = System.currentTimeMillis();
                logger.error("go-scheduler-rpc remoting: time elapse={},error={}", (end - start) / 1000, e);
                message = e.getMessage();
                try {
                    TimeUnit.MILLISECONDS.sleep(200 * (i + 1) * (i + 1));
                } catch (InterruptedException e1) {
                }
            } finally {
                try {
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                } catch (Exception e2) {
                    logger.error(e2.getMessage(), e2);
                }
            }
        }
        logger.error("go-scheduler-rpc remoting error({}), for url : {}", message, url);
        return new ReturnT<String>(ReturnT.FAIL_CODE, "go-scheduler-rpc remoting error(" + message + "), for url : " + url);
    }

//    public static ESParam.ESResponse postBody4ES(String url, String accessToken, String proxyAddr, int timeout, String esSql,String esParams, Class returnTargClassOfT, Map<String, String> additionalProperties) {
    public static ESParam.ESResponse postBody4ES(String url, String accessToken, String proxyAddr, int timeout, Object requestObj, Class returnTargClassOfT, Map<String, String> additionalProperties) {
        HttpURLConnection connection = null;
        BufferedReader bufferedReader = null;
        Boolean printRespHeader = false;

        String message = "";
        for (int i = 0; i < MAX_RETRY_NUM; i++) {
            long start = System.currentTimeMillis();
            try {
                // connection
                URL realUrl = new URL(url);
                if (!StringUtils.isEmpty(proxyAddr)) {
                    String[] arr = proxyAddr.split(":");
                    InetSocketAddress addr = new InetSocketAddress(arr[0], arr.length == 2 ? Integer.parseInt(arr[1]) : 80);
                    // http 代理
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);
                    connection = (HttpURLConnection) realUrl.openConnection(proxy);
                } else {
                    connection = (HttpURLConnection) realUrl.openConnection();
                }
                // trust-https
                boolean useHttps = url.startsWith("https");
                if (useHttps) {
                    HttpsURLConnection https = (HttpsURLConnection) connection;
                    trustAllHosts(https);
                }
                // connection setting
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setUseCaches(false);
                connection.setReadTimeout(timeout * 1000);
                connection.setConnectTimeout(30 * 1000);
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("Keep-Alive", "timeout=5, max=100");
                connection.setRequestProperty("Content-Type", "application/json");
                // 设置ES proxy请求头
                connection.setRequestProperty("Operator-Name", "scheduling_platform");
                connection.setRequestProperty("Caller-Service", "scheduling_platform");

                if (!CollectionUtils.isEmpty(additionalProperties)) {
                    printRespHeader = true;
                    for (Map.Entry<String, String> entry : additionalProperties.entrySet()) {
                        connection.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }
                if (accessToken != null && accessToken.trim().length() > 0) {
                    connection.setRequestProperty(JOB_ACCESS_TOKEN, accessToken);
                }

                // do connection
                connection.connect();

                // write requestBody
//                if (esSql != null || esParams!=null) {
                if (requestObj != null ) {
//                    String jsonInputString = "{\"sql\": \"select * from fast_escrow_transaction_group_tab_br_index_canal where create_time > cast( ? as timestamp) and disburse_time_fs < ? limit 10\",\"params_json_array_str\": \"[\\\"2020-12-12 04:01:08\\\",1721873097]\"}";
                    Gson gson = new GsonBuilder()
                            .disableHtmlEscaping() // 关闭HTML转义
                            .create();
                    String requestBody = gson.toJson(requestObj);
                    requestBody=requestBody.replace("\\\\\\","\\");
                    DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
//                    dataOutputStream.write(jsonInputString.getBytes("UTF-8"));
                    dataOutputStream.write(requestBody.getBytes("UTF-8"));
                    dataOutputStream.flush();
                    dataOutputStream.close();
                }

                // valid StatusCode
                int statusCode = connection.getResponseCode();
                // result
                bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    result.append(line);
                }


                // 打印所有响应头字段
                if (printRespHeader) {
                    Map<String, List<String>> headers = connection.getHeaderFields();

                    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                        String key = entry.getKey();
                        List<String> values = entry.getValue();

                        if (key != null) {
                            if (values != null) {
                                // 如果有多个值，打印所有值
                                for (String value : values) {
                                    logger.info("postBody print resp header,key:{},value:{}", key, value);
                                }
                            } else {
                                // 如果只有一个值
                                logger.info("postBody print resp header,value:{}", connection.getHeaderField(key));
                            }
                        }
                    }
                }


                String resultJson = result.toString();
                if (statusCode != 200) {
                    logger.error("go-scheduler-rpc remoting fail, StatusCode({}) invalid. for url={},resultJson={} ", statusCode, resultJson, url);
                    return new ESParam.ESResponse(statusCode, "error", null, null);
                }
                logger.info("postBody resultJson {}", resultJson);
                // parse returnT
                try {
                    return GsonTool.fromJson(resultJson, ESParam.ESResponse.class, returnTargClassOfT);
                } catch (Exception e) {
                    logger.error("go-scheduler-rpc remoting (url=" + url + ") response content invalid(" + resultJson + ").", e);
                    return new ESParam.ESResponse(ReturnT.FAIL_CODE, "error", null, null);
                }
            } catch (Exception e) {
                long end = System.currentTimeMillis();
                logger.error("go-scheduler-rpc remoting: time elapse={},error={}", (end - start) / 1000, e);
                message = e.getMessage();
                try {
                    TimeUnit.MILLISECONDS.sleep(200 * (i + 1) * (i + 1));
                } catch (InterruptedException e1) {
                }
            } finally {
                try {
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                } catch (Exception e2) {
                    logger.error(e2.getMessage(), e2);
                }
            }
        }
        logger.error("go-scheduler-rpc remoting error({}), for url : {}", message, url);
        return new ESParam.ESResponse(ReturnT.FAIL_CODE, "error", null, null);
    }

}
