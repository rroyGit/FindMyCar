package com.release.rroycsdev.findmycar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.HandlerThread;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Handler;

public class ImageOCRAdapter extends RecyclerView.Adapter<ImageOCRAdapter.ImageOCRViewHolder>{
    private List<ImageDataClass> data;
    private Context context;
    private CameraFragListener cameraFragListener;



    public ImageOCRAdapter(List<ImageDataClass> data, Context context, CameraFragListener cameraFragListener) {
        this.data = data;
        this.context = context;
        this.cameraFragListener = cameraFragListener;
    }

    @Override
    public ImageOCRViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.image_items, parent, false);

        ImageOCRViewHolder imageOCRViewHolder = new ImageOCRViewHolder(itemView);
        imageOCRViewHolder.imageView.setOnClickListener(view -> {
            LatLng latLng = data.get(imageOCRViewHolder.getAdapterPosition()).getLatLng();
            Marker marker = data.get(imageOCRViewHolder.getAdapterPosition()).getMarker();
            cameraFragListener.setMarkersOnImageClick(latLng, marker);
        });

        return imageOCRViewHolder;
    }


    @Override
    public void onBindViewHolder(ImageOCRViewHolder holder, int position) {
        holder.bind(data.get(position));
    }


    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setData(List<ImageDataClass> newData) {
        if (data != null) {
            PostDiffCallback postDiffCallback = new PostDiffCallback(data, newData);
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(postDiffCallback);
            data.clear();
            data.addAll(newData);
            diffResult.dispatchUpdatesTo(this);
        }
    }

    class PostDiffCallback extends DiffUtil.Callback {

        private final List<ImageDataClass> oldPosts, newPosts;

        public PostDiffCallback(List<ImageDataClass> oldPosts, List<ImageDataClass> newPosts) {
            this.oldPosts = oldPosts;
            this.newPosts = newPosts;
        }

        @Override
        public int getOldListSize() {
            return oldPosts.size();
        }

        @Override
        public int getNewListSize() {
            return newPosts.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldPosts.get(oldItemPosition).getPath().equals(newPosts.get(newItemPosition).getPath());
        }


        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldPosts.get(oldItemPosition).equals(newPosts.get(newItemPosition));
        }
    }

    class ImageOCRViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;
        private TextView textView;

        ImageOCRViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageOCR);
            textView = itemView.findViewById(R.id.myText);
        }

        void bind(final ImageDataClass post) {
            if (post != null) {
                imageView.setImageBitmap(post.getCompressedBitmap());
                textView.setText(post.getPath().substring(post.getPath().indexOf("Camera/") + "Camera/".length()));
            }
        }
    }
}
