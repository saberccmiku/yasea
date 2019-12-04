package net.ossrs.yasea.demo.model;

import java.io.Serializable;

public class UserInfo  implements Serializable {

    private static final long serialVersionUID = -2946645039603724239L;

    private String userName;

    public UserInfo(){

    }

    public UserInfo(String userName){
        this.userName = userName;
    }


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
