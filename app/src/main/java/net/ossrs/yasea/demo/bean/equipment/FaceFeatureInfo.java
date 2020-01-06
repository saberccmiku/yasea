package net.ossrs.yasea.demo.bean.equipment;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FaceFeatureInfo {
    private String name;
    private String faceFeatureCode;
    private Integer similarValue;
    private String imgUrl;
    private Integer groupId;
    private String windowId;

    public FaceFeatureInfo(Integer groupId, String faceFeatureCode,String windowId) {
        this.groupId = groupId;
        this.faceFeatureCode = faceFeatureCode;
        this.windowId = windowId;
    }

}
