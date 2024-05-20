package com.example.ambicare;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class SettingAdapter extends BaseAdapter {

    private List<SettingItem> settingItems;
    private LayoutInflater inflater;
    private Context context;

    public SettingAdapter(Context context, List<SettingItem> settingItems) {
        this.context = context;
        this.settingItems = settingItems;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return settingItems.size();
    }

    @Override
    public Object getItem(int position) {
        return settingItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_setting, parent, false);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(R.id.itemIcon);
            holder.name = convertView.findViewById(R.id.itemText);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        SettingItem settingItem = settingItems.get(position);
        holder.icon.setImageResource(settingItem.getIcon());
        holder.name.setText(settingItem.getName());

        return convertView;
    }

    static class ViewHolder {
        ImageView icon;
        TextView name;
    }
}
