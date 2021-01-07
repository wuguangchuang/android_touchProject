package adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.touch.R;

import java.util.ArrayList;
import java.util.List;

public class SignalAdapter extends BaseAdapter {
    private static final String TAG = "myText";
    List<String> list;
    Context context;
    TextView textView;

    public SignalAdapter(Context context,List<String> list)
    {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if(view == null)
        {
            view = LayoutInflater.from(context).inflate(R.layout.test_item,viewGroup,false);
            textView = view.findViewById(R.id.test_item_text);
            view.setTag(textView);
        }
        else
        {
            textView = (TextView) view.getTag();
        }
        textView.setText(list.get(i));
        Log.e(TAG, "getView: " + list.get(i));
        return view;
    }
}
