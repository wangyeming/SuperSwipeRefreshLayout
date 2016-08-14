##SuperSwipeRefreshLayout

同时支持下拉刷新和上拉加载的控件，fork自[nuptboyzhb/SuperSwipeRefreshLayout](https://github.com/nuptboyzhb/SuperSwipeRefreshLayout)

做了大量的设计优化及Bug修复

##特点：
1. 无侵入性，用法类似官方的SwipeRefreshLayout
2. 上拉加载的交互设计与下拉刷新的相同。
3. 支持自定义header和footer
4. 目前仅支持ListView RecyclerView ScrollView
5. 支持禁用上拉和下拉
6. 支持自定义开启上拉是否允许超过FooterView的高度

##相比原项目修复了哪些问题？
1. 修复了判断ListView是否滑动到底部的标准异常
2. 修复了判断RecyclerView是否滑动到底部的标准异常
3. 修复了触发下拉刷新时，拉到列表的底部显示内容不完整
4. 修复了触发下拉刷新后，没有动画弹回效果的异常
5. 修复了触发上拉加载后，没有动画弹回效果的异常
6. 修复了当child位置为顶部时，这时触发上拉操作，上拉高度的初始值异常，不应该跳跃
7. 更多...

##相比原项目优化了哪些地方呢？
1. 新增了支持禁用上拉和下拉的方法
2. 暴露的接口方法更合理 

        /**
         * 下拉刷新回调
         */
        public interface OnPullRefreshListener {
            void onRefresh();
    
            void onPull(float percent);     //百分比，而不是距离，下同
    
            void onPullEnable(boolean enable);
        }
    
        /**
         * 上拉加载更多
         */
        public interface OnPushLoadMoreListener {
            void onLoadMore();
    
            void onPushDistance(int distance);
    
            void onPushEnable(boolean enable);
        }

##目前存在的问题：
1. child不支持任意ViewGroup，导致对布局有很多限制(急需修复)
2. 上拉过程中，child的布局高度异常，导致此时如果下拉到列表的顶部，会发现布局展示不完整
3. 启用上拉不允许超过FooterView的高度后，某些情况下距离计算有点问题（后续可能会去掉该方法，没什么用，之前是为了解决设计的某种特殊需求）
4. 上拉触发并松手后，似乎有一点不太正常的抖动，不明显

##如何在项目中使用？
>直接复制SuperSwipeRefreshLayout.java, 哈哈，简单省事，

##贡献：
非常欢迎，但是有一些希望能够注意的地方：
* pull request每次的commit颗粒度尽量小，只解决一个问题，这样被采纳的可能性更高
* 如果有比较大的改动，可以事先联系我，我们一起讨论方案~

##用法：

xml:

    <com.tianyeguang.superswiperefreshlayout.SuperSwipeRefreshLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent">

            <ListView
                android:layout_width="match_parent"
                android:layout_height="match_parent"/>

    </com.tianyeguang.superswiperefreshlayout.SuperSwipeRefreshLayout>

设置HeaderView和FooterView

    vSuperSwipeRefreshLayout.setHeaderView(vHeader);
    vSuperSwipeRefreshLayout.setFooterView(vFooter);
    
设置上拉是否允许超过FooterView的高度：

    vSuperSwipeRefreshLayout.setEnablePushOverFooterHeight(boolean isAllow);
    
启用或关闭下拉刷新/上拉加载：

    vSuperSwipeRefreshLayout.setOnPullEnable(boolean enableRefresh)
    vSuperSwipeRefreshLayout.setOnPushEnable(boolean enableLoadMore)

关闭本次下拉刷新/上拉加载：

    vSuperSwipeRefreshLayout.onRefresh(boolean isRefresh);
    vSuperSwipeRefreshLayout.onLoadMore(boolean isLoadMore);



