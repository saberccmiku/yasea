package net.ossrs.yasea.demo.net;

import net.ossrs.yasea.demo.bean.RecommendInfo;

import io.reactivex.Observable;
import retrofit2.http.GET;


/**
 * @author fjy
 * Description：
 */
public interface APIService {

    /**
     * 首页推荐数据
     */
    /**
     * 首页推荐数据
     /**
     * 首页推荐数据
     */
    @GET("x/show/old?platform=android&device=&build=412001")
    Observable<RecommendInfo> getRecommendedInfo();

}
