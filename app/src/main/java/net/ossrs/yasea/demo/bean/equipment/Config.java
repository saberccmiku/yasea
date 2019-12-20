package net.ossrs.yasea.demo.bean.equipment;

/**
 * 用户app列表展示
 */
public class Config {

    //标题
    private String title;
    //栏目
    private String label;
    //栏目 值
    private String input;

    private Integer sort;

    public Config() {
    }

    public Config(String title, String label, String input, Integer sort) {
        this.title = title;
        this.label = label;
        this.input = input;
        this.sort = sort;
    }

    public Config(String label, Integer sort) {
        this.label = label;
        this.sort = sort;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    @Override
    public String toString() {
        return "Config{" +
                "title='" + title + '\'' +
                ", label='" + label + '\'' +
                ", input='" + input + '\'' +
                ", sort=" + sort +
                '}';
    }
}
