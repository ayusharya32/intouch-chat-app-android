package com.easycodingg.intouch.utils.events;

import com.google.firebase.firestore.DocumentSnapshot;

public interface OnDocSnapshotListenerTriggered {
    void onListenerTriggered(DocumentSnapshot documentSnapshot);
}
