package net.ossrs.yasea.demo.contract;

import net.ossrs.yasea.demo.base.BaseView;
import net.ossrs.yasea.demo.bean.ActiveBean;
import net.ossrs.yasea.demo.bean.BaseObjectBean;
import net.ossrs.yasea.demo.bean.LoginBean;

public class ActiveContract {

    interface Model {

    }

    interface View extends BaseView{

        @Override
        void showLoading();

        @Override
        void hideLoading();

        @Override
        void onError(Throwable throwable);

        void onSuccess(BaseObjectBean<ActiveBean> bean);
    }
}
