package com.gdg.wearbong;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.util.Scanner;


public class MyActivity extends ActionBarActivity implements
        DataApi.DataListener{

    private GoogleApiClient mGoogleApiClient;
    private Node mWearNode = null;

    private int CapturePhotoTime = 0;

    protected MessageApi.MessageListener mMessageListener = new MessageApi.MessageListener(){ // listener
        @Override

        public void onMessageReceived(MessageEvent messageEvent) {
            Scanner s = new Scanner(messageEvent.getPath());
            String command = s.next();
            if(command.compareTo("capture") == 0){ // to capture
            setTimer(0);
            } else if(command.compareTo("timer") == 0){
              int args = s.nextInt();
              setTimer(args);
            } else if(command.compareTo("???") == 0){

            } else {
                //getPreview(~~);
            }

            s.close();
        }
    };

    private void setTimer(int args){ CapturePhotoTime = args * 1000; }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void sendPreview(Bitmap previewPic){
        if(previewPic == null){
            Log.e("jul", "bm is null");
        } else{
            Log.e("jul", "bm is not null");
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        previewPic.compress(Bitmap.CompressFormat.WEBP, 50, stream); // PNG && 80% quailty
        sendToWear("preview", stream.toByteArray(), null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d("jul", "onConnected : " + bundle);
                        findWearNode();
                        Wearable.MessageApi.addListener(mGoogleApiClient, mMessageListener);
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d("jul", "onConnetionSuspended : " + i);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d("jul", "onConnectionFailed : " + connectionResult);
                    }
                })
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onStop() {
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d("jul", "DataItem deleted: " + event.getDataItem().getUri());
            } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                Log.d("jul", "DataItem changed: " + event.getDataItem().getUri());
            }
        }
    }


    private Asset createAssetFromBitmap(Bitmap bitmap){
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
        return Asset.createFromBytes(bos.toByteArray());
    }

    void findWearNode() {
        PendingResult<NodeApi.GetConnectedNodesResult> pending = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
        pending.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                if(result.getNodes().size()>0) {
                    mWearNode = result.getNodes().get(0);
                    Bitmap testBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher);
                    sendPreview(testBitmap);
                } else {
                    mWearNode = null;
                }
            }
        });
    }

    private void sendToWear(String path, byte[] data, final ResultCallback<MessageApi.SendMessageResult> callback) {
        if (mWearNode != null) {
            Log.d("jul", "send image = "+data.length);
            PendingResult<MessageApi.SendMessageResult> pending = Wearable.MessageApi.sendMessage(mGoogleApiClient, mWearNode.getId(), path, data);
            pending.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult result) {
                    if (callback != null) {
                        callback.onResult(result);
                    }
                    if (!result.getStatus().isSuccess()) {
                        Log.d("jul", "ERROR: failed to send Message: " + result.getStatus());
                    }
                }
            });
        } else {
            Log.d("jul", "ERROR: tried to send message before device was found");
        }
    }
}
