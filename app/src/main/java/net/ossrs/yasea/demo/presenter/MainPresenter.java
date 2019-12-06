package net.ossrs.yasea.demo.presenter;


import net.ossrs.yasea.demo.base.BasePresenter;
import net.ossrs.yasea.demo.bean.BaseObjectBean;
import net.ossrs.yasea.demo.bean.LoginBean;
import net.ossrs.yasea.demo.contract.MainContract;
import net.ossrs.yasea.demo.model.MainModel;
import net.ossrs.yasea.demo.net.RxScheduler;

import io.reactivex.functions.Consumer;

/**
 * @author fjy
 * Description：
 */
public class MainPresenter extends BasePresenter<MainContract.View> implements MainContract.Presenter {

    private MainContract.Model model;

    public MainPresenter() {
        model = new MainModel();
    }

    @Override
    public void login(String username, String password) {
        //View是否绑定 如果没有绑定，就不执行网络请求
        if (!isViewAttached()) {
            return;
        }
        mView.showLoading();
        model.login(username, password)
                .compose(RxScheduler.<BaseObjectBean<LoginBean>>Flo_io_main())
                .as(mView.<BaseObjectBean<LoginBean>>bindAutoDispose())
                .subscribe(new Consumer<BaseObjectBean<LoginBean>>() {
                    @Override
                    public void accept(BaseObjectBean<LoginBean> bean) throws Exception {
                        mView.onSuccess(bean);
                        mView.hideLoading();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        mView.onError(throwable);
                        mView.hideLoading();
                    }
                });
    }
}
