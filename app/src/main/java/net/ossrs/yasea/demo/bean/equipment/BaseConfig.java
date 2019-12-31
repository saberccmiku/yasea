package net.ossrs.yasea.demo.bean.equipment;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BaseConfig {

    //网络配置
    private String networkIp;
    private String networkPort;
    //监控配置
    private String monitorIp;
    private String monitorPort;
    //本机配置
    private String localSerial;
    private String localStation;


}
