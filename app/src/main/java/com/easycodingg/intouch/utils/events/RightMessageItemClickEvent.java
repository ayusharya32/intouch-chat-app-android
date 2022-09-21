package com.easycodingg.intouch.utils.events;

import com.easycodingg.intouch.models.Message;

public interface RightMessageItemClickEvent {
    void onItemClick(Message message);
    void onUploadButtonClick(Message message);
    void onUploadCancelButtonClick(Message message);
}
