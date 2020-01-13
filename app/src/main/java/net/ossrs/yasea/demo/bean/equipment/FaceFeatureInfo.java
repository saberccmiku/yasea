package net.ossrs.yasea.demo.bean.equipment;

public class FaceFeatureInfo {
    private String name;
    private String faceFeatureCode;
    private Integer similarValue;
    private String imgUrl;
    private Integer groupId;
    private String windowId;

    public FaceFeatureInfo(Integer groupId, String faceFeatureCode, String windowId) {
        this.groupId = groupId;
        this.faceFeatureCode = faceFeatureCode;
        this.windowId = windowId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFaceFeatureCode() {
        return faceFeatureCode;
    }

    public void setFaceFeatureCode(String faceFeatureCode) {
        this.faceFeatureCode = faceFeatureCode;
    }

    public Integer getSimilarValue() {
        return similarValue;
    }

    public void setSimilarValue(Integer similarValue) {
        this.similarValue = similarValue;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public String getWindowId() {
        return windowId;
    }

    public void setWindowId(String windowId) {
        this.windowId = windowId;
    }
}
