package adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.touch.R;

import java.io.File;
import java.util.List;

public class MySaveFileAdapter extends BaseAdapter
{
    private LayoutInflater mInflater;
    private Bitmap mIcon1;
    private Bitmap mIcon2;
    private Bitmap mIcon3;
    private Bitmap mIcon4;
    private List<String> items;
    private List<String> paths;
    public MySaveFileAdapter(Context context, List<String> it, List<String> pa)
    {
        mInflater = LayoutInflater.from(context);
        items = it;
        paths = pa;
        mIcon1 = BitmapFactory.decodeResource(context.getResources(),R.drawable.folder);
        mIcon2 = BitmapFactory.decodeResource(context.getResources(),R.drawable.folder);
        mIcon3 = BitmapFactory.decodeResource(context.getResources(),R.drawable.folder);
        mIcon4 = BitmapFactory.decodeResource(context.getResources(),R.drawable.file);
    }

    @Override
    public int getCount()
    {
        return items.size();
    }
    @Override
    public Object getItem(int position)
    {
        return items.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder holder;

        if(convertView == null)
        {
            convertView = mInflater.inflate(R.layout.save_file, null);
            holder = new ViewHolder();
            holder.text = (TextView) convertView.findViewById(R.id.save_text);
            holder.icon = (ImageView) convertView.findViewById(R.id.save_icon);

            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder) convertView.getTag();
        }
        File f=new File(paths.get(position).toString());
        if(items.get(position).toString().equals("b1"))
        {
            holder.text.setText("返回主目录..");
            holder.icon.setImageBitmap(mIcon1);
        }
        else if(items.get(position).toString().equals("b2"))
        {
            holder.text.setText("返回上一层..");
            holder.icon.setImageBitmap(mIcon2);
        }
        else
        {
            holder.text.setText(f.getName());
            if(f.isDirectory())
            {
                holder.icon.setImageBitmap(mIcon3);
            }
            else
            {
                holder.icon.setImageBitmap(mIcon4);
            }
        }
        return convertView;
    }
    private class ViewHolder
    {
        TextView text;
        ImageView icon;
    }
}