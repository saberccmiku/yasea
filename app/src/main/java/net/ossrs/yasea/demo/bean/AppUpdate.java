package net.ossrs.yasea.demo.bean;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AppUpdate {

    private Integer code;
    private String msg;
    private Integer updateStatus;
    private Integer versionCode;
    private String versionName;
    private String updateContent;
    private String downloadUrl;
    private Integer apkSize;
    private String apkMd5;
    private boolean hasUpdate;
    private boolean isIgnorable;

}
