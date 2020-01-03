package net.ossrs.yasea.demo.bean.equipment;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FaceFeatureInfo {
    private String name;
    private String faceFeatureCode;
    private Integer groupId;

    public FaceFeatureInfo(Integer groupId, String faceFeatureCode) {
        this.groupId = groupId;
        this.faceFeatureCode = faceFeatureCode;
    }

}
