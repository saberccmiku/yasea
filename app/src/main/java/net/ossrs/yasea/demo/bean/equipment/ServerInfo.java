package net.ossrs.yasea.demo.bean.equipment;

public class ServerInfo {

    private String ip;
    private Integer port;
    private String serial;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    @Override
    public String toString() {
        return "ServerInfo{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", serial='" + serial + '\'' +
                '}';
    }
}
