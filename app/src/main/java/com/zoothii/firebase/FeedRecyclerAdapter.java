package com.zoothii.firebase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class FeedRecyclerAdapter extends RecyclerView.Adapter<FeedRecyclerAdapter.PostHolder> {

    ArrayList<String> userEmailList;
    ArrayList<String> userCommentList;
    ArrayList<String> userImageList;

    public FeedRecyclerAdapter(ArrayList<String> userEmailList, ArrayList<String> userCommentList, ArrayList<String> userImageList) {
        this.userEmailList = userEmailList;
        this.userCommentList = userCommentList;
        this.userImageList = userImageList;
    }

    @NonNull
    @Override
    public PostHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.recycler_row,parent,false);
        return new PostHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostHolder holder, int position) {
        holder.feed_emailText.setText(userEmailList.get(position));
        holder.feed_commentText.setText(userCommentList.get(position));
        Picasso.get().load(userImageList.get(position)).into(holder.feed_imageView);
    }

    @Override
    public int getItemCount() {
        return userEmailList.size();
    }

    class PostHolder extends RecyclerView.ViewHolder {

        ImageView feed_imageView;
        EditText feed_commentText;
        TextView feed_emailText;

        public PostHolder(@NonNull View itemView) {
            super(itemView);

            feed_imageView = itemView.findViewById(R.id.feed_imageView);
            feed_commentText = itemView.findViewById(R.id.feed_commentText);
            feed_emailText = itemView.findViewById(R.id.feed_emailText);

        }
    }
}
