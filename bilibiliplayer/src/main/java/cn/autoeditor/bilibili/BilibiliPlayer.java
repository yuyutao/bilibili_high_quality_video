package cn.autoeditor.bilibili;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.strictmode.Violation;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Callable;

import cn.autoeditor.sharelibrary.LtLog;
import cn.autoeditor.sharelibrary.PartInfo;
import cn.autoeditor.sharelibrary.VideoDatabase;
import cn.autoeditor.sharelibrary.VideoInfo;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import xyz.doikki.videocontroller.StandardVideoController;
import xyz.doikki.videoplayer.player.BaseVideoView;
import xyz.doikki.videoplayer.player.VideoView;

public class BilibiliPlayer extends AppCompatActivity implements BaseVideoView.OnStateChangeListener, EmailSyncer.OnNewVideoInfoCallback {
    private static final String TAG = BilibiliPlayer.class.getName() ;
    private static final String BASE_URL = "https://www.bilibili.com/video/" ;

    private static final String CURRENT_INFO = "current_play" ;

    private static final String KEY_CURRENT_VIDEO = "current_video" ;
    private static final String KEY_CID_INDEX = "cid_index";
    private static final String KEY_CURRENT_POSITION = "position";
    private VideoView mVideoView;
    StandardVideoController controller ;
    private EmailSyncer mEmailSyncer ;
    private VideoDatabase mVideoDatabase ;
    private VideoInfo mCurrentVideoInfo;
    private PartInfo mCurrentPartInfo;

    private Stack<VideoInfo> mPlayList ;
    private int mCidIndex = 0 ;
    private SharedPreferences mSharedPreferences ;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        mVideoView = findViewById(R.id.player) ;

        mPlayList = new Stack<>() ;
        mSharedPreferences = getSharedPreferences(CURRENT_INFO, MODE_PRIVATE) ;

        mVideoDatabase = VideoDatabase.getInstance(this) ;
        mEmailSyncer = new EmailSyncer(this) ;
        mEmailSyncer.setOnNewVideoInfoCallback(this);
        mEmailSyncer.start();
        String videoInfo = mSharedPreferences.getString(KEY_CURRENT_VIDEO,null) ;
        long position = 0;
        if(videoInfo != null){
            mCurrentVideoInfo = VideoInfo.fromJson(videoInfo) ;
            mCidIndex = mSharedPreferences.getInt(KEY_CID_INDEX,0) ;
            position = mSharedPreferences.getLong(KEY_CURRENT_POSITION,0) ;
        }else{
            mCurrentVideoInfo = mVideoDatabase.getNext(-1) ;
            mCidIndex = 0 ;
        }
        controller = new StandardVideoController(this);
        mVideoView.setVideoController(controller); //设置控制器

        mVideoView.addOnStateChangeListener(this);
        if(mCurrentVideoInfo != null){
            mPlayList.add(mCurrentVideoInfo) ;
            play(position);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoView.pause();
        if(mCurrentVideoInfo != null) {
            mSharedPreferences.edit().putString(KEY_CURRENT_VIDEO, mCurrentVideoInfo.toJson()).commit();
            mSharedPreferences.edit().putInt(KEY_CID_INDEX, mCidIndex).commit();
            mSharedPreferences.edit().putLong(KEY_CURRENT_POSITION, mVideoView.getCurrentPosition()).commit() ;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoView.resume();
    }

    @SuppressLint("AutoDispose")
    private void play( long postion){

        mCurrentPartInfo = mCurrentVideoInfo.partInfos.get(mCidIndex) ;

        Single.fromCallable(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        return BibiliVideoInfo.getVideoUrl(mCurrentVideoInfo.bvid, mCurrentPartInfo.cid) ;
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(String s) {
                        Log.i(TAG,"video url:"+s) ;
                        if(s != null){
                            String title ;
                            if(mCurrentVideoInfo.partInfos.size() == 1){
                                title = mCurrentVideoInfo.title ;
                            }else{
                                title = mCurrentPartInfo.title ;
                            }
                            controller.removeAllControlComponent() ;
                            controller.addDefaultControlComponent(title, false);
                            mVideoView.setUrl(s, createPlayerVideoHeader(mCurrentVideoInfo.bvid));
                            mVideoView.skipPositionWhenPlay((int) (postion != 0?postion:mCurrentPartInfo.timestamp));
                            mVideoView.start();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mEmailSyncer.stop();
    }

    private boolean forwarding = false ;
    private long lastNextKeyTime = 0 ;

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if(event.getAction()== KeyEvent.ACTION_DOWN){

            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    break ;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if(forwarding){
                        break ;
                    }
                    forwarding = true ;
                    mVideoView.setSpeed(2);
//                    mHandler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            if(forwarding ) {
//                                mVideoView.seekTo(mVideoView.getCurrentPosition() + 500);
//                            }
//                            mHandler.postDelayed(this, 100) ;
//                        }
//                    },100) ;
                    break ;

            }
            return super.dispatchKeyEvent(event) ;
        }

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if(System.currentTimeMillis() - lastNextKeyTime <= 500){
                    showSkipCollectionDialog() ;
                }else {
                    startNext();
                    lastNextKeyTime = System.currentTimeMillis() ;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                startLast();
                break ;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                break ;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                Log.i(TAG, "set speed ..."+mVideoView.getSpeed()) ;

                mVideoView.setSpeed(1);
                forwarding = false ;
                break;
            case KeyEvent.KEYCODE_MENU :
            case KeyEvent.KEYCODE_SPACE :
                Settings.launchActivity(this) ;
                break ;
        }
        return super.dispatchKeyEvent(event) ;
    }

    private void showSkipCollectionDialog(){
        if(mCurrentVideoInfo != null && mCurrentVideoInfo.partInfos.size() > 1){
            new AlertDialog.Builder(this)
                    .setTitle(R.string.hint)
                    .setMessage(getString( R.string.skip_collection_msg,mCurrentVideoInfo.title))
                    .setNegativeButton(R.string.cancel,null)
                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startNextCollection() ;
                        }
                    }).show() ;

        }
    }

    private void startNext(){

        if(mCurrentVideoInfo == null || !mCurrentVideoInfo.skipable){
            return;
        }
        mCidIndex++ ;
        if(mCidIndex >= mCurrentVideoInfo.partInfos.size()){
            mCurrentVideoInfo = mVideoDatabase.getNext(mCurrentVideoInfo.getId()) ;
            mCidIndex = 0 ;
            if(mCurrentVideoInfo == null){
                return;
            }
            mPlayList.add(mCurrentVideoInfo) ;
        }
        mSharedPreferences.edit().putString(KEY_CURRENT_VIDEO, mCurrentVideoInfo.toJson()).commit();
        mSharedPreferences.edit().putInt(KEY_CID_INDEX, mCidIndex).commit();
        mVideoView.release();
        play(0);

    }
    private void startNextCollection(){

        if(mCurrentVideoInfo == null || !mCurrentVideoInfo.skipable){
            return;
        }
        mCurrentVideoInfo = mVideoDatabase.getNext(mCurrentVideoInfo.getId()) ;
        mCidIndex = 0 ;
        if(mCurrentVideoInfo == null){
                return;
        }
        mPlayList.add(mCurrentVideoInfo) ;
        mSharedPreferences.edit().putString(KEY_CURRENT_VIDEO, mCurrentVideoInfo.toJson()).commit();
        mSharedPreferences.edit().putInt(KEY_CID_INDEX, mCidIndex).commit();
        mVideoView.release();
        play(0);

    }
    private void startLast(){
        if(mCurrentVideoInfo == null || !mCurrentVideoInfo.skipable){
            return;
        }
        mVideoDatabase.playInfo(mCurrentVideoInfo.bvid, mCurrentPartInfo.cid, mVideoView.getCurrentPosition());
        mCidIndex-- ;
        if(mCidIndex < 0){
            if(mPlayList.empty()){
                mCidIndex++ ;//还原
                return ;
            }
            mCurrentVideoInfo =mPlayList.pop() ;
            mCidIndex = mCurrentVideoInfo.partInfos.size()-1 ;
        }
        mSharedPreferences.edit().putString(KEY_CURRENT_VIDEO, mCurrentVideoInfo.toJson()).commit();
        mSharedPreferences.edit().putInt(KEY_CID_INDEX, mCidIndex).commit();
        mVideoView.release();
        play(0);
    }

    public static Map<String, String> createPlayerVideoHeader(String id) {
        HashMap<String, String> header = new HashMap<>(3);
        header.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.5112.81 Safari/537.36 Edg/104.0.1293.54");
        header.put("Connection", "keep-alive");
        header.put("Referer", id.startsWith("BV") ? ("https://www.bilibili.com/video/" + id) : ("https://www.bilibili.com/bangumi/play/ep" + id));

        return header;
    }

    @Override
    public void onPlayerStateChanged(int playerState) {
        Log.i(TAG, "onPlayerStateChanged:"+playerState) ;
    }

    @Override
    public void onPlayStateChanged(int playState) {
        Log.i(TAG, "onPlayStateChanged:"+playState) ;

        if(playState == BaseVideoView.STATE_PLAYBACK_COMPLETED){
            mVideoView.release();
            mCidIndex++ ;
            if(mCidIndex >= mCurrentVideoInfo.partInfos.size()){
                mCurrentVideoInfo = mVideoDatabase.getNext(mCurrentVideoInfo.getId()) ;
                mCidIndex = 0 ;
                if(mCurrentVideoInfo == null){
                    return;
                }
                mPlayList.add(mCurrentVideoInfo) ;
            }
            mSharedPreferences.edit().putString(KEY_CURRENT_VIDEO, mCurrentVideoInfo.toJson()).commit();
            mSharedPreferences.edit().putInt(KEY_CID_INDEX, mCidIndex).commit();
            play(0);
        }

    }

    private void delHistory(VideoInfo delInfo){
        if(delInfo.partInfos == null){
            return;
        }
        Iterator<VideoInfo> iterator = mPlayList.iterator() ;
        VideoInfo videoInfo = null;
        while (iterator.hasNext()){
            VideoInfo info = iterator.next() ;
            if(info.bvid.equals(delInfo.bvid)){
                videoInfo = info ;
                break ;
            }
        }
        if(videoInfo == null){
            return ;
        }

        if(videoInfo.partInfos == null){
            return;
        }

        Iterator<PartInfo> iter = videoInfo.partInfos.iterator() ;
        while (iter.hasNext()){
            PartInfo partInfo = iter.next() ;
            if(delInfo.partInfos.contains(partInfo)) {
                iter.remove();
            }
        }
        if(videoInfo.partInfos.size() == 0){
            mPlayList.remove(videoInfo) ;
        }

    }

    @Override
    public void onNewVideo(VideoInfo info) {
        LtLog.i("onNewVideo:"+info.title) ;
        switch (info.action){
            case VideoInfo.ACTION_ADD :
                if(mCurrentVideoInfo == null){
                    mCurrentVideoInfo = info ;
                    mCidIndex = 0 ;
                    play(0);
                }
                break ;
            case VideoInfo.ACTION_DEL :
                delHistory(info);
                break ;
        }
    }
}