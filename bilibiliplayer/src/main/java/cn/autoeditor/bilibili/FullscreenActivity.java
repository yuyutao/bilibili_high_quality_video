package cn.autoeditor.bilibili;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import xyz.doikki.videocontroller.StandardVideoController;
import xyz.doikki.videoplayer.player.VideoView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        Configs configs = new Configs(this) ;
        Intent intent;
        if(TextUtils.isEmpty(configs.getEmail())){
            intent = new Intent(this, Settings.class) ;
        }else{
            intent = new Intent(this, BilibiliPlayer.class) ;
        }

        startActivity(intent);
        finish();
    }

}