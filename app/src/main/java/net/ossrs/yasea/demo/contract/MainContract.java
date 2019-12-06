package net.ossrs.yasea.demo.contract;

import net.ossrs.yasea.demo.base.BaseView;
import net.ossrs.yasea.demo.bean.BaseObjectBean;
import net.ossrs.yasea.demo.bean.LoginBean;

import io.reactivex.Flowable;

public interface MainContract {
    interface Model {
        Flowable<BaseObjectBean<LoginBean>> login(String username, String password);
    }

    interface View extends BaseView {
        @Override
        void showLoading();

        @Override
        void hideLoading();

        @Override
        void onError(Throwable throwable);

        void onSuccess(BaseObjectBean<LoginBean> bean);
    }

    interface Presenter {
        /**
         * 登陆
         *
         * @param username
         * @param password
         */
        void login(String username, String password);
    }
}