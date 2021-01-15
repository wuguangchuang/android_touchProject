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

public class NavigatioListAdapter extends BaseAdapter {
    private int type;
    private Context context;
    private List<TabInfo> list;
    private int selectedPosition = -1;

    public NavigatioListAdapter(Context context, int layoutId, List<TabInfo> datas, int selectType) {
        super();
        this.context = context;
        this.type = selectType;
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


    public void setSelectedPosition(int position) {

        selectedPosition = position;

    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MyViewHolder holder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.layout_selector_tab, parent, false);
            holder = new MyViewHolder();
            holder.iv = (ImageView) convertView.findViewById(R.id.image_file_tab);
            holder.tv = (TextView) convertView.findViewById(R.id.text_file_tab);
            convertView.setTag(holder);
        } else {
            holder = (MyViewHolder) convertView.getTag();
        }
        holder.iv.setImageResource(list.get(position).getResource());
        holder.tv.setText(list.get(position).getTab());

        if (selectedPosition == position) {
            convertView.setBackgroundResource(R.drawable.navigation__bg_pressed);
            holder.iv.setSelected(true);
            holder.tv.setTextColor(context.getResources().getColor(R.color.pressed_color));
        }
        else {
            convertView.setBackgroundResource(R.drawable.navigation__bg_normal);
            holder.iv.setSelected(false);
            holder.tv.setTextColor(context.getResources().getColor(R.color.normal_color));
        }
        return convertView;
    }

    public static class MyViewHolder {
        ImageView iv;
        TextView tv;
    }
}

