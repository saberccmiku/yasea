package net.ossrs.yasea.demo.net;

import okhttp3.MediaType;

public class ApiConstants {
    public final static String APP_BASE_URL = "http://app.bilibili.com/";
    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");// 这个需要和服务端保持一致
    public static final String COMMON_UA_STR = "OhMyBiliBili Android Client/2.1 (639878266@qq.com)";
    public static String rtmpUrl = "rtmp://192.168.1.25/hls/test";
}
