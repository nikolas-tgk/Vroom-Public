package com.p17142.vroom.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.p17142.vroom.databinding.ItemContainerRecentConvertationBinding;
import com.p17142.vroom.listeners.ConversationListener;
import com.p17142.vroom.models.Message;
import com.p17142.vroom.models.User;
import com.p17142.vroom.utilities.ImageUtils;

import java.util.List;

public class RecentConversationAdapter extends RecyclerView.Adapter<RecentConversationAdapter.ConversationViewHolder> {

    private final List<Message> directMessages;
    private final ConversationListener conversationListener;

    private String loggedUserUid;

    public RecentConversationAdapter(List<Message> directMessages, ConversationListener conversationListener, String loggedUserIUid) {
        this.directMessages = directMessages;
        this.conversationListener = conversationListener;
        this.loggedUserUid = loggedUserIUid;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        return new ConversationViewHolder(ItemContainerRecentConvertationBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        holder.setData(directMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return directMessages.size();
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder{

        ItemContainerRecentConvertationBinding binding;

        public ConversationViewHolder(ItemContainerRecentConvertationBinding itemContainerRecentConvertationBinding) {
            super(itemContainerRecentConvertationBinding.getRoot());
            binding = itemContainerRecentConvertationBinding;
        }

        void setData(Message message){
            binding.profileImageView.setImageBitmap(ImageUtils.decodeImage(message.getConversationImage()));
            binding.conversationNameText.setText(message.getConversationName());
            if(!message.getMostRecentReceiver().equals(loggedUserUid))
            {
                binding.recentMessageText.setText("You: "+ message.getMessage());
            }
            else{
                binding.recentMessageText.setText(message.getConversationName()+": "+ message.getMessage());
            }
            binding.getRoot().setOnClickListener(v -> {
                User user = new User(message.getConversationUid(), message.getConversationName(), message.getConversationImage());
                conversationListener.onConversationClicked(user);
            });
        }
    }
}
