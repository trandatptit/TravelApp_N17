package com.example.travelapp.Adapter;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.travelapp.Domain.ChatMessage;
import com.example.travelapp.R;
import io.noties.markwon.Markwon;
import java.util.ArrayList;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private ArrayList<ChatMessage> chatMessages;
    private Markwon markwon;
    
    public ChatAdapter(ArrayList<ChatMessage> chatMessages, Markwon markwon) {
        this.chatMessages = chatMessages;
        this.markwon = markwon;
    }
    
    @Override
    public int getItemViewType(int position) {
        return chatMessages.get(position).isUser() ? 0 : 1;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) { // User message
            view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_user, parent, false);
        } else { // Bot message
            view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_bot, parent, false);
        }
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);
        
        if (message.isUser()) {
            // Regular text for user messages
            holder.messageText.setText(message.getMessage());
        } else {
            // Markdown rendering for bot messages
            markwon.setMarkdown(holder.messageText, message.getMessage());
        }
    }
    
    @Override
    public int getItemCount() {
        return chatMessages.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        android.widget.TextView messageText;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageTextView);
        }
    }
} 