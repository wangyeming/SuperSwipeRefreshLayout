package com.tianyeguang.superswiperefreshlayout.defaultswipe;

/**
 * 下拉刷新头部的接口
 *
 * Created by wangyeming on 2016/8/14.
 */
public interface SwipeRefreshHeader {

    void onRefresh(boolean isRefresh);

    void onDrag(float percent);
}
