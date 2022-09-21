package com.easycodingg.intouch.utils.diffutil;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.easycodingg.intouch.models.Message;

public class MessageDiffUtil extends DiffUtil.ItemCallback<Message> {
        @Override
        public boolean areItemsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
            return oldItem.equals(newItem);
        }
    }