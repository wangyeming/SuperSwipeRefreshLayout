package com.tianyeguang.superswiperefreshlayout.defaultswipe;

/**
 * 上拉加载footer接口
 *
 * Created by wangyeming on 2016/8/14.
 */
public interface SwipeRefreshFooter {

    void onLoadMore(boolean isLoadMore);

    void onDrag(int distance);
}
