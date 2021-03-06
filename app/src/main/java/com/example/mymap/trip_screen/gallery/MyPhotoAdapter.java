package com.example.mymap.trip_screen.gallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymap.R;

import java.util.List;

public class MyPhotoAdapter extends RecyclerView.Adapter<MyPhotoAdapter.PhotoViewHolder> {

    private List<String> mListPhotoPath;
    private OnItemListener mOnItemListener;

    public MyPhotoAdapter(List<String> mListPhotoPath, OnItemListener mOnItemListener) {
        this.mListPhotoPath = mListPhotoPath;
        this.mOnItemListener = mOnItemListener;
    }

    public void setData(List<String> list)
    {
        mListPhotoPath = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_item, parent, false);
        return new PhotoViewHolder(view, mOnItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        String path = mListPhotoPath.get(position);
        if(path == null)
            return;

        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = 16;
        Bitmap bitmap = BitmapFactory.decodeFile(path, opt);
        holder.imageView.setImageBitmap(bitmap);
    }

    @Override
    public int getItemCount() {
        if(mListPhotoPath != null)
            return mListPhotoPath.size();
        return 0;
    }

    public class PhotoViewHolder extends RecyclerView.ViewHolder implements  View.OnClickListener{
        private ImageView imageView;
        private OnItemListener onItemListener;
        public PhotoViewHolder(@NonNull View itemView, OnItemListener listener) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_photo_test);
            onItemListener = listener;
            imageView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onItemListener.onItemClick(getAdapterPosition());
        }
    }
    public interface OnItemListener{
        void onItemClick(int position);
    }
}
