package net.ossrs.yasea.demo.bean.equipment;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ThWindowStatus {

    /**
     * 主键唯一编码
     */
    private String id;
    /**
     * 窗口id
     */
    private String windowId;
    /**
     * 设备编号
     */
    private String devCode;
    /**
     * 直播视频地址
     */
    private String address;
    /**
     * 坐席状态（0离线1在线2人脸不匹配）
     */
    private int status;


}
