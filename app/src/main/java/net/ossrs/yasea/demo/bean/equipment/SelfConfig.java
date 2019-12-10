package net.ossrs.yasea.demo.bean.equipment;

import io.objectbox.annotation.Id;

/**
 * 本机配置
 */
public class SelfConfig {

    @Id
    private long id;
    private String serialNo;
    private String stationNo;
    private String machineCode;

    public SelfConfig() {
    }

    public SelfConfig(String serialNo, String stationNo, String machineCode) {
        this.serialNo = serialNo;
        this.stationNo = stationNo;
        this.machineCode = machineCode;
    }

    public SelfConfig(long id, String serialNo, String stationNo, String machineCode) {
        this.id = id;
        this.serialNo = serialNo;
        this.stationNo = stationNo;
        this.machineCode = machineCode;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public String getStationNo() {
        return stationNo;
    }

    public void setStationNo(String stationNo) {
        this.stationNo = stationNo;
    }

    public String getMachineCode() {
        return machineCode;
    }

    public void setMachineCode(String machineCode) {
        this.machineCode = machineCode;
    }
}
