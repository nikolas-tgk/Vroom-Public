package com.p17142.vroom.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.p17142.vroom.databinding.ReceivedMessageContainerBinding;
import com.p17142.vroom.databinding.SentMessageContainerBinding;
import com.p17142.vroom.models.Message;

import java.util.List;

public class DirectMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Bitmap receiverProfileImage;
    private final List<Message> messages;
    private final String senderUid;
    public static final int VIEW_TYPE_RECEIVED = 0;
    public static final int VIEW_TYPE_SENT = 1;

    public DirectMessageAdapter(Bitmap receiverProfileImage, List<Message> messages, String senderUid) {
        this.receiverProfileImage = receiverProfileImage;
        this.messages = messages;
        this.senderUid = senderUid;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_SENT) {
            return new SentMessageViewHolder(SentMessageContainerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
         else{
            return new ReceivedMessageViewHolder(ReceivedMessageContainerBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(getItemViewType(position) == VIEW_TYPE_SENT)
        {
            ((SentMessageViewHolder) holder).setData(messages.get(position));
        }else{
            ((ReceivedMessageViewHolder) holder).setData(messages.get(position), receiverProfileImage);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    /** determine the message type (is it received/sent?)
     *
     * @param position position to query
     * @return weather this message a received or a sent message in relation to logged user
     */
    @Override
    public int getItemViewType(int position) {
        if(messages.get(position).getSenderUid().equals(senderUid)){
            return VIEW_TYPE_SENT;
        }else{
            return VIEW_TYPE_RECEIVED;
        }
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final SentMessageContainerBinding binding;

        SentMessageViewHolder(SentMessageContainerBinding sentMessageContainerBinding) {
            super(sentMessageContainerBinding.getRoot());
            setIsRecyclable(false);
            binding = sentMessageContainerBinding;
        }

        void setData(Message message){
            binding.messageTextView.setText(message.getMessage());
            binding.dateTimeTextView.setText(message.getDateTime());
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final ReceivedMessageContainerBinding binding;

        ReceivedMessageViewHolder(ReceivedMessageContainerBinding receivedMessageContainerBinding) {
            super(receivedMessageContainerBinding.getRoot());
            setIsRecyclable(false); // not the most efficient fix !TO-DO
            binding = receivedMessageContainerBinding;
        }

        void setData(Message message, Bitmap profileImage){
            binding.receivedMsgTextView.setText(message.getMessage());
            binding.receiveDateTimeTextView.setText(message.getDateTime());
            binding.profileMsgImageView.setImageBitmap(profileImage);
        }
    }
}
