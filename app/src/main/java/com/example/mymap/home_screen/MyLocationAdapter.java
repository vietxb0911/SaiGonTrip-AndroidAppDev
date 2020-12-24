package com.example.mymap.home_screen;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.example.mymap.database.MyLocation;
import com.example.mymap.R;

import java.util.ArrayList;

public class MyLocationAdapter extends ArrayAdapter<MyLocation>  implements CompoundButton.OnCheckedChangeListener {
    Context _context;
    int _layoutResourceID;
    ArrayList<MyLocation> mArrayListLocations = null;
    SparseBooleanArray _checkStates;

    public MyLocationAdapter(@NonNull Context context, int textViewResourceId,
                             @NonNull ArrayList<MyLocation> objects) {
        super(context, textViewResourceId, objects);
        _context = context;
        _layoutResourceID = textViewResourceId;
        mArrayListLocations = objects;
        _checkStates = new SparseBooleanArray(mArrayListLocations.size());
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(_context);
            convertView = layoutInflater.inflate(_layoutResourceID, parent, false);

            holder = new ViewHolder();

            holder.pictureView = (ImageView)convertView.findViewById(R.id.imgView_locationPicture);
            holder.locationName = (TextView)convertView.findViewById(R.id.textView_locationName);
            holder.checkBox = (CheckBox) convertView.findViewById(R.id.btn_checkbox);

            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        MyLocation location = mArrayListLocations.get(position);

        Glide.with(_context).load(location.getIcon()).into(holder.pictureView);
        holder.locationName.setText(location.getName());

        holder.checkBox.setTag(position);
        holder.checkBox.setChecked(_checkStates.get(position, false));
        holder.checkBox.setOnCheckedChangeListener(this);

        return convertView;
    }

    public SparseBooleanArray get_checkStates() {
        return _checkStates;
    }

    @Override
    public int getCount() {
        return mArrayListLocations.size();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        _checkStates.put((Integer) buttonView.getTag(), isChecked);
    }

    static class ViewHolder{
        ImageView pictureView;
        TextView locationName;
        CheckBox checkBox;
    }

}
