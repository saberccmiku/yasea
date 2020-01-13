package net.ossrs.yasea.demo.bean;


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


    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
