package net.ossrs.yasea.demo;



public class CompareResult {
    private String userName;
    private float similar;
    private String sex;
    private int trackId;

    public CompareResult(String userName, float similar) {
        this.userName = userName;
        this.similar = similar;
    }


    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public float getSimilar() {
        return similar;
    }

    public void setSimilar(float similar) {
        this.similar = similar;
    }

    public int getTrackId() {
        return trackId;
    }

    public void setTrackId(int trackId) {
        this.trackId = trackId;
    }
}
