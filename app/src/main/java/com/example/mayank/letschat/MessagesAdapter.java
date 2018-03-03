package com.example.mayank.letschat;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

/**
 * Created by mayank on 3/3/18.
 */

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {

    ArrayList<ChatMessage> messages;
    static FirebaseAuth  mFirebaseAuth;
    static FirebaseUser user;
    public static String userName = MainActivity.ANONYMOUS;
    public ListItemClickListener listener;

    //Layouts
    public static final int otherUsers = R.layout.item_message;
    public static final int currentUser = R.layout.item_message_user;

    //Click Listener
    public interface ListItemClickListener{
        void onListItemClick(String imageRes);
    }

    public MessagesAdapter(ArrayList<ChatMessage> messages,ListItemClickListener listener)
    {
        this.messages = messages;
        this.listener = listener;
    }

    public void changeUserName()
    {
        mFirebaseAuth = FirebaseAuth.getInstance();
        user = mFirebaseAuth.getCurrentUser();
        userName = user.getDisplayName();
        this.notifyDataSetChanged();
    }


    @Override
    public int getItemViewType(int position) {
        if(userName.equals(messages.get(position).getName()))
            return currentUser;
        else
            return otherUsers;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(viewType,parent,false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final ChatMessage message = messages.get(position);
        if(getItemViewType(position) == otherUsers)
        {
            if(position>0 && message.getName().equals(messages.get(position-1).getName()))
            {
                holder.authorTextView.setVisibility(View.GONE);
            }else
                holder.authorTextView.setVisibility(View.VISIBLE);
        }
        boolean isPhoto = message.getPhotoUrl() != null;
        if (isPhoto) {
            holder.messageTextView.setVisibility(View.GONE);
            holder.photoImageView.setVisibility(View.VISIBLE);
            Glide.with(holder.photoImageView.getContext())
                    .load(message.getPhotoUrl())
                    .into(holder.photoImageView);
            holder.imageRes = message.getPhotoUrl();
        } else {
            holder.messageTextView.setVisibility(View.VISIBLE);
            holder.photoImageView.setVisibility(View.GONE);
            holder.messageTextView.setText(message.getText());
        }
        holder.authorTextView.setText(message.getName());
    }

    @Override
    public int getItemCount() {
        if(messages == null)
            return 0;
        else
            return messages.size();
    }

    public void swapArrayList(ArrayList<ChatMessage> newMessages)
    {
        messages = newMessages;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        ImageView photoImageView;
        TextView messageTextView;
        TextView authorTextView;
        CardView cardView;
        String imageRes=null;
        public ViewHolder(View convertView) {
            super(convertView);
            photoImageView = (ImageView) convertView.findViewById(R.id.photoImageView);
            messageTextView = (TextView) convertView.findViewById(R.id.messageTextView);
            authorTextView = (TextView) convertView.findViewById(R.id.nameTextView);
            cardView = (CardView)convertView.findViewById(R.id.cardView);
            photoImageView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            listener.onListItemClick(imageRes);
        }

        public void setRes(String imageRes)
        {
            this.imageRes = imageRes;
        }
    }
}
