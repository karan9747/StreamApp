package com.vasukam.karan.StreamApp;


import static io.agora.rtc.video.VideoEncoderConfiguration.STANDARD_BITRATE;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;


public class Stream extends AppCompatActivity {


    private RtcEngine mRtcEngine;

    public String channelName;
    private int channelProfile = 0;
    Context baseContext;
    RequestQueue requestQueue;
    public String results;
    public static String newData="";
    public String sendnow;
    static String actualResult;
    int flag =1;
    static String finalname;

    private static final String TAG = "backEndTrade";
    private static final String NAME = "channelName";
    private static final String ETA = "expiryTime";
    private static final String TOKEN = "tokenID";
    String token;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final DocumentReference documentReference = db.collection("users").document("Hqwam03IlsVDmdkG5OYf");

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onUserJoined(final int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setupRemoteVideo(uid);
                }
            });
        }
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed)
        {
            Log.i(TAG, String.format("onJoinChannelSuccess channel %s uid %d", channel, uid));
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    System.out.println("successfully joined a channel");
                }
                });
            }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    onRemoteUserLeft();
                }
            });
        }
    };

    private void setupRemoteVideo(int uid) {
        FrameLayout container = findViewById(R.id.remote_video_view_container);
        if (container.getChildCount()>=1){return;
        }
        SurfaceView surfaceView = RtcEngine.CreateRendererView(baseContext);
        /*surfaceView.setZOrderMediaOverlay(true);
        checking whether the overlay is working or not.*/
        container.addView(surfaceView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid));
        surfaceView.setTag(uid);
        Toast.makeText(this, "remote video started", Toast.LENGTH_SHORT).show();
    }

    private void onRemoteUserLeft(){
        Toast.makeText(this, "remote user left",Toast.LENGTH_SHORT).show();
        FrameLayout frameLayout= (FrameLayout) findViewById(R.id.remote_video_view_container);
        frameLayout.removeAllViews();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseContext=this.getBaseContext();
        setContentView(R.layout.activity_stream);
        ImageView imageView = findViewById(R.id.endcall);
        ImageView imageView2 = findViewById(R.id.toggleCam);
        ImageView imageView3 = findViewById(R.id.muteBtn);
        ImageView imageView4 = findViewById(R.id.btnChat);




        //button handle part  of the stream application.
        imageView3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if(imageView3.isSelected()){

                if (flag == 1)
                mRtcEngine.muteAllRemoteAudioStreams(true);
                else if(flag == 0) {
                    mRtcEngine.muteAllRemoteAudioStreams(false);
                    flag = 1;
                }
            }
            }
        });
        imageView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRtcEngine.switchCamera();
            }
        });
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDestroy();
                System.exit(0);
                Intent intent = new Intent(Stream.this, MainActivity.class);
                startActivity(intent);
            }
        });

        //program
        baseContext=  getApplicationContext();
        Intent intent = getIntent();
        channelName = intent.getStringExtra("channelMessage");
        channelProfile = intent.getIntExtra("profileMessage",-1);
        if (channelProfile == -1) {
            Log.e("tag ", "No profile");
            Toast.makeText(this, "channel selection error",Toast.LENGTH_SHORT).show();
            System.exit(0);
        }

        else if (channelProfile==1) {
            jsonParse();
            //start engine
            initAgoraEngineAndJoinChannel();
        }
        else if (channelProfile==0){
           getDatax(channelName);
           newData=token;
            if (token==null){
                Toast.makeText(this, "Key does not hold the actual token", Toast.LENGTH_SHORT).show();
                /*Intent intent3 = new Intent(Stream.this, MainActivity.class);
                startActivity(intent3);
                System.exit(0);*/
            }
            else Toast.makeText(this, "token value is  "+token, Toast.LENGTH_SHORT).show();
        }





    }
    public void getDatax(String channelName2){
        documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()){
                   if(documentSnapshot.getString(NAME).equals(channelName2)) {
                        token = documentSnapshot.getString(TOKEN);
                        finalname= documentSnapshot.getString(NAME);

                        Toast.makeText(Stream.this, token, Toast.LENGTH_LONG).show();
                        initAgoraEngineAndJoinChannel(token,finalname);
                   }else Toast.makeText(Stream.this, "Channel name does not match", Toast.LENGTH_LONG).show();
                }

                else System.out.println("Document does not exist");
                Log.d(TAG, "onSuccess: token gotten from database");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, e.toString());
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRtcEngine.leaveChannel();
        RtcEngine.destroy();
        mRtcEngine=null;
    }

    private void initAgoraEngineAndJoinChannel(String token1, String finalname1) {
        initalizeAgoraEngine();


        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        mRtcEngine.setClientRole(channelProfile);
        mRtcEngine.enableVideo();
        if (channelProfile==1){
            setupLocalVideo();
        }else {
            FrameLayout frameLayout= findViewById(R.id.local_video_view_container);
            frameLayout.setVisibility(View.INVISIBLE);
            setupRemoteVideo(0);
        }
        setupVideoProfile();
        joinChannel(token1,finalname1);
    }

    private void joinChannel (String token2,String finalname2){

        mRtcEngine.joinChannel("006db01d6c676014af8a76614eac46c6a8fIAD9n/DJU2x2xHYMhNZ4Ky5DStRi1NJ9i0b/MJJzaZX8W0Xs1dgh39v0EAAJVtXNVGS8YQEAAQAAAAAA", finalname2,null, 0);
        Toast.makeText(this,"channel joined" + token, Toast.LENGTH_SHORT).show();
    }


    private void initAgoraEngineAndJoinChannel() {
        initalizeAgoraEngine();


        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        mRtcEngine.setClientRole(channelProfile);
        mRtcEngine.enableVideo();
        if (channelProfile==1){
            setupLocalVideo();
        }else {
            FrameLayout frameLayout= findViewById(R.id.local_video_view_container);
            frameLayout.setVisibility(View.INVISIBLE);
        }
        setupVideoProfile();
        joinChannel();
    }



    private void setupVideoProfile() {
        mRtcEngine.enableVideo();
        mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(VideoEncoderConfiguration.VD_480x480, VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));

    }
    public void setupLocalVideo() {
        FrameLayout container = findViewById(R.id.local_video_view_container);
        SurfaceView surfaceView = RtcEngine.CreateRendererView(baseContext);
        surfaceView.setZOrderMediaOverlay(true);
        container.addView(surfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0));
    }

    private void joinChannel (){
        
        mRtcEngine.joinChannel(sendnow, channelName,null, 0);
        Toast.makeText(this,"channel joined" + token, Toast.LENGTH_SHORT).show();
    }


    private void initalizeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(baseContext, constants.appID, mRtcEventHandler);
        } catch (Exception e) {
            System.out.println(e);
        }
    }


    public  String jsonParse() {
        int uid=0;
        requestQueue = Volley.newRequestQueue(this);

        String url = "https://videofinal.herokuapp.com/access_token?channel=" + channelName + "&uid=" + uid;
        Toast.makeText(this, url, Toast.LENGTH_SHORT).show();
        JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                results= response.toString();
                try {
                    JSONObject jsonObject = new JSONObject(results);
                  //  for (int i=0;i<jsonObject.length(); i++){
                        actualResult = jsonObject.getString("token");
                        Toast.makeText(Stream.this, "results = "+ actualResult, Toast.LENGTH_SHORT).show();
                   // }
                } catch (JSONException e) {
                    Toast.makeText(Stream.this, e.getMessage(),Toast.LENGTH_SHORT).show();
                } finally {
                    sendnow = actualResult;
                    Toast.makeText(Stream.this, "send now now has  = "+ sendnow, Toast.LENGTH_SHORT).show();
                    newData = enterData(channelName,sendnow);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(Stream.this, error.getMessage(), Toast.LENGTH_SHORT ).show();
            }
        });
        requestQueue.add(stringRequest);
        return sendnow;
    }


    //this part implements the database part.
    public String enterData( String  channelName, String thisTOKEN){
        Map<String, Object> note = new HashMap<>();
        note.put(NAME, channelName);
        note.put(ETA,true);
        note.put(TOKEN,thisTOKEN);
        documentReference.set(note)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Successfully entered token data");
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure: data entry failed!");
            }
        });
        return thisTOKEN;
    }






}