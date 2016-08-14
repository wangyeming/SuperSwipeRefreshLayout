package com.tianyeguang.superswiperefreshlayout;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.tianyeguang.superswiperefreshlayout.defaultswipe.DefaultSwipeLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * ListView Demo
 *
 * Created by wangyeming on 2016/8/15.
 */
public class ListViewActivity extends AppCompatActivity implements DefaultSwipeLayout.OnRefreshListener, DefaultSwipeLayout.OnLoadMoreListener {

    private DefaultSwipeLayout vDefaultSwipeLayout;
    private ArrayAdapter<Integer> mSimpleAdapter;

    private List<Integer> mData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);

        vDefaultSwipeLayout = (DefaultSwipeLayout) findViewById(R.id.activity_list_view);
        ListView vList = (ListView) findViewById(R.id.child_lv);

        initData();
        mSimpleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mData);
        vList.setAdapter(mSimpleAdapter);

        vDefaultSwipeLayout.setOnLoadMoreListener(this);
        vDefaultSwipeLayout.setOnRefreshListener(this);
    }

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
                mSimpleAdapter.notifyDataSetChanged();
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
                mSimpleAdapter.notifyDataSetChanged();
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
}
