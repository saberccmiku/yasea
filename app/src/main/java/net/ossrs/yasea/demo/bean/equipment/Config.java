package net.ossrs.yasea.demo.bean.equipment;

import android.os.Parcel;
import android.os.Parcelable;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * 用户app列表展示
 */
@Entity
public class Config implements Parcelable {

    @Id
    private long id;
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

    protected Config(Parcel in) {
        id = in.readLong();
        title = in.readString();
        label = in.readString();
        input = in.readString();
        if (in.readByte() == 0) {
            sort = null;
        } else {
            sort = in.readInt();
        }
    }

    public static final Creator<Config> CREATOR = new Creator<Config>() {
        @Override
        public Config createFromParcel(Parcel in) {
            return new Config(in);
        }

        @Override
        public Config[] newArray(int size) {
            return new Config[size];
        }
    };

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
                "id=" + id +
                ", title='" + title + '\'' +
                ", label='" + label + '\'' +
                ", input='" + input + '\'' +
                ", sort=" + sort +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(label);
        dest.writeString(input);
        if (sort == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(sort);
        }
    }
}
