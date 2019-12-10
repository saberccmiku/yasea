package net.ossrs.yasea.demo.bean.equipment;

import io.objectbox.annotation.Id;

/**
 * 监控配置
 */
public class MonitorConfig {

    @Id
    private long id;
    private String ip;
    private String port;

    public MonitorConfig() {

    }

    public MonitorConfig(String ip, String port) {
        this.ip = ip;
        this.port = port;
    }

    public MonitorConfig(long id,String ip, String port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}
