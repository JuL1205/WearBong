package com.gdg.wearbong;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.TextView;
import android.hardware.camera2.*;
import android.widget.*;
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

import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class MyActivity extends Activity implements DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    private String TAG = "jul";
    private GoogleApiClient mGoogleApiClient;
    private Node mPhoneNode = null;
//    WearGridPagerAdapter adapter;

    private ImageView mIvFrame;

    private MessageApi.MessageListener mMessageListener = new MessageApi.MessageListener() {
        @Override
        public void onMessageReceived (MessageEvent m){
            Scanner s = new Scanner(m.getPath());
            String command = s.next();
            Log.e("jul", "cmd = "+command);
            if(command.equals("preview")) {
                onReceivePreview(m.getData());
            }

        }
    };

    private void onReceivePreview(final byte[] data){
        Log.e("jul", "data length = "+data.length);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                mIvFrame.setImageBitmap(bmp);
            }
        });
    }

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        mContext = this;

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
//                final GridViewPager pager = (GridViewPager) findViewById(R.id.pager);
//                adapter = new WearGridPagerAdapter(MyActivity.this, getFragmentManager());
//                pager.setAdapter(adapter);


                mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                        .addApi(Wearable.API)
                        .addConnectionCallbacks(MyActivity.this)
                        .addOnConnectionFailedListener(MyActivity.this)
                        .build();

                mGoogleApiClient.connect();

                initViews();
            }
        });



    }

    private void initViews(){
        mIvFrame = (ImageView) findViewById(R.id.iv_frame);
    }

    @Override
    protected void onDestroy() {

        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            Wearable.MessageApi.removeListener(mGoogleApiClient, mMessageListener);

            mGoogleApiClient.disconnect();
        }

        super.onDestroy();
    }
    @Override
    public void onConnected(Bundle bundle) {
        Log.e(TAG, "wear - onConnected");

        Wearable.DataApi.addListener(mGoogleApiClient, this);

        findPhoneNode();

        Wearable.MessageApi.addListener(mGoogleApiClient, mMessageListener);
//        PutDataMapRequest dataMap = PutDataMapRequest.create("/count");
//        dataMap.getDataMap().putInt("count", 1205);
//        PutDataRequest request = dataMap.asPutDataRequest();
//        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
//                .putDataItem(mGoogleApiClient, request);
//
//        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
//            @Override
//            public void onResult(DataApi.DataItemResult dataItemResult) {
//                Log.e(TAG, "status = "+dataItemResult.getStatus());
//            }
//        });
    }
    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "wear - onConnectionSuspended");
    }
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "wear - onConnectionFailed");
    }
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d(TAG, "wear - DataItem deleted: " + event.getDataItem().getUri());
            } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                Log.d(TAG, "wear - DataItem changed: " + event.getDataItem().getUri());
            }
        }
    }
    public Bitmap loadBitmapFromAsset(Asset asset){
        if(asset == null){
            throw new IllegalArgumentException("Asset must be non-null");
        }

        ConnectionResult result = mGoogleApiClient.blockingConnect(3000, TimeUnit.MILLISECONDS);

        if(!result.isSuccess()){
            Log.e(TAG, "wear - connect google api fail");
            return null;
        }

        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(mGoogleApiClient, asset).await().getInputStream();
        mGoogleApiClient.disconnect();

        if(assetInputStream == null){
            Log.e(TAG, "wear - Requested an unknown Asset.");
            return null;
        }

        return BitmapFactory.decodeStream(assetInputStream);
    }

    //PageAdapter




    void findPhoneNode() {
        PendingResult<NodeApi.GetConnectedNodesResult> pending = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
        pending.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                if(result.getNodes().size()>0) {
                    mPhoneNode = result.getNodes().get(0);
                    Log.d(TAG, "Found wearable: name=" + mPhoneNode.getDisplayName() + ", id=" + mPhoneNode.getId());
                    sendToPhone("start", null, null);
                } else {
                    mPhoneNode = null;
                }
            }
        });
    }

    private void sendToPhone(String path, byte[] data, final ResultCallback<MessageApi.SendMessageResult> callback) {
        if (mPhoneNode != null) {
            PendingResult<MessageApi.SendMessageResult> pending = Wearable.MessageApi.sendMessage(mGoogleApiClient, mPhoneNode.getId(), path, data);
            pending.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult result) {
                    if (callback != null) {
                        callback.onResult(result);
                    }
                    if (!result.getStatus().isSuccess()) {
                        Log.d(TAG, "ERROR: failed to send Message: " + result.getStatus());
                    }
                }
            });
        } else {
            Log.d(TAG, "ERROR: tried to send message before device was found");
        }
    }
}
