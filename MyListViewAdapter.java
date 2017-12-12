package mclovin.lowlevelchat;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class MyListViewAdapter extends BaseAdapter
{
    Context mContext;
    ArrayList<String> content;
    LayoutInflater layoutInflater;

    public MyListViewAdapter(Context mContext, ArrayList<String> content)
    {
        this.mContext = mContext;
        this.content = content;
        this.layoutInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount()
    {
        return content.size();
    }

    @Override
    public Object getItem(int position)
    {
        return content.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View row = layoutInflater.inflate(R.layout.ips_view, parent, false);

        //ImageView imgView = (ImageView)row.findViewById(R.id.imageView);
        TextView textView = (TextView)row.findViewById(R.id.textView);

        String ipAndName = (String)getItem(position);
        if (!ipAndName.contains("\n"))
        {
            ipAndName = "User" + '\n' + ipAndName;
            content.set(position, ipAndName);
        }

        textView.setText(ipAndName);

        return row;
    }
}
