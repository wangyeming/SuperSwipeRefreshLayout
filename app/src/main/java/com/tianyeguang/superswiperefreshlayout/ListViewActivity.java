package com.tianyeguang.superswiperefreshlayout;

import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * ListView Demo
 *
 * Created by wangyeming on 2016/8/15.
 */
public class ListViewActivity extends BaseDemoActivity{

    private ArrayAdapter<Integer> mSimpleAdapter;

    @Override
    protected void init() {
        setContentView(R.layout.activity_list_view);
        ListView vList = (ListView) findViewById(R.id.child_lv);
        mSimpleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mData);
        vList.setAdapter(mSimpleAdapter);
    }


    @Override
    public void notifyAdapterChanged() {
        mSimpleAdapter.notifyDataSetChanged();
    }
}
