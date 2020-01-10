package net.ossrs.yasea.demo.bean;

import android.graphics.drawable.Drawable;

import lombok.Data;

@Data
public class IcoInfo {
    private String name;
    private int drawableId;

    public IcoInfo() {

    }

    public IcoInfo(String name, int drawableId) {
        this.name = name;
        this.drawableId = drawableId;
    }
}
