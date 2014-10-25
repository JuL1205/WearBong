package com.gdg.wearbong;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by owner on 14. 10. 23..
 */
public class DataLayerListenerService extends WearableListenerService {
    private static final String TAG = "jul";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);
    }
    @Override
    public void onMessageReceived(MessageEvent m) {
        if(m.getPath().equals("start")) {
            Intent startIntent = new Intent(this, MyActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
        }
    }
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        // i don't care
    }

}
