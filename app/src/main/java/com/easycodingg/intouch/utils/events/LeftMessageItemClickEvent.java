package com.easycodingg.intouch.utils.events;

import com.easycodingg.intouch.models.Message;

public interface LeftMessageItemClickEvent {
    void onItemClick(Message message);
    void onDownloadButtonClick(Message message);
    void onDownloadCancelButtonClick(Message message);
}
