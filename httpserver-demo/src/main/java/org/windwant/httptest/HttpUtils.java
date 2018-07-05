package org.windwant.httptest;

import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.MappedByteBuffer;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

/**
 * Created by windwant on 2016/6/5.
 */
public class HttpUtils {
    //应用结束时需要shutdown getTotalStats：leased当前使用的线程数，pending等待数，available可用数 max最大数
    protected static PoolingHttpClientConnectionManager mgr;
    //CloseableHttpClient是线程安全的，建议相同的这个类的实例被重用于多个请求执行
    protected static CloseableHttpClient client;

    static {
        mgr = new PoolingHttpClientConnectionManager();
        ConnectionConfig config = ConnectionConfig.custom().setBufferSize(4 * 1024).setCharset(Charset.defaultCharset()).build();
        mgr.setDefaultConnectionConfig(config);
        SocketConfig sconfig = SocketConfig.custom()
                .setBacklogSize(65535) //backlog的定义是已连接但未进行accept处理的SOCKET队列大小
                .setRcvBufSize(4 * 1024)
                .setSndBufSize(4 * 1024)
                .setSoKeepAlive(false)
                .setSoTimeout(30000)
                .setTcpNoDelay(true)
                .build();
        mgr.setDefaultSocketConfig(sconfig);
        mgr.setDefaultMaxPerRoute(100);//每个路由最大连接数
        mgr.setMaxTotal(100); //设置整个连接池最大连接数
        mgr.setValidateAfterInactivity(2000);

        client = HttpClientBuilder.create()
                .setConnectionManager(mgr)
                .setConnectionReuseStrategy(new DefaultClientConnectionReuseStrategy())
                .setConnectionManagerShared(true)
                .setDefaultCredentialsProvider(new SystemDefaultCredentialsProvider())
                .build();

    }

    public static CloseableHttpResponse httpGet(String url) throws IOException {
        System.out.println("conn mgr stats: " + mgr.getTotalStats());
        return client.execute(new HttpGet(url));
    }

    public static CloseableHttpClient getInstance(){
        return HttpClients.createDefault();
    }

    public static HttpGet getHttpGet(String url){
        return new HttpGet(url);
    }
    public static HttpPost getHttpPost(String url){
        return new HttpPost(url);
    }

    public static HttpGet getHttpGet(String host, int port, String path, String scheme, List<NameValuePair> params){
        URIBuilder ub = new URIBuilder();
        ub.setScheme(null != scheme?scheme:"http");
        ub.setHost(host);
        ub.setPort(port);
        ub.setPath(path);
        if(null != params) {
            ub.setParameters(params);
        }
        try {
            return new HttpGet(ub.build());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void unmap(final MappedByteBuffer mappedByteBuffer) {
        try {
            if (mappedByteBuffer == null) {
                return;
            }

            mappedByteBuffer.force();
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @SuppressWarnings("restriction")
                public Object run() {
                    try {
                        Method getCleanerMethod = mappedByteBuffer.getClass()
                                .getMethod("cleaner", new Class[0]);
                        getCleanerMethod.setAccessible(true);
                        sun.misc.Cleaner cleaner =
                                (sun.misc.Cleaner) getCleanerMethod
                                        .invoke(mappedByteBuffer, new Object[0]);
                        cleaner.clean();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("clean MappedByteBuffer completed");
                    return null;
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
