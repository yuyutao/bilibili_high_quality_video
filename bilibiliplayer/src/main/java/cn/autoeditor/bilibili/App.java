package cn.autoeditor.bilibili;

import android.app.Application;

import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import java.net.CookieStore;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import xyz.doikki.videoplayer.exo.ExoMediaPlayerFactory;
import xyz.doikki.videoplayer.ijk.IjkPlayerFactory;
import xyz.doikki.videoplayer.player.AndroidMediaPlayerFactory;
import xyz.doikki.videoplayer.player.VideoViewConfig;
import xyz.doikki.videoplayer.player.VideoViewManager;

public class App extends Application {


    SharedPrefsCookiePersistor sharedPrefsCookiePersistor;
    private static App sInstance ;
    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this ;
        VideoViewManager.setConfig(VideoViewConfig.newBuilder()
                //使用MediaPlayer解码
                .setPlayerFactory(IjkPlayerFactory.create())
//                .setPlayerFactory(ExoMediaPlayerFactory.create())
//                .setPlayerFactory(AndroidMediaPlayerFactory.create())
                .build());

        sharedPrefsCookiePersistor = new SharedPrefsCookiePersistor(this) ;
    }

    public static SharedPrefsCookiePersistor getCookieJar(){
        return sInstance.sharedPrefsCookiePersistor;
    }
}
