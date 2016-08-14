package com.tianyeguang.superswiperefreshlayout.defaultswipe;

import android.content.Context;
import android.util.AttributeSet;
import com.tianyeguang.lib.SuperSwipeRefreshLayout;

/**
 * 通用的下拉刷新加上拉加载的控件，对{@link SuperSwipeRefreshLayout}的一层封装
 * 配置好Header和Footer的样式，对外暴露尽可能简单的方法
 * <p>
 * Created by wangyeming on 2016/8/14.
 */
public class DefaultSwipeLayout extends SuperSwipeRefreshLayout implements
        SuperSwipeRefreshLayout.OnPullRefreshListener, SuperSwipeRefreshLayout.OnPushLoadMoreListener {

    private OnRefreshListener mOnRefreshListener;
    private OnLoadMoreListener mOnLoadMoreListener;

    private CustomSwipeHeader vHeader;
    private CustomSwipeFooter vFooter;

    public DefaultSwipeLayout(Context context) {
        this(context, null);
    }

    public DefaultSwipeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        vHeader = new CustomSwipeHeader(getContext());
        setHeaderView(vHeader);

        vFooter = new CustomSwipeFooter(getContext());
        setFooterView(vFooter);

        setOnPullRefreshListener(this);
        setOnPushLoadMoreListener(this);

        //设置上拉高度不超过footer的高度
//        setEnablePushOverFooterHeight(false);
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
          mOnRefreshListener = onRefreshListener;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        mOnLoadMoreListener = onLoadMoreListener;
    }

    @Override
    public void onRefresh() {
        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
        vHeader.onRefresh(true);
    }

    @Override
    public void onPull(float percent) {
        vHeader.onDrag(percent);
    }

    @Override
    public void onPullEnable(boolean enable) {

    }

    @Override
    public void onLoadMore() {
        if (mOnLoadMoreListener != null) {
            mOnLoadMoreListener.onLoadMore();
        }
        vFooter.onLoadMore(true);
    }

    @Override
    public void onPushDistance(int distance) {
        vFooter.onDrag(distance);
    }

    @Override
    public void onPushEnable(boolean enable) {

    }

    /**
     * 对外暴露的下拉刷新的接口
     */
    public interface OnRefreshListener {
        void onRefresh();
    }

    /**
     * 对外暴露的上拉加载的接口
     */
    public interface OnLoadMoreListener {
        void onLoadMore();
    }
}
