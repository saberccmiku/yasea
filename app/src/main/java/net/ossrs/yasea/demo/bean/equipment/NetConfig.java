package net.ossrs.yasea.demo.bean.equipment;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * 网络配置
 */
@Entity
public class NetConfig {

    @Id
    private long id;
    private String ip;
    private String port;

    public NetConfig() {

    }

    public NetConfig(long id, String ip, String port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    public NetConfig(String ip, String port) {
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
