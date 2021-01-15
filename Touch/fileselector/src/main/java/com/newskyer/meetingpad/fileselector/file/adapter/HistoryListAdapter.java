package com.newskyer.meetingpad.fileselector.file.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.newskyer.meetingpad.R;
import com.newskyer.meetingpad.fileselector.model.TabInfo;

import java.util.List;

public class HistoryListAdapter extends BaseAdapter {

    private Context context;
    private List<String> list;

    public HistoryListAdapter(Context context,int layout, List<String> datas) {
        super();
        this.context = context;

        this.list = datas;
    }
    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
       MyViewHolder holder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.history_listview_item, parent, false);
            holder = new MyViewHolder();
            holder.tv = (TextView) convertView.findViewById(R.id.historyDiritem);
            convertView.setTag(holder);
        } else {
            holder = (MyViewHolder) convertView.getTag();
        }
        holder.tv.setText(list.get(position));
//        holder.tv.requestFocus();
//        holder.tv.setFocusable(true);
//        holder.tv.setSelected(true);
        return convertView;
    }
    public static class MyViewHolder {
        TextView tv;
    }
}
