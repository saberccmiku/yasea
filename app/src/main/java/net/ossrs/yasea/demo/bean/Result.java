package net.ossrs.yasea.demo.bean;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class Result<T> {

    private Integer code;
    private boolean success;
    private String message;
    private T data;

    public Result() {
        this(true);
    }

    public Result(boolean success) {
        this.success = true;
        this.success = success;
    }


}
