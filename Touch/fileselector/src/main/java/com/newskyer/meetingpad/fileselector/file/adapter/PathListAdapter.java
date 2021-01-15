package com.newskyer.meetingpad.fileselector.file.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.newskyer.meetingpad.R;

import java.util.List;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class PathListAdapter extends RecyclerView.Adapter<PathListAdapter.ViewHolder> {
    private Context context;
    private List<PathData> list;
    private OnItemClickListener listener = null;
    private View view;
    public PathListAdapter(List<PathData> list) {
        this.list = list;
    }

    public static class PathData {
        public String path;
        public int selector;
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iv;
        TextView tv;

        public ViewHolder(View itemView) {
            super(itemView);
            iv = (ImageView) itemView.findViewById(R.id.image_back_to_parent);
            tv = (TextView) itemView.findViewById(R.id.dir_name);

        }
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        context = parent.getContext();
        View convertView = LayoutInflater.from(context).inflate(R.layout.path_select_item, parent, false);
        ViewHolder holder = new ViewHolder(convertView);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        holder.tv.setText(list.get(position).path);
        if(1==list.get(position).selector) {
            holder.tv.setTextColor(context.getResources().getColor(R.color.pressed_color));
            view = holder.tv;
        } else {
            holder.tv.setTextColor(context.getResources().getColor(R.color.normal_color));
        }
        if (position == list.size() - 1) {
            holder.iv.setVisibility(View.GONE);
        } else {
            holder.iv.setVisibility(View.VISIBLE);
        }
        holder.tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView textView = (TextView) view.findViewById(R.id.dir_name);
                textView.setTextColor(context.getResources().getColor(R.color.normal_color));

                holder.tv.setTextColor(context.getResources().getColor(R.color.pressed_color));
                view = holder.tv;
                if (listener != null)
                    listener.onItemClick(v,position);

            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public interface OnItemClickListener{
        void onItemClick(View view, int position);
    }
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

}
