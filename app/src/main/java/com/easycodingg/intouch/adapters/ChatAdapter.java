package com.easycodingg.intouch.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.easycodingg.intouch.R;
import com.easycodingg.intouch.databinding.ItemChatBinding;
import com.easycodingg.intouch.models.Chat;
import com.easycodingg.intouch.utils.CommonMethods;
import com.easycodingg.intouch.utils.events.ChatItemClickEvent;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    public List<Chat> chatList;
    private ChatItemClickEvent clickEvent;

    public ChatAdapter(List<Chat> chatList, ChatItemClickEvent clickEvent) {
        this.chatList = chatList;
        this.clickEvent = clickEvent;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatBinding binding = ItemChatBinding.inflate(LayoutInflater.from(parent.getContext()),
                parent, false);

        return new ChatViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);
        holder.bind(chat);
    }

    @Override
    public int getItemCount() { return chatList.size(); }

    public class ChatViewHolder extends RecyclerView.ViewHolder {
        private ItemChatBinding binding;

        public ChatViewHolder(ItemChatBinding binding) {
            super(binding.getRoot());

            this.binding = binding;

            binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(clickEvent != null) {
                        Chat chat = chatList.get(getBindingAdapterPosition());
                        clickEvent.onItemClick(chat);
                    }
                }
            });
        }

        public void bind(Chat chat) {
            if(chat.otherUser != null) {
                binding.txtChatName.setText(chat.otherUser.name);

                if(chat.otherUser.profileImageUrl != null) {
                    Glide.with(binding.getRoot())
                            .load(chat.otherUser.profileImageUrl)
                            .into(binding.imgChat);

                } else {
                    binding.imgChat.setImageResource(R.drawable.ic_user);
                }
            }

            if(chat.lastMessage != null) {
                binding.txtLastMsgContent.setText(CommonMethods.getMessageContentString(chat.lastMessage));
                binding.txtLastMsgTime.setText(CommonMethods.getChatMessageTime(chat.lastMessage));
                showMessageStatus(chat);
            }

            if(chat.unreadMessagesCount > 0) {
                binding.txtUnreadMessages.setText(Integer.toString(chat.unreadMessagesCount));
                binding.txtUnreadMessages.setVisibility(View.VISIBLE);
                binding.txtLastMsgContent.setTextAppearance(R.style.text_bold);
                hideMessageStatus();

            } else {
                binding.txtUnreadMessages.setVisibility(View.GONE);
                binding.txtLastMsgContent.setTextAppearance(R.style.text_normal);
                showMessageStatus(chat);
            }

            checkIfUserIsTyping(chat);
        }

        private void checkIfUserIsTyping(Chat chat) {
            if(chat.otherUser != null) {
                if(chat.typing.contains(chat.otherUser.id)) {
                    binding.txtLastMsgContent.setText("Typing...");
                    binding.txtLastMsgContent.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(),
                            R.color.green_300));

                    hideMessageStatus();

                } else {
                    if(chat.lastMessage != null) {
                        binding.txtLastMsgContent.setText(CommonMethods.getMessageContentString(chat.lastMessage));
                        binding.txtLastMsgTime.setText(CommonMethods.getChatMessageTime(chat.lastMessage));

                        binding.txtLastMsgContent.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(),
                                R.color.grey_300));

                        showMessageStatus(chat);
                    }
                }
            }
        }

        private void showMessageStatus(Chat chat) {
            if(chat.lastMessage == null || chat.unreadMessagesCount > 0) {
                hideMessageStatus();
                return;
            }

            binding.imgMessageStatus.setImageResource(CommonMethods.getMessageStatusImageId(chat.lastMessage.status));
            binding.imgMessageStatus.setImageTintList(ColorStateList.valueOf(
                    CommonMethods.getMessageStatusImageColor(itemView.getContext(), chat.lastMessage.status)));
            binding.imgMessageStatus.setVisibility(View.VISIBLE);
        }

        private void hideMessageStatus() {
            binding.imgMessageStatus.setVisibility(View.GONE);
        }
    }
}
