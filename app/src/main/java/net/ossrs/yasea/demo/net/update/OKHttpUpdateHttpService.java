package net.ossrs.yasea.demo.net.update;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;
import com.xuexiang.xupdate.proxy.IUpdateHttpService;

import net.ossrs.yasea.demo.bean.AppUpdate;
import net.ossrs.yasea.demo.bean.Temp;
import net.ossrs.yasea.demo.net.RetrofitHelper;

import java.util.Map;
import java.util.Observable;
import java.util.TreeMap;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 使用okhttp
 *
 * @author xuexiang
 * @since 2018/7/10 下午4:04
 */
public class OKHttpUpdateHttpService implements IUpdateHttpService {

    private String TAG = OKHttpUpdateHttpService.class.getName();


    public OKHttpUpdateHttpService() {

    }

    @Override
    public void asyncGet(@NonNull String url, @NonNull Map<String, Object> params, final @NonNull Callback callBack) {
        Log.d(TAG,"-----------------------asyncGet---------------------");
        RetrofitHelper.getAppAPI()//基础URL
                .getAppUpdate()//接口后缀URL
                //.compose(RxLifecycle.bindUntilEvent(lifecycle(), ActivityEvent.DESTROY))//设计是否备份数据
                //.map(RecommendInfo::getResult)//得到JSON子数组
                .subscribeOn(Schedulers.io())//设计线程读写方式
                .observeOn(AndroidSchedulers.mainThread())//指定线程运行的位置
                .subscribe(new Observer<AppUpdate>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(AppUpdate appUpdate) {
                        Gson gson = new Gson();
                        String s = gson.toJson(appUpdate);
                        callBack.onSuccess(s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        callBack.onError(e);
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    @Override
    public void asyncPost(@NonNull String url, @NonNull Map<String, Object> params, final @NonNull Callback callBack) {
        //这里默认post的是Form格式，使用json格式的请修改 post -> postString
        Log.d(TAG,"-----------------------asyncPost---------------------");
        RetrofitHelper.getAppAPI()//基础URL
                .getAppUpdate()//接口后缀URL
                //.compose(RxLifecycle.bindUntilEvent(lifecycle(), ActivityEvent.DESTROY))//设计是否备份数据
                //.map(RecommendInfo::getResult)//得到JSON子数组
                .subscribeOn(Schedulers.io())//设计线程读写方式
                .observeOn(AndroidSchedulers.mainThread())//指定线程运行的位置
                .subscribe(new Observer<AppUpdate>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(AppUpdate appUpdate) {
                        Gson gson = new Gson();
                        String s = gson.toJson(appUpdate);
                        callBack.onSuccess(s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        callBack.onError(e);
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    @Override
    public void download(@NonNull String url, @NonNull String path, @NonNull String fileName, final @NonNull DownloadCallback callback) {
        Log.d(TAG,"-----------------------download---------------------");

    }

    @Override
    public void cancelDownload(@NonNull String url) {
        Log.d(TAG,"-----------------------cancelDownload---------------------");
    }

    private Map<String, String> transform(Map<String, Object> params) {
        Map<String, String> map = new TreeMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            map.put(entry.getKey(), entry.getValue().toString());
        }
        return map;
    }


}