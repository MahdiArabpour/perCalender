package com.aydhamm.percalender;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ListViewAdapter extends ArrayAdapter {

    TextView fileName, filePath;

    public ListViewAdapter(@NonNull Context context, @NonNull File[] files) {
        super(context, R.layout.list_view_item, files);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater myInflater = LayoutInflater.from(getContext());
        View customView = myInflater.inflate(R.layout.list_view_item, parent, false);

        // references:
        fileName = customView.findViewById(R.id.item_file_name);
        filePath = customView.findViewById(R.id.item_file_path);

        File singleFile = (File)getItem(position);

        fileName.setText(singleFile.getName());
        filePath.setText(singleFile.getPath().substring(20));

        return customView;
    }
}
