package net.ossrs.yasea.demo.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public abstract class CommonRecyclerAdapter<T> extends RecyclerView.Adapter<CommonRecyclerViewHolder> {
    private Context mContext;
    private int mLayoutId;
    private List<T> mDataList;
    private List<CommonRecyclerViewHolder> holderList;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);

        void onItemLongClick(View view, int position);
    }

    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    protected CommonRecyclerAdapter(Context context, int layoutId, List<T> dataList) {
        mContext = context;
        mLayoutId = layoutId;
        mDataList = dataList;
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        holderList = new ArrayList<>();
    }


    @Override
    public CommonRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        return CommonRecyclerViewHolder.getViewHolder(mContext, mLayoutId, parent);
    }

    @Override
    public void onBindViewHolder(@NonNull final CommonRecyclerViewHolder holder, int position) {
        convertView(holder, mDataList.get(position));
        holderList.add(holder);
        // 如果设置了回调，则设置点击事件
        if (mOnItemClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickListener.onItemClick(holder.itemView, pos);
                }
            });

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickListener.onItemLongClick(holder.itemView, pos);
                    return false;
                }
            });
        }


    }

    public abstract void convertView(CommonRecyclerViewHolder holder, T t);


    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    public List<CommonRecyclerViewHolder> getHolderList() {
        return holderList;
    }

    public void setHolderList(List<CommonRecyclerViewHolder> holderList) {
        this.holderList = holderList;
    }
}