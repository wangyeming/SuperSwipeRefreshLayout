package com.tianyeguang.superswiperefreshlayout;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import com.tianyeguang.superswiperefreshlayout.defaultswipe.DefaultSwipeLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * 基类activity
 *
 * Created by wangyeming on 2016/8/15.
 */
public abstract class BaseDemoActivity extends AppCompatActivity implements DefaultSwipeLayout.OnRefreshListener, DefaultSwipeLayout.OnLoadMoreListener {

    protected DefaultSwipeLayout vDefaultSwipeLayout;
    protected List<Integer> mData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
        init();
        vDefaultSwipeLayout = (DefaultSwipeLayout) findViewById(R.id.default_swipe);
        vDefaultSwipeLayout.setOnLoadMoreListener(this);
        vDefaultSwipeLayout.setOnRefreshListener(this);
    }

    protected abstract void init();

    @Override
    public void onLoadMore() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                int lastValue = mData.get(mData.size() - 1);
                for (int i = lastValue; i < lastValue + 30; i++) {
                    mData.add(i);
                }
                vDefaultSwipeLayout.setLoadMore(false);
                notifyAdapterChanged();
            }
        }, 3000);

    }

    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                initData();
                vDefaultSwipeLayout.setRefreshing(false);
                notifyAdapterChanged();
            }
        }, 3000);
        initData();
    }

    private void initData() {
        mData.clear();
        for (int i = 0; i < 30; i++) {
            mData.add(i);
        }
    }

    public abstract void notifyAdapterChanged();

}
