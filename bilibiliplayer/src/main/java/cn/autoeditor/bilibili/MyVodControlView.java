package cn.autoeditor.bilibili;

import static xyz.doikki.videoplayer.util.PlayerUtils.stringForTime;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.widget.SeekBar;

import androidx.annotation.NonNull;

import cn.autoeditor.sharelibrary.LtLog;
import xyz.doikki.videocontroller.component.VodControlView;


/**
 * 点播底部控制栏
 */
public class MyVodControlView extends VodControlView {

    public static final int NORMAL_PLAY = 0 ;
    public static final int FAST_FORWARD = 1 ;
    public static final int REWIND = 2 ;
    private View mViewNext ;
    private SeekBar mSeekBar ;

    private int lastProgress = 0 ;

    private float seekSpeed = 2.0f ;
    private int mSeekBarState = NORMAL_PLAY ;
    public MyVodControlView(@NonNull Context context) {
        super(context);
        mViewNext = findViewById(R.id.iv_next) ;
        mSeekBar = findViewById(R.id.seekBar) ;

    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mSeekBar.dispatchKeyEvent(event);
    }

    protected int getLayoutId() {
        return R.layout.layout_vod_control_view ;
    }
    public void setOnNextClickListener(OnClickListener nextClickListener){
        mViewNext.setOnClickListener(nextClickListener);
    }

    public void setFastforwarding(int direction){
        mSeekBarState = direction ;
    }

    public boolean fastwarding(){
        return  mSeekBarState != NORMAL_PLAY ;
    }

    public long stopFastForward(){
        mSeekBarState = NORMAL_PLAY ;
        long duration = mControlWrapper.getDuration();
        long newPosition = (duration * mSeekBar.getProgress()) / mSeekBar.getMax();
        return newPosition ;
    }

    public void setProgress(int duration, int position) {
        if(mSeekBarState != NORMAL_PLAY){
            return;
        }
        super.setProgress(duration, position);
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        super.onProgressChanged(seekBar, progress, fromUser);

        if(fromUser){
            if(lastProgress != 0){

                int addedProgress = progress - lastProgress ;

                if(mSeekBarState == FAST_FORWARD){
                    addedProgress = Math.abs(addedProgress) ;
                    addedProgress = (int) (addedProgress*0.6);
                    if(addedProgress < 1){
                        addedProgress = 1 ;
                    }
                }else if(mSeekBarState == REWIND){
                    addedProgress = -Math.abs(addedProgress) ;
                    addedProgress = (int) (addedProgress*0.6);
                    if(addedProgress > 0){
                        addedProgress = -1 ;
                    }
                }

                mSeekBar.setProgress((int) (progress + addedProgress));
            }
        }
        lastProgress = progress ;
    }
}
