package net.ossrs.yasea.demo.bean.equipment;


import android.os.Parcel;
import android.os.Parcelable;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class BaseConfig implements Parcelable {

    @Id
    private long id;

    //网络配置
    private String networkIp;
    private String networkPort;
    //监控配置
    private String monitorIp;
    private String monitorPort;
    //本机配置
    private String localSerial;
    private String localStation;
    private String localWindowId;

    public BaseConfig() {
    }

    protected BaseConfig(Parcel in) {
        id = in.readLong();
        networkIp = in.readString();
        networkPort = in.readString();
        monitorIp = in.readString();
        monitorPort = in.readString();
        localSerial = in.readString();
        localStation = in.readString();
        localWindowId = in.readString();
    }

    public static final Creator<BaseConfig> CREATOR = new Creator<BaseConfig>() {
        @Override
        public BaseConfig createFromParcel(Parcel in) {
            return new BaseConfig(in);
        }

        @Override
        public BaseConfig[] newArray(int size) {
            return new BaseConfig[size];
        }
    };

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNetworkIp() {
        return networkIp;
    }

    public void setNetworkIp(String networkIp) {
        this.networkIp = networkIp;
    }

    public String getNetworkPort() {
        return networkPort;
    }

    public void setNetworkPort(String networkPort) {
        this.networkPort = networkPort;
    }

    public String getMonitorIp() {
        return monitorIp;
    }

    public void setMonitorIp(String monitorIp) {
        this.monitorIp = monitorIp;
    }

    public String getMonitorPort() {
        return monitorPort;
    }

    public void setMonitorPort(String monitorPort) {
        this.monitorPort = monitorPort;
    }

    public String getLocalSerial() {
        return localSerial;
    }

    public void setLocalSerial(String localSerial) {
        this.localSerial = localSerial;
    }

    public String getLocalStation() {
        return localStation;
    }

    public void setLocalStation(String localStation) {
        this.localStation = localStation;
    }

    public String getLocalWindowId() {
        return localWindowId;
    }

    public void setLocalWindowId(String localWindowId) {
        this.localWindowId = localWindowId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(networkIp);
        dest.writeString(networkPort);
        dest.writeString(monitorIp);
        dest.writeString(monitorPort);
        dest.writeString(localSerial);
        dest.writeString(localStation);
        dest.writeString(localWindowId);
    }

    @Override
    public String toString() {
        return "BaseConfig{" +
                "id=" + id +
                ", networkIp='" + networkIp + '\'' +
                ", networkPort='" + networkPort + '\'' +
                ", monitorIp='" + monitorIp + '\'' +
                ", monitorPort='" + monitorPort + '\'' +
                ", localSerial='" + localSerial + '\'' +
                ", localStation='" + localStation + '\'' +
                ", localWindowId='" + localWindowId + '\'' +
                '}';
    }
}
