package cn.autoeditor.bilibili;

import android.app.Application;

import xyz.doikki.videoplayer.ijk.IjkPlayerFactory;
import xyz.doikki.videoplayer.player.AndroidMediaPlayerFactory;
import xyz.doikki.videoplayer.player.VideoViewConfig;
import xyz.doikki.videoplayer.player.VideoViewManager;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        VideoViewManager.setConfig(VideoViewConfig.newBuilder()
                //使用MediaPlayer解码
                .setPlayerFactory(IjkPlayerFactory.create())
//                .setPlayerFactory(AndroidMediaPlayerFactory.create())
                .build());
    }
}
