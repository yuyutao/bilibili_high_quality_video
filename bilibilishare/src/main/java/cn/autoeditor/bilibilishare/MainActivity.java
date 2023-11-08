package cn.autoeditor.bilibilishare;

import static cn.autoeditor.sharelibrary.Utils.EMAIL_ERROR_SUCCESS;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import cn.autoeditor.sharelibrary.PartInfo;
import cn.autoeditor.sharelibrary.Utils;
import cn.autoeditor.sharelibrary.VideoDatabase;
import cn.autoeditor.sharelibrary.VideoInfo;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final int MAX_COUNT = 20 ;

    private static final String BILIBILI_SHARE_MATCH = "^【.+】.*https://b23\\.tv/.+" ;
    private ExpandableListView mListView ;
    private ListAdapter mAdapter ;

    private List<VideoInfo> mVideoInfoList ;

    private View mViewClipShareInfo ;
    private TextView mTextViewClicpShare ;
    private int mPageId = -1 ;
    private String mSearchText ;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView = findViewById(R.id.listview_share);
        mViewClipShareInfo = findViewById(R.id.view_share_info);
        mTextViewClicpShare = findViewById(R.id.textView_clip_hint);

        mAdapter =  new ListAdapter() ;
        mListView.setAdapter(mAdapter);
        mListView.setDivider(null);

        update();

        EditText editTextSearch = findViewById(R.id.editText_search) ;
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                    mSearchText = s.toString() ;
                    mPageId = -1 ;
                    update();

            }
        });
    }
    private void checkClip(){
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clipData = clipboardManager.getPrimaryClip() ;
        if(clipData != null && clipData.getItemCount() > 0){
            ClipData.Item item = clipData.getItemAt(0) ;
            String s = item.getText().toString() ;
            if(s.matches(BILIBILI_SHARE_MATCH)) {
                showBilibliClip(item.getText().toString());
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus){
            checkClip();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        update();
    }
    private void update(){
        mVideoInfoList = VideoDatabase.getInstance(this).getVideos(mSearchText, mPageId, MAX_COUNT) ;
        mAdapter.notifyDataSetChanged();
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                this.finish(); // back button
                break;

        }
        if(item.getItemId() == R.id.action_set){
            Intent intent = new Intent(this, Settings.class) ;
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_set, menu);
        return super.onCreateOptionsMenu(menu);
    }
    private void showBilibliClip(String text){
        mViewClipShareInfo.setVisibility(View.VISIBLE);
        mTextViewClicpShare.setText(text);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mViewClipShareInfo.getVisibility() == View.GONE){
                    return;
                }
                Animation animation = AnimationUtils.loadAnimation(MainActivity.this,R.anim.zoomoutleft) ;
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mViewClipShareInfo.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                mViewClipShareInfo.startAnimation(animation);
            }
        },3000) ;

        View btnShare = findViewById(R.id.textView_share) ;
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewClipShareInfo.setVisibility(View.GONE);
                ShareActivity.launchForClip(MainActivity.this, text);
            }
        });
    }

    private void showDeleteDialog(VideoInfo videoInfo){

        String msg = getString(R.string.delete_video_msg, videoInfo.title) ;
        if(videoInfo.partInfos.size() > 1){
            msg += getString(R.string.delete_multi_part_msg) ;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.hint)
                .setMessage(msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        videoInfo.action = VideoInfo.ACTION_DEL ;
                        sendVideoInfo(videoInfo);
                    }
                }).show() ;
    }
    private void showDeleteDialog(PartInfo partInfo){
        String msg = getString(R.string.delete_video_msg, partInfo.title) ;
        new AlertDialog.Builder(this)
                .setTitle(R.string.hint)
                .setMessage(msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        VideoInfo videoInfo = new VideoInfo();
                        videoInfo.bvid = partInfo.bvid ;
                        videoInfo.partInfos = new ArrayList<>( );
                        videoInfo.action = VideoInfo.ACTION_DEL ;
                        videoInfo.partInfos.add(partInfo) ;
                        sendVideoInfo(videoInfo);
                    }
                }).show() ;
    }

    private void showEditDialog(VideoInfo videoInfo){
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit,null) ;
        TextView textViewTitle = view.findViewById(R.id.textView_title) ;
        CheckBox checkBoxSkipable = view.findViewById(R.id.checkBox_skipable) ;
        CheckBox checkBoxNextPlay = view.findViewById(R.id.checkBox_nextplay) ;
        textViewTitle.setText(videoInfo.title);
        checkBoxSkipable.setChecked(!videoInfo.skipable);
        new AlertDialog.Builder(this)
                .setTitle(R.string.edit_video_title)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        videoInfo.action = VideoInfo.ACTION_EDIT ;
                        videoInfo.skipable = !checkBoxSkipable.isChecked() ;
                        videoInfo.nextplay = checkBoxNextPlay.isChecked() ;
                        sendVideoInfo(videoInfo) ;
                    }
                }).show() ;
    }

    @SuppressLint("AutoDispose")
    private void sendVideoInfo(VideoInfo videoInfo){
        ProgressDialog progressDialog = new ProgressDialog(this) ;
        progressDialog.setMessage(getString(R.string.sending_message));
        progressDialog.setCancelable(false);
        Single.fromCallable(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        Configs configs = new Configs(MainActivity.this) ;

                        String sender = configs.getEmail() ;
                        String password = configs.getPassword() ;
                        String targetEmail = configs.getTargetEmail() ;
                        String smtpServer = configs.getSmtpServer() ;
                        String json = videoInfo.toJson() ;

                        int ret = Utils.sendEmail(sender, password,targetEmail, VideoInfo.SUBJECT,json, smtpServer) ;
                        if(ret == EMAIL_ERROR_SUCCESS){
                            VideoDatabase.getInstance(MainActivity.this).addShareInfo(videoInfo);
                        }
                        return ret ;
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        progressDialog.show();
                    }

                    @Override
                    public void onSuccess(Integer ret) {
                        String msg ;
                        switch (ret){
                            case EMAIL_ERROR_SUCCESS :
                                msg = getString(R.string.send_success) ;
                                break ;
                            default:
                                msg = getString(R.string.send_fail, ret) ;
                                break ;

                        }
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.hint)
                                .setMessage(msg)
                                .setPositiveButton(R.string.confirm, null).show() ;
                        progressDialog.dismiss();
                    }

                    @Override
                    public void onError(Throwable e) {
                        progressDialog.dismiss();
                    }

                });

    }
    class ListAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return mVideoInfoList == null?0 :mVideoInfoList.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            List<PartInfo> partInfos = mVideoInfoList.get(groupPosition).partInfos ;

            return partInfos==null|| partInfos.size()<=1?0:partInfos.size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return mVideoInfoList.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return mVideoInfoList.get(groupPosition).partInfos.get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            ViewHolder viewHolder ;
            if(convertView == null){
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_video_info, parent, false) ;
                viewHolder = new ViewHolder();
                viewHolder.mTextView = convertView.findViewById(R.id.textView_title) ;
                viewHolder.mViewDelete = convertView.findViewById(R.id.imageView_remove) ;
                viewHolder.mViewEdit = convertView.findViewById(R.id.imageView_edit) ;
                convertView.setTag(viewHolder);
            }else{
                viewHolder = (ViewHolder) convertView.getTag();
            }
            VideoInfo videoInfo = mVideoInfoList.get(groupPosition) ;
            viewHolder.mTextView.setText(videoInfo.title);
            viewHolder.mViewDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDeleteDialog(videoInfo);
                }
            });

            viewHolder.mViewEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showEditDialog(videoInfo);
                }
            });
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            ViewHolder viewHolder ;
            if(convertView == null){
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_video_info, parent, false) ;
                viewHolder = new ViewHolder();
                viewHolder.mTextView = convertView.findViewById(R.id.textView_title) ;
                viewHolder.mViewDelete = convertView.findViewById(R.id.imageView_remove) ;
                convertView.findViewById(R.id.imageView_edit).setVisibility(View.GONE); ;
                convertView.setTag(viewHolder);
            }else{
                viewHolder = (ViewHolder) convertView.getTag();
            }

            VideoInfo videoInfo = mVideoInfoList.get(groupPosition) ;
            PartInfo partInfo = videoInfo.partInfos.get(childPosition) ;
            viewHolder.mTextView.setText(partInfo.title);
            viewHolder.mViewDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDeleteDialog(partInfo);
                }
            });
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }
    }
    class ViewHolder {
        TextView mTextView ;
        ImageView mViewDelete ;
        ImageView mViewEdit ;
    }
}
