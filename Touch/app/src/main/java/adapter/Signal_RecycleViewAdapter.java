package adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.touch.R;
import com.example.touch.SignalCanvasView;

import java.util.List;

import fragment_package.Signal_fragment;

public class Signal_RecycleViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final String TAG = "myText";
    private List<String> signalList;

    public void setNormalColor(int normalColor) {
        this.normalColor = normalColor;
    }

    private int normalColor = 0;

    //定义一个监听对象，用来存储监听事件
    public OnItemClickListener mOnItemClickListener;
    Signal_fragment signal_fragment;

    public Signal_RecycleViewAdapter(List<String> list, Signal_fragment signal_fragment) {
        signalList = list;
        this.signal_fragment = signal_fragment;
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.test_item, parent, false);
        RecyclerView.ViewHolder holder = new MyViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MyViewHolder myHolder = (MyViewHolder)holder;
        String text = signalList.get(position);
        myHolder.tv.setText(text);

        myHolder.tv.setBackgroundColor(normalColor);
        for(int i = 0; i < Signal_fragment.checkSignalMapList.size();i++)
        {
            if(position == Signal_fragment.checkSignalMapList.get(i).get("position"))
            {
                int checkColorNum = Signal_fragment.checkSignalMapList.get(i).get("color");
                myHolder.tv.setBackgroundColor(Signal_fragment.signalColorS.getColor(checkColorNum,0));
            }
        }

//        myHolder.tv.setBackgroundColor();
//        Log.e(TAG, "onBindViewHolder: "+ myHolder.tv.getText() + "position = " + position);
        myHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signal_fragment.listenClickSignalItem(view,position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return  signalList == null ? 0 : signalList.size();
    }
    public void removeData(int position)
    {
        signalList.remove(position);
        notifyItemRemoved(position);
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener itemClickListener) {
        mOnItemClickListener = itemClickListener;
    }

    //定义OnItemClickListener的接口,便于在实例化的时候实现它的点击效果
    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

//    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView tv;
        private LinearLayout linearLayout;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.test_item_text);
            linearLayout = (LinearLayout)itemView;
//            linearLayout.setOnClickListener(this);
        }

//        @Override
//        public void onClick(View view) {
//            if(mOnItemClickListener!=null) {
//                //此处调用的是onItemClick方法，而这个方法是会在RecyclerAdapter被实例化的时候实现
//                mOnItemClickListener.onItemClick(view, getLayoutPosition());
//            }
//
//        }
    }

}
