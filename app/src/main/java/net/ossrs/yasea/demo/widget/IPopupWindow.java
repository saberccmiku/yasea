package net.ossrs.yasea.demo.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import net.ossrs.yasea.demo.R;
import net.ossrs.yasea.demo.adapter.CommonRecyclerAdapter;
import net.ossrs.yasea.demo.adapter.CommonRecyclerViewHolder;
import net.ossrs.yasea.demo.bean.IcoInfo;
import net.ossrs.yasea.demo.bean.equipment.Config;
import net.ossrs.yasea.demo.bean.equipment.ConfigPattern;

import java.util.ArrayList;
import java.util.List;

public class IPopupWindow extends PopupWindow {

    private Context context;
    private View view;
    private CommonRecyclerAdapter<Config> adapter;
    private List<Config> configList;
    private IPopupWindow iPopupWindow;
    private TextView tv;

    public IPopupWindow(Context context, CommonRecyclerAdapter<Config> adapter, List<Config> configList,TextView tv) {
        super(context);
        this.context = context;
        this.adapter = adapter;
        this.configList = configList;
        //设置宽高
        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setFocusable(true);
        setOutsideTouchable(true);
        setTouchable(true);
        view = LayoutInflater.from(context).inflate(R.layout.layout_i_popup_window, null);
        setContentView(view);
        this.iPopupWindow= this;
        this.tv= tv;
        setBackgroundDrawable(null);
        initData();
    }

    private void initData() {
        List<IcoInfo> menuList = new ArrayList<>();

        menuList.add(new IcoInfo("生产", R.drawable.ic_pro));
        menuList.add(new IcoInfo("测试", R.drawable.ic_test));
        menuList.add(new IcoInfo("开发", R.drawable.ic_dev));

        RecyclerView rvMore = view.findViewById(R.id.rv_more);
        rvMore.setLayoutManager(new LinearLayoutManager(context));
        CommonRecyclerAdapter<IcoInfo> adapter = new CommonRecyclerAdapter<IcoInfo>(context, R.layout.item_more, menuList) {
            @Override
            public void convertView(CommonRecyclerViewHolder holder, IcoInfo icoInfo) {
                TextView tv = holder.getView(R.id.tv);
                tv.setText(icoInfo.getName());
                Drawable drawable = view.getResources().getDrawable(icoInfo.getDrawableId(), null);
                drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                tv.setCompoundDrawables(drawable, null, null, null);
            }
        };
        adapter.setOnItemClickListener(new CommonRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                resetConfigData(position);
                iPopupWindow.dismiss();
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        });
        rvMore.setAdapter(adapter);
    }

    /**
     * 加载默认数据配置
     */
    private void resetConfigData(Integer status) {

        //网络配置
        switch (status) {
            case 0:
                tv.setText("设备激活(正式)");
                configList.get(1).setInput("111.231.209.105");
                configList.get(4).setInput("111.231.209.105");
                configList.get(5).setInput("8935");
                break;
            case 1:
                tv.setText("设备激活(测试)");
                configList.get(1).setInput("192.168.0.72");
                configList.get(4).setInput("192.168.0.242");
                configList.get(5).setInput("8935");
                break;
            case 2:
                tv.setText("设备激活(开发)");
                configList.get(1).setInput("192.168.1.58");
                configList.get(4).setInput("192.168.1.58");
                configList.get(5).setInput("80");
                break;
        }
        configList.get(8).setInput("");
        configList.get(9).setInput("");
        adapter.notifyItemChanged(1);
        adapter.notifyItemChanged(4);
        adapter.notifyItemChanged(5);
        adapter.notifyItemChanged(8);
        adapter.notifyItemChanged(9);
    }


}
