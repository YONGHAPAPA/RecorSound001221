package com.example.recordsound;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class RecordListAdapter extends BaseAdapter {
    ArrayList<RecordItem> recordList = new ArrayList<RecordItem>();

    RecordListAdapter(){

    }

    RecordListAdapter(ArrayList<RecordItem> recordList){
        this.recordList = recordList;
    }

    @Override
    public int getCount() {
        return this.recordList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.recordList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return this.recordList.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final int pos = position;
        final Context context = parent.getContext();

        if(convertView == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.record_list_item, parent, false);
        }

        TextView txv_recordTitle = (TextView) convertView.findViewById(R.id.txt_recordTitle);
        TextView txv_recordAddDate = (TextView) convertView.findViewById(R.id.txt_recordAddDate);

        RecordItem item = recordList.get(position);

        //Log.d("item.getDisplayName()", item.getDisplayName());
        txv_recordTitle.setText(item.getDisplayName());
        txv_recordAddDate.setText(item.getAddDate());

        return convertView;
    }

    public void addItem(Drawable icon,Long id, String displayName, String addDate, String size){
        RecordItem item = new RecordItem(icon, id, displayName, addDate, size);
        recordList.add(item);
    }
}
