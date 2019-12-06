package net.ossrs.yasea.demo.model;

import net.ossrs.yasea.demo.bean.BaseObjectBean;
import net.ossrs.yasea.demo.bean.LoginBean;
import net.ossrs.yasea.demo.contract.MainContract;
import net.ossrs.yasea.demo.net.RetrofitClient;

import io.reactivex.Flowable;

/**
 * @author fjy
 * Description：
 */
public class MainModel  implements MainContract.Model {
    @Override
    public Flowable<BaseObjectBean<LoginBean>> login(String username, String password) {
        return RetrofitClient.getInstance().getApi().login(username,password);
    }
}
