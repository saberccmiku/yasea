package net.ossrs.yasea.demo.bean;

public class IcoInfo {
    private String name;
    private int drawableId;

    public IcoInfo() {

    }

    public IcoInfo(String name, int drawableId) {
        this.name = name;
        this.drawableId = drawableId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDrawableId() {
        return drawableId;
    }

    public void setDrawableId(int drawableId) {
        this.drawableId = drawableId;
    }
}
