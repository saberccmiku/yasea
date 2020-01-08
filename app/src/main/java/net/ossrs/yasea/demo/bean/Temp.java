package net.ossrs.yasea.demo.bean;

import lombok.Data;

@Data
public class Temp {

    private Integer Code;
    private String Msg;
    private Integer UpdateStatus;
    private Integer VersionCode;
    private String VersionName;
    private String ModifyContent;
    private String DownloadUrl;
    private Integer ApkSize;
    private String ApkMd5;

    public Temp() {
    }

    public Temp(Integer Code,
                String Msg,
                Integer UpdateStatus,
                Integer VersionCode,
                String VersionName,
                String ModifyContent,
                String DownloadUrl,
                Integer ApkSize,
                String ApkMd5) {
        this.Code = Code;
        this.Msg = Msg;
        this.UpdateStatus = UpdateStatus;
        this.VersionCode = VersionCode;
        this.VersionName = VersionName;
        this.ModifyContent = ModifyContent;
        this.DownloadUrl = DownloadUrl;
        this.ApkSize = ApkSize;
        this.ApkMd5 = ApkMd5;
    }


}
