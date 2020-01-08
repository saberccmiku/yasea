package net.ossrs.yasea.demo.net.update;

import com.google.gson.Gson;
import com.xuexiang.xupdate.entity.UpdateEntity;
import com.xuexiang.xupdate.proxy.IUpdateParser;

import net.ossrs.yasea.demo.bean.AppUpdate;

/**
 * 实现IUpdateParser接口即可实现解析器的自定义。
 */
public class CustomUpdateParser implements IUpdateParser {
    @Override
    public UpdateEntity parseJson(String json) {
        Gson gson = new Gson();
        AppUpdate result = gson.fromJson(json, AppUpdate.class);
        if (result != null) {
            return new UpdateEntity()
                    .setHasUpdate(result.isHasUpdate())
                    .setIsIgnorable(result.isIgnorable())
                    .setVersionCode(result.getVersionCode())
                    .setVersionName(result.getVersionName())
                    .setUpdateContent(result.getUpdateContent())
                    .setDownloadUrl(result.getDownloadUrl())
                    .setSize(result.getApkSize());
        }
        return null;
    }
}