package net.ossrs.yasea.demo.bean;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class AppUpdate {

    private Integer Code;
    private String Msg;
    private Integer UpdateStatus;
    private Integer VersionCode;
    private String VersionName;
    private String ModifyContent;
    private String DownloadUrl;
    private Integer ApkSize;
    private String ApkMd5;

    public AppUpdate(){}

    public AppUpdate(Integer code, String msg, Integer updateStatus, Integer versionCode, String versionName, String modifyContent, String downloadUrl, Integer apkSize, String apkMd5) {
        Code = code;
        Msg = msg;
        UpdateStatus = updateStatus;
        VersionCode = versionCode;
        VersionName = versionName;
        ModifyContent = modifyContent;
        DownloadUrl = downloadUrl;
        ApkSize = apkSize;
        ApkMd5 = apkMd5;
    }
}
