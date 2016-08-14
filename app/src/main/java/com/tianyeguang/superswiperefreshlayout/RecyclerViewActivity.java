package com.tianyeguang.superswiperefreshlayout;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class RecyclerViewActivity extends BaseDemoActivity {

    private SimpleRecyclerAdapter mAdapter;

    @Override
    protected void init() {
        setContentView(R.layout.activity_recycler_view);
        RecyclerView vRecycler = (RecyclerView) findViewById(R.id.child_recycler);
        vRecycler.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new SimpleRecyclerAdapter(this, mData);
        vRecycler.setAdapter(mAdapter);
    }

    @Override
    public void notifyAdapterChanged() {
        mAdapter.notifyDataSetChanged();
    }

    private class SimpleRecyclerAdapter extends RecyclerView.Adapter<SimpleRecyclerAdapter.SimpleViewHolder> {

        private Context mContext;
        private List<Integer> mData = new ArrayList<>();

        SimpleRecyclerAdapter(Context context, List<Integer> data) {
            mContext = context;
            mData = data;
        }

        @Override
        public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View contentView = LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new SimpleViewHolder(contentView);
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        @Override
        public void onBindViewHolder(SimpleViewHolder holder, int position) {
            holder.vContent.setText(String.valueOf(mData.get(position)));
        }

        class SimpleViewHolder extends RecyclerView.ViewHolder{

            SimpleViewHolder(View itemView) {
                super(itemView);
                vContent = (TextView) itemView.findViewById(android.R.id.text1);
            }

            TextView vContent;
        }
    }
}
