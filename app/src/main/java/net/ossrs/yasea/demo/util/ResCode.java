package net.ossrs.yasea.demo.util;

/**
 * 响应代码
 */
public enum ResCode {

    OK(200, "成功"),
    NETWORK_ERROR(201, "网络异常"),
    LIVE_SERVER_ERROR(202, "监控服务不存在或未开启"),
    EQUIPMENT_NOT_ACTIVE(203, "设备未激活"),
    CENTER_SERVER_ERROR(204, "中央服务不存在或者未开启"),
    INCOMPLETE_INFORMATION(205, "信息不全"),
    ACTIVE_SUCCESS(206, "激活成功"),
    ACTIVE_ERROR(207, "激活失败"),
    ACTIVATED(208, "已激活"),
    NOT_FOUND_STATION(209, "未找到与该设备相匹配的工位信息");

    // 成员变量
    private Integer code;
    private String msg;

    // 构造方法
    private ResCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}