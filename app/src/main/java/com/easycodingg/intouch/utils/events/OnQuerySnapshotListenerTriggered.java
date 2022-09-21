package com.easycodingg.intouch.utils.events;

import com.google.firebase.firestore.QuerySnapshot;

public interface OnQuerySnapshotListenerTriggered {
    void onListenerTriggered(QuerySnapshot querySnapshot);
}
