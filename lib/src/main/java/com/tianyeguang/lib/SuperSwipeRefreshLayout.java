package com.tianyeguang.lib;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

/**
 * 来源自： https://github.com/nuptboyzhb/SuperSwipeRefreshLayout
 * 做了大量的Bug修复和优化
 * <p>
 * 目前仅支持RecyclerView， ListView, ScrollView,  GridView
 * <p>
 * 目前已知的问题：
 * 1. 触发上拉加载后，滑到顶部有部分区域被截断（上拉时onMeasure() 高度计算不对）
 * 2. 不支持任意布局child
 * 3. 上拉加载超过footer高度后，超出的部分下拉没有效果
 * 4. 触发上拉后有一点弹动
 * <p>
 * <p>
 * Created by wangyeming on 16-8-14.
 */
@SuppressLint("ClickableViewAccessibility")
public class SuperSwipeRefreshLayout extends ViewGroup {

    private static final String LOG_TAG = "SuperSwipeRefreshLayout";

    private static final int HEADER_VIEW_HEIGHT = 70;// HeaderView height (dp)
    private static final int FOOTER_VIEW_HEIGHT = 70;// FooterView height (dp)

    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;
    private static final float DRAG_RATE = .5f;

    private static final int ANIMATE_TO_TRIGGER_DURATION = 200;
    private static final int ANIMATE_TO_START_DURATION = 200;

    // SuperSwipeRefreshLayout内的目标View，比如RecyclerView,ListView,ScrollView,GridView
    // etc.
    private View mTarget;
    private View vScrollTarget;                             //child的scrollTarget

    private OnPullRefreshListener mListener;                // 下拉刷新listener
    private OnPushLoadMoreListener mOnPushLoadMoreListener; // 上拉加载更多

    private boolean mRefreshing = false;                    //当前是否在刷新
    private boolean mLoadMore = false;                      //当前是否在加载
    private int mTouchSlop;                                 //最小滑动距离，当手指移动距离大于该值时，才开始处理滑动
    private float mTotalPushDragDistance = -1;              //触发下拉刷新的滑动临界值
    private int mCurrentTargetOffsetTop;                    //当前child距离顶部的偏离值
    // Whether or not the starting offset has been determined.
    private boolean mOriginalOffsetCalculated = false;      //用于标示top距离是否初始化

    protected int mOriginalOffsetTop;                       //下拉刷新 progress 出现的位置
    private int mActivePointerId = INVALID_POINTER;         //初始触摸点的标示，用于判断滑动的合法性及计算移动距离

    // Target is returning to its start offset because it was cancelled or a
    // refresh was triggered.
    private boolean mReturningToStart;                      //target是否返回到初始偏移位置, true的话不拦截不处理事件，ACTION_DOWN时重置为false
    private final DecelerateInterpolator mDecelerateInterpolator;       //动画减速器

    private static final int[] LAYOUT_ATTRS = new int[]{
            android.R.attr.enabled
    };

    private HeadViewContainer mHeadViewContainer;           //下拉刷新的头
    private RelativeLayout mFooterViewContainer;            //上拉加载的头
    private int mHeaderViewIndex = -1;                      //header的绘制index，数字越高，绘制优先级更高
    private int mFooterViewIndex = -1;                      //footer的绘制index，数字越高，绘制优先级更高

    protected int mFrom;                                    //下拉刷新（移动到开始位置和移动到触发位置）动画的初始值
    private float mSpinnerFinalOffset;                      //progress 回弹开始的位置
    private boolean mNotify;                                //是否需要通知外部下拉刷新开始

    private int mHeaderViewWidth;
    private int mFooterViewWidth;
    private int mHeaderViewHeight;
    private int mFooterViewHeight;

    private int mPushDistance = 0;
    private float mTotalPullDragDistance = -1;              //触发上拉加载的滑动临界值

    private CircleProgressView defaultProgressView = null;

    private float density = 1.0f;

    /**
     * 下拉时，超过距离之后，弹回来的动画监听器
     */
    private AnimationListener mRefreshListener = new AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (mRefreshing) {
                if (mNotify) {
                    if (mListener != null) {
                        mListener.onRefresh();
                    }
                }
            } else {
                mHeadViewContainer.setVisibility(View.GONE);
                setTargetOffsetTopAndBottom(mOriginalOffsetTop - mCurrentTargetOffsetTop, true);
            }
            mCurrentTargetOffsetTop = mHeadViewContainer.getTop();
        }
    };

    /**
     * 添加头布局
     */
    public void setHeaderView(View child) {
        if (child == null) {
            return;
        }
        if (mHeadViewContainer == null) {
            return;
        }
        mHeadViewContainer.removeAllViews();
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(mHeaderViewWidth, mHeaderViewHeight);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mHeadViewContainer.addView(child, layoutParams);
    }

    /**
     * 添加footer布局
     */
    public void setFooterView(View child) {
        if (child == null) {
            return;
        }
        if (mFooterViewContainer == null) {
            return;
        }
        mFooterViewContainer.removeAllViews();
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(mFooterViewWidth, mFooterViewHeight);
        mFooterViewContainer.addView(child, layoutParams);
    }

    public SuperSwipeRefreshLayout(Context context) {
        this(context, null);
    }

    @SuppressWarnings("deprecation")
    public SuperSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        /**
         * getScaledTouchSlop是一个距离，表示滑动的时候，手的移动要大于这个距离才开始移动控件。如果小于这个距离就不触发移动控件
         */
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        setWillNotDraw(false);
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mHeaderViewWidth = display.getWidth();
        mFooterViewWidth = display.getWidth();
        mHeaderViewHeight = (int) (HEADER_VIEW_HEIGHT * metrics.density);
        mFooterViewHeight = (int) (FOOTER_VIEW_HEIGHT * metrics.density);
        defaultProgressView = new CircleProgressView(getContext());
        createHeaderViewContainer();
        createFooterViewContainer();
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
        density = metrics.density;
    }

    /**
     * 孩子节点绘制的顺序
     */
    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        // 将新添加的View,放到最后绘制
        if (mHeaderViewIndex < 0 && mFooterViewIndex < 0) {
            return i;
        }
        if (i == childCount - 2) {
            return mHeaderViewIndex;
        }
        if (i == childCount - 1) {
            return mFooterViewIndex;
        }
        int bigIndex = mFooterViewIndex > mHeaderViewIndex ? mFooterViewIndex
                : mHeaderViewIndex;
        int smallIndex = mFooterViewIndex < mHeaderViewIndex ? mFooterViewIndex
                : mHeaderViewIndex;
        if (i >= smallIndex && i < bigIndex - 1) {
            return i + 1;
        }
        if (i >= bigIndex || (i == bigIndex - 1)) {
            return i + 2;
        }
        return i;
    }

    /**
     * 创建头布局的容器
     */
    private void createHeaderViewContainer() {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                (int) (mHeaderViewHeight * 0.8),
                (int) (mHeaderViewHeight * 0.8));
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mHeadViewContainer = new HeadViewContainer(getContext());
        mHeadViewContainer.setVisibility(View.GONE);
        defaultProgressView.setVisibility(View.VISIBLE);
        defaultProgressView.setOnDraw(false);
        mHeadViewContainer.addView(defaultProgressView, layoutParams);
        addView(mHeadViewContainer);
    }

    /**
     * 添加底部布局
     */
    private void createFooterViewContainer() {
        mFooterViewContainer = new RelativeLayout(getContext());
        mFooterViewContainer.setVisibility(View.GONE);
        addView(mFooterViewContainer);
    }

    /**
     * 设置下拉刷新监听
     */
    public void setOnPullRefreshListener(OnPullRefreshListener listener) {
        mListener = listener;
    }

    /**
     * 设置上拉加载更多的接口
     */
    public void setOnPushLoadMoreListener(OnPushLoadMoreListener onPushLoadMoreListener) {
        this.mOnPushLoadMoreListener = onPushLoadMoreListener;
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setRefreshing(boolean refreshing) {
        if (refreshing && mRefreshing != refreshing) {
            setRefreshing(refreshing, false);
        } else {
            setRefreshing(refreshing, false /* notify */);
        }
    }

    private void setRefreshing(boolean refreshing, final boolean notify) {
        if (mRefreshing != refreshing) {
            mNotify = notify;
            ensureTarget();
            mRefreshing = refreshing;
            if (mListener != null) {
                mListener.onPullEnable(refreshing);
            }
            if (mRefreshing) {
                animateOffsetToCorrectPosition(mCurrentTargetOffsetTop, mRefreshListener);
            } else {
                animateOffsetToStartPosition(mCurrentTargetOffsetTop, null);
            }
        }
    }

    public boolean isRefreshing() {
        return mRefreshing;
    }

    public void setScrollTarget(View target) {
        vScrollTarget = target;
    }

    /**
     * 确保mTarget不为空<br>
     * mTarget一般是可滑动的ScrollView,ListView,RecyclerView等
     */
    private void ensureTarget() {
        if (mTarget == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(mHeadViewContainer) && !child.equals(mFooterViewContainer)) {
                    mTarget = child;
                    break;
                }
            }
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        int moveOffset = mCurrentTargetOffsetTop - mOriginalOffsetTop;
        int height = getMeasuredHeight() - getPaddingTop() - getPaddingBottom() - moveOffset;
        mTarget.measure(
                MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        );

        mHeadViewContainer.measure(
                MeasureSpec.makeMeasureSpec(mHeaderViewWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mHeaderViewHeight, MeasureSpec.EXACTLY)
        );

        mFooterViewContainer.measure(
                MeasureSpec.makeMeasureSpec(mFooterViewWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mFooterViewHeight, MeasureSpec.EXACTLY)
        );

        mSpinnerFinalOffset = mHeadViewContainer.getMeasuredHeight();
        mTotalPushDragDistance = mSpinnerFinalOffset;
        mTotalPullDragDistance = mFooterViewContainer.getMeasuredHeight();

        if (!mOriginalOffsetCalculated) {
            //如果没有初始化mOriginalOffsetTop的话
            mOriginalOffsetCalculated = true;
            mCurrentTargetOffsetTop = mOriginalOffsetTop = -mHeadViewContainer.getMeasuredHeight();
        }
        mHeaderViewIndex = -1;
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == mHeadViewContainer) {
                mHeaderViewIndex = index;
                break;
            }
        }
        mFooterViewIndex = -1;
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == mFooterViewContainer) {
                mFooterViewIndex = index;
                break;
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        int distance = mCurrentTargetOffsetTop + mHeadViewContainer.getMeasuredHeight();

        final View child = mTarget;
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();// 根据偏移量distance更新
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();

        child.layout(
                childLeft,
                childTop + distance - mPushDistance,        //如果下拉，child的top下移
                childLeft + childWidth,
                childTop + childHeight - mPushDistance   //如果上拉，child的bottom上移
        );// 更新目标View的位置


        int headViewWidth = mHeadViewContainer.getMeasuredWidth();
        int headViewHeight = mHeadViewContainer.getMeasuredHeight();
        mHeadViewContainer.layout(
                (width / 2 - headViewWidth / 2),
                mCurrentTargetOffsetTop,
                (width / 2 + headViewWidth / 2),
                mCurrentTargetOffsetTop + headViewHeight
        );// 更新头布局的位置
        int footViewWidth = mFooterViewContainer.getMeasuredWidth();
        int footViewHeight = mFooterViewContainer.getMeasuredHeight();
        mFooterViewContainer.layout(
                (width / 2 - footViewWidth / 2),
                height - mPushDistance,
                (width / 2 + footViewWidth / 2),
                height + footViewHeight - mPushDistance);
    }

    /**
     * 判断目标View是否滑动到顶部-还能否继续滑动
     */
    public boolean isChildScrollToTop() {
        if (Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return !(absListView.getChildCount() > 0 && (absListView
                        .getFirstVisiblePosition() > 0 || absListView
                        .getChildAt(0).getTop() < absListView.getPaddingTop()));
            } else {
                return !(mTarget.getScrollY() > 0);
            }
        } else {
            return !ViewCompat.canScrollVertically(mTarget, -1);
        }
    }

    /**
     * 是否滑动到底部
     */
    public boolean isChildScrollToBottom() {
        if (isChildScrollToTop()) {
            return false;
        }
        if (mTarget instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) mTarget;
            View lastChildView = recyclerView.getLayoutManager().getChildAt(recyclerView.getLayoutManager().getChildCount() - 1);
            int lastChildBottom = lastChildView.getBottom();
            int recyclerBottom = recyclerView.getHeight();
            int lastPosition = recyclerView.getLayoutManager().getPosition(lastChildView);

            //这里同样修改了RecyclerView滑动到最底部的判断方法，原方法当item超过一屏时，即失效
            if (lastChildBottom == recyclerBottom && lastPosition == recyclerView.getLayoutManager().getItemCount() - 1) {
                return true;
            }
            return false;
        } else if (mTarget instanceof AbsListView) {
            final AbsListView absListView = (AbsListView) mTarget;
            int count = absListView.getAdapter().getCount();
            int firstPos = absListView.getFirstVisiblePosition();
            if (firstPos == 0 && absListView.getChildAt(0).getTop() >= absListView.getPaddingTop()) {
                return false;
            }
            int lastPos = absListView.getLastVisiblePosition();
            if (lastPos > 0 && count > 0 && lastPos == count - 1) {
                //这里修改了原代码关于滑动到底部的判断标准，当item比较高时，很容易提前判断为滑动到底部
                View vLastItem = absListView.getChildAt(absListView.getChildCount() - 1);
                if (vLastItem != null) {
                    if (vLastItem.getBottom() == absListView.getHeight()) {
                        return true;
                    }
                }
            }
            return false;
        } else if (mTarget instanceof ScrollView) {
            ScrollView scrollView = (ScrollView) mTarget;
            View view = scrollView.getChildAt(scrollView.getChildCount() - 1);
            if (view != null) {
                int diff = (view.getBottom() - (scrollView.getHeight() + scrollView.getScrollY()));
                if (diff == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private float mInitialMotionY = -1;             //记录下拉刷新的起始y
    private float mInitialMotionYWithPush = -1;     //记录上拉加载的起始y

    private boolean mIsBeingDragged;                        //当前是否在上拉

    /**
     * 主要判断是否应该拦截子View的事件<br>
     * 如果拦截，则交给自己的OnTouchEvent处理<br>
     * 否者，交给子View处理<br>
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();

        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled() || mReturningToStart || mRefreshing || mLoadMore || (!isChildScrollToTop() && !isChildScrollToBottom())) {
            // 如果子View可以滑动，不拦截事件，交给子View处理-下拉刷新
            // 或者子View没有滑动到底部不拦截事件-上拉加载更多
            return false;
        }

        if (isChildScrollToTop() && !mPullEnable) {
            //如果用户禁止下拉刷新，则不处理事件
            return false;
        }

        if (isChildScrollToBottom() && !mPushEnable) {
            //如果用户禁止了上拉加载，则同样不处理事件
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                setTargetOffsetTopAndBottom(mOriginalOffsetTop - mHeadViewContainer.getTop(), true);
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                final float initialMotionY = getMotionEventY(ev, mActivePointerId);
                if (initialMotionY == -1) {
                    return false;
                }
                if (isChildScrollToTop()) {
                    mInitialMotionY = initialMotionY;
                }
                if (isChildScrollToBottom()) {
                    mInitialMotionYWithPush = initialMotionY;
                }

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }

                final float y = getMotionEventY(ev, mActivePointerId);
                if (y == -1) {
                    return false;
                }
                float yDiff = 0;

                if (isChildScrollToBottom()) {
                    if (mInitialMotionYWithPush == -1) {
                        mInitialMotionYWithPush = y;        //上拉的初始距离应该从此刻开始更新
                    }
                    yDiff = mInitialMotionYWithPush - y;// 计算上拉距离
                    if (yDiff > mTouchSlop && !mIsBeingDragged) {// 判断是否上拉的距离足够
                        mIsBeingDragged = true;// 正在上拉
                    }
                } else {
                    if (mInitialMotionY == -1) {
                        mInitialMotionY = y;
                    }
                    yDiff = y - mInitialMotionY;// 计算下拉距离
                    if (yDiff > mTouchSlop && !mIsBeingDragged) {// 判断是否下拉的距离足够
                        mIsBeingDragged = true;// 正在下拉
                    }
                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                mInitialMotionY = -1;
                mInitialMotionYWithPush = -1;
                break;
        }

        return mIsBeingDragged;// 如果正在拖动，则拦截子View的事件
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = MotionEventCompat.findPointerIndex(ev,
                activePointerId);
        if (index < 0) {
            return -1;
        }
        return MotionEventCompat.getY(ev, index);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // Nope.
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled() || mReturningToStart || (!isChildScrollToTop() && !isChildScrollToBottom())) {
            // 如果子View可以滑动，不拦截事件，交给子View处理
            return false;
        }

        if (isChildScrollToBottom()) {// 上拉加载更多
            return handlerPushTouchEvent(ev, action);
        } else {// 下拉刷新
            return handlerPullTouchEvent(ev, action);
        }
    }

    protected boolean mPushEnable = true;   //允许上拉加载

    protected boolean mPullEnable = true;   //允许下拉刷新

    /**
     * 设置是否允许下拉刷新
     */
    public void setOnPullEnable(boolean enable) {
        mPullEnable = enable;
    }

    /**
     * 设置是否允许上拉加载
     */
    public void setOnPushEnable(boolean enable) {
        mPushEnable = enable;
    }

    private boolean handlerPullTouchEvent(MotionEvent ev, int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //记录初始点
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;      //当前下拉距离
                if (mIsBeingDragged) {
                    float originalDragPercent = overscrollTop / mTotalPushDragDistance; // 下拉百分比
                    if (originalDragPercent < 0) {
                        return false;
                    }
                    float dragPercent = Math.min(1f, Math.abs(originalDragPercent));    //最小1%
                    if (mListener != null) {
                        mListener.onPull(dragPercent);
                    }
                    float extraOS = Math.abs(overscrollTop) - mTotalPushDragDistance;       //距离触发下拉刷新还有多少距离
                    float slingshotDist = mSpinnerFinalOffset;                          //回弹开始的位置
                    //下面两行没看懂，貌似是计算
                    float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, slingshotDist * 2) / slingshotDist);
                    float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow((tensionSlingshotPercent / 4), 2)) * 2f;
                    float extraMove = (slingshotDist) * tensionPercent * 2;
                    int targetY = mOriginalOffsetTop + (int) ((slingshotDist * dragPercent) + extraMove);
                    if (mHeadViewContainer.getVisibility() != View.VISIBLE) {
                        mHeadViewContainer.setVisibility(View.VISIBLE);
                    }
                    setTargetOffsetTopAndBottom(targetY - mCurrentTargetOffsetTop, true);
                }
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mActivePointerId == INVALID_POINTER) {
                    if (action == MotionEvent.ACTION_UP) {
                    }
                    return false;
                }
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
                mIsBeingDragged = false;
                if (overscrollTop > mTotalPushDragDistance) {
                    setRefreshing(true, true /* notify */);
                } else {
                    mRefreshing = false;
                    animateOffsetToStartPosition(mCurrentTargetOffsetTop, null);
                }
                mActivePointerId = INVALID_POINTER;
                return false;
            }
        }

        return true;
    }

    /**
     * 处理上拉加载更多的Touch事件
     */
    private boolean handlerPushTouchEvent(MotionEvent ev, int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                break;
            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float overscrollBottom = (mInitialMotionYWithPush - y) * DRAG_RATE;       //计算上拉的距离
                if (mIsBeingDragged) {
                    float originalDragPercent = overscrollBottom/mTotalPullDragDistance;    //上拉百分比
                    if(originalDragPercent < 0) {
                        return false;
                    }
                    float dragPercent = Math.min(1f, Math.abs(originalDragPercent));    //最小1%
                    mPushDistance = (int) overscrollBottom;
                    updateFooterViewPosition();
                    if (mOnPushLoadMoreListener != null) {
                        mOnPushLoadMoreListener.onPushDistance(dragPercent);
                        mOnPushLoadMoreListener.onPushEnable(mPushDistance >= mFooterViewHeight);
                    }
                }
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mActivePointerId == INVALID_POINTER) {
                    if (action == MotionEvent.ACTION_UP) {
                    }
                    return false;
                }
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev,
                        mActivePointerId);
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float overscrollBottom = (mInitialMotionYWithPush - y) * DRAG_RATE;// 松手是下拉的距离
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                if ((overscrollBottom < mFooterViewHeight) || mOnPushLoadMoreListener == null) {
                    //如果上拉距离不足或没有设置上拉加载监听，动画弹回
                    animatorFooterToBottom((int) overscrollBottom, 0);
                    return false;
                } else {// 下拉到mFooterViewHeight
                    mPushDistance = mFooterViewHeight;
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                    updateFooterViewPosition();
                    if (mPushDistance == mFooterViewHeight && mOnPushLoadMoreListener != null) {
                        mLoadMore = true;
                        mOnPushLoadMoreListener.onLoadMore();
                    }
                } else {
                    animatorFooterToBottom((int) overscrollBottom, mPushDistance);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * 松手之后，使用动画将Footer从距离start变化到end
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void animatorFooterToBottom(int start, final int end) {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(start, end);
        valueAnimator.setDuration(150);
        valueAnimator.addUpdateListener(new AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                // update
                mPushDistance = (Integer) valueAnimator.getAnimatedValue();
                updateFooterViewPosition();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (end > 0 && mOnPushLoadMoreListener != null) {
                    // start loading more
                    mLoadMore = true;
                    mOnPushLoadMoreListener.onLoadMore();
                } else {
                    resetTargetLayout();
                    mLoadMore = false;
                }
            }
        });
        valueAnimator.setInterpolator(mDecelerateInterpolator);
        valueAnimator.start();
    }

    /**
     * 设置停止加载
     */
    public void setLoadMore(boolean loadMore) {
        if (!loadMore && mLoadMore) {// 停止加载
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                mLoadMore = false;
                mPushDistance = 0;
                updateFooterViewPosition();
            } else {
                animatorFooterToBottom(mFooterViewHeight, 0);
            }
        }
    }

    //移动到触发下拉的位置
    private void animateOffsetToCorrectPosition(int from, AnimationListener listener) {
        mFrom = from;
        mAnimateToCorrectPosition.reset();
        mAnimateToCorrectPosition.setDuration(ANIMATE_TO_TRIGGER_DURATION);
        mAnimateToCorrectPosition.setInterpolator(mDecelerateInterpolator);
        if (listener != null) {
            mHeadViewContainer.setAnimationListener(listener);
        }
        mHeadViewContainer.clearAnimation();
        mHeadViewContainer.startAnimation(mAnimateToCorrectPosition);
    }

    //移动到开始的位置
    private void animateOffsetToStartPosition(int from, AnimationListener listener) {
        mFrom = from;
        mAnimateToStartPosition.reset();
        mAnimateToStartPosition.setDuration(ANIMATE_TO_START_DURATION);
        mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
        if (listener != null) {
            mHeadViewContainer.setAnimationListener(listener);
        }
        mHeadViewContainer.clearAnimation();
        mHeadViewContainer.startAnimation(mAnimateToStartPosition);
        resetTargetLayoutDelay(ANIMATE_TO_START_DURATION);
    }

    /**
     * 重置Target位置
     *
     * @param delay
     */
    public void resetTargetLayoutDelay(int delay) {
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                resetTargetLayout();
            }
        }, delay);
    }

    /**
     * 重置Target的位置
     */
    public void resetTargetLayout() {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        final View child = mTarget;
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();
        final int childWidth = child.getWidth() - getPaddingLeft() - getPaddingRight();
        final int childHeight = child.getHeight() - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);

        int headViewWidth = mHeadViewContainer.getMeasuredWidth();
        int headViewHeight = mHeadViewContainer.getMeasuredHeight();
        mHeadViewContainer.layout((width / 2 - headViewWidth / 2),
                -headViewHeight, (width / 2 + headViewWidth / 2), 0);// 更新头布局的位置
        int footViewWidth = mFooterViewContainer.getMeasuredWidth();
        int footViewHeight = mFooterViewContainer.getMeasuredHeight();
        mFooterViewContainer.layout((width / 2 - footViewWidth / 2), height,
                (width / 2 + footViewWidth / 2), height + footViewHeight);
    }

    //下拉刷新---移动到正确的位置
    private final Animation mAnimateToCorrectPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop = 0;
            int endTarget = 0;
            endTarget = (int) (mSpinnerFinalOffset - Math.abs(mOriginalOffsetTop));
            targetTop = (mFrom + (int) ((endTarget - mFrom) * interpolatedTime));
            int offset = targetTop - mHeadViewContainer.getTop();
            setTargetOffsetTopAndBottom(offset, false /* requires update */);
        }

        @Override
        public void setAnimationListener(AnimationListener listener) {
            super.setAnimationListener(listener);
        }
    };

    private void moveToStart(float interpolatedTime) {
        int targetTop = 0;
        targetTop = (mFrom + (int) ((mOriginalOffsetTop - mFrom) * interpolatedTime));
        int offset = targetTop - mHeadViewContainer.getTop();
        setTargetOffsetTopAndBottom(offset, false /* requires update */);
    }

    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveToStart(interpolatedTime);
        }
    };

    //移动指示器位置
    private void setTargetOffsetTopAndBottom(int offset, boolean requiresUpdate) {
        mCurrentTargetOffsetTop = mHeadViewContainer.getTop() + offset;
        //低版本系统上，页面刷新不及时导致下拉时，布局滞后
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            requestLayout();
        } else if (!isInLayout()) {
            requestLayout();
        }

        if (requiresUpdate && Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            invalidate();
        }
    }

    /**
     * 修改底部布局的位置-敏感pushDistance
     */
    private void updateFooterViewPosition() {
        mFooterViewContainer.setVisibility(View.VISIBLE);
        mFooterViewContainer.bringToFront();
        //针对4.4及之前版本的兼容
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            mFooterViewContainer.getParent().requestLayout();
        }
        mFooterViewContainer.offsetTopAndBottom(-mPushDistance);
    }


    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = MotionEventCompat.getPointerId(ev,
                    newPointerIndex);
        }
    }

    /**
     * @Description 下拉刷新布局头部的容器
     */
    private class HeadViewContainer extends RelativeLayout {

        private AnimationListener mListener;

        public HeadViewContainer(Context context) {
            super(context);
        }

        public void setAnimationListener(AnimationListener listener) {
            mListener = listener;
        }

        @Override
        public void onAnimationStart() {
            super.onAnimationStart();
            if (mListener != null) {
                mListener.onAnimationStart(getAnimation());
            }
        }

        @Override
        public void onAnimationEnd() {
            super.onAnimationEnd();
            if (mListener != null) {
                mListener.onAnimationEnd(getAnimation());
            }
        }
    }

    /**
     * 下拉刷新回调
     */
    public interface OnPullRefreshListener {
        void onRefresh();

        void onPull(float percent);

        void onPullEnable(boolean enable);
    }

    /**
     * 上拉加载更多
     */
    public interface OnPushLoadMoreListener {
        void onLoadMore();

        void onPushDistance(float percent);

        void onPushEnable(boolean enable);
    }

    /**
     * 默认的下拉刷新样式
     */
    public class CircleProgressView extends View implements Runnable {

        private static final int PEROID = 16;// 绘制周期
        private Paint progressPaint;
        private Paint bgPaint;
        private int width;// view的高度
        private int height;// view的宽度

        private boolean isOnDraw = false;
        private boolean isRunning = false;
        private int startAngle = 0;
        private int speed = 8;
        private RectF ovalRect = null;
        private RectF bgRect = null;
        private int swipeAngle;
        private int progressColor = 0xffcccccc;
        private int circleBackgroundColor = 0xffffffff;
        private int shadowColor = 0xff999999;

        public CircleProgressView(Context context) {
            super(context);
        }

        public CircleProgressView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public CircleProgressView(Context context, AttributeSet attrs,
                                  int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawArc(getBgRect(), 0, 360, false, createBgPaint());
            int index = startAngle / 360;
            if (index % 2 == 0) {
                swipeAngle = (startAngle % 720) / 2;
            } else {
                swipeAngle = 360 - (startAngle % 720) / 2;
            }
            canvas.drawArc(getOvalRect(), startAngle, swipeAngle, false,
                    createPaint());
        }

        private RectF getBgRect() {
            width = getWidth();
            height = getHeight();
            if (bgRect == null) {
                int offset = (int) (density * 2);
                bgRect = new RectF(offset, offset, width - offset, height
                        - offset);
            }
            return bgRect;
        }

        private RectF getOvalRect() {
            width = getWidth();
            height = getHeight();
            if (ovalRect == null) {
                int offset = (int) (density * 8);
                ovalRect = new RectF(offset, offset, width - offset, height
                        - offset);
            }
            return ovalRect;
        }

        public void setProgressColor(int progressColor) {
            this.progressColor = progressColor;
        }

        public void setCircleBackgroundColor(int circleBackgroundColor) {
            this.circleBackgroundColor = circleBackgroundColor;
        }

        public void setShadowColor(int shadowColor) {
            this.shadowColor = shadowColor;
        }

        /**
         * 根据画笔的颜色，创建画笔
         *
         * @return
         */
        private Paint createPaint() {
            if (this.progressPaint == null) {
                progressPaint = new Paint();
                progressPaint.setStrokeWidth((int) (density * 3));
                progressPaint.setStyle(Paint.Style.STROKE);
                progressPaint.setAntiAlias(true);
            }
            progressPaint.setColor(progressColor);
            return progressPaint;
        }

        private Paint createBgPaint() {
            if (this.bgPaint == null) {
                bgPaint = new Paint();
                bgPaint.setColor(circleBackgroundColor);
                bgPaint.setStyle(Paint.Style.FILL);
                bgPaint.setAntiAlias(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    this.setLayerType(LAYER_TYPE_SOFTWARE, bgPaint);
                }
                bgPaint.setShadowLayer(4.0f, 0.0f, 2.0f, shadowColor);
            }
            return bgPaint;
        }

        public void setPullDistance(int distance) {
            this.startAngle = distance * 2;
            postInvalidate();
        }

        @Override
        public void run() {
            while (isOnDraw) {
                isRunning = true;
                long startTime = System.currentTimeMillis();
                startAngle += speed;
                postInvalidate();
                long time = System.currentTimeMillis() - startTime;
                if (time < PEROID) {
                    try {
                        Thread.sleep(PEROID - time);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void setOnDraw(boolean isOnDraw) {
            this.isOnDraw = isOnDraw;
        }

        public void setSpeed(int speed) {
            this.speed = speed;
        }

        public boolean isRunning() {
            return isRunning;
        }

        @Override
        public void onWindowFocusChanged(boolean hasWindowFocus) {
            super.onWindowFocusChanged(hasWindowFocus);
        }

        @Override
        protected void onDetachedFromWindow() {
            isOnDraw = false;
            super.onDetachedFromWindow();
        }
    }

}
