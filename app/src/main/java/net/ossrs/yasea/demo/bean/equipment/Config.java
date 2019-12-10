package net.ossrs.yasea.demo.bean.equipment;

/**
 * 用户app列表展示
 */
public class Config {

    private boolean isTitle;
    private String label;
    private String input;

    public Config() {
    }

    public Config(String label, String input) {
        this.label = label;
        this.input = input;
    }

    public Config(String label, boolean isTitle) {
        this.isTitle = isTitle;
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public boolean isTitle() {
        return isTitle;
    }

    public void setTitle(boolean title) {
        isTitle = title;
    }

    @Override
    public String toString() {
        return "Config{" +
                "isTitle=" + isTitle +
                ", label='" + label + '\'' +
                ", input='" + input + '\'' +
                '}';
    }
}
