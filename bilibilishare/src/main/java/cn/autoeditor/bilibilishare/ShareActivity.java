package cn.autoeditor.bilibilishare;

import static cn.autoeditor.sharelibrary.Utils.EMAIL_ERROR_SUCCESS;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import cn.autoeditor.sharelibrary.PartInfo;
import cn.autoeditor.sharelibrary.VideoDatabase;
import cn.autoeditor.sharelibrary.VideoInfo;
import cn.autoeditor.sharelibrary.Utils;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ShareActivity extends AppCompatActivity {
    private static final String TAG = ShareActivity.class.getName() ;

    private static final String BASE_URL = "https://www.bilibili.com/video/" ;
    private static final String REDIRECT_URL = "https://b23.tv/" ;

    private static final String KEY_TEXT = "share_text" ;


    public static void launchForClip(Activity from, String text){
        Intent intent = new Intent(from, ShareActivity.class) ;
        intent.putExtra(KEY_TEXT, text) ;
        from.startActivity(intent);
    }


    private TextView mTextViewTitile ;
    private TextView mTextViewUrl ;
    private CheckBox mCheckBoxSkip ;
    private CheckBox mCheckBoxSelectAll ;
    private RecyclerView mRecyclerView ;
    private View mBtnShare ;
    private View mViewExplain ;
    private String mTitle ;
    private String mUrl ;
    private String mBVID ;

    private ListAdapter mAdapter ;

    private List<VideoPartInfo> mPartInfoList ;

    private int mSelectedPartCount = 0 ;

    private VideoDatabase mVideoDatabase ;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        ActionBar bar =  getSupportActionBar() ;
        bar.setDisplayHomeAsUpEnabled(true);
        mVideoDatabase = VideoDatabase.getInstance(this) ;
        mTextViewTitile = findViewById(R.id.textView_title) ;
        mTextViewUrl = findViewById(R.id.textView_url) ;
        mRecyclerView = findViewById(R.id.recyclerView) ;
        mCheckBoxSkip = findViewById(R.id.checkBox) ;
        mCheckBoxSelectAll = findViewById(R.id.checkBox_select_all) ;
        mBtnShare = findViewById(R.id.textView_share) ;
        mViewExplain = findViewById(R.id.imageView_explain) ;
        mBtnShare.setOnClickListener(v -> share());
        mViewExplain.setOnClickListener(v -> {
            PopupWindow popupWindow = new PopupWindow(ShareActivity.this) ;
            TextView textView= new TextView(ShareActivity.this) ;
            textView.setTextColor(Color.WHITE);
            textView.setBackgroundColor(Color.BLACK);
            textView.setText(R.string.can_not_skip_explain);
            popupWindow.setContentView(textView);
            popupWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
            popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
            popupWindow.setFocusable(true);
            popupWindow.setOutsideTouchable(true);
            popupWindow.showAsDropDown(mViewExplain);
        });
        mCheckBoxSelectAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean changed = false ;
                if(isChecked && mSelectedPartCount != mPartInfoList.size() ){
                    changed = true ;
                }
                if(!isChecked && mSelectedPartCount == mPartInfoList.size()){
                    changed = true ;
                }
                if(changed){
                    mSelectedPartCount  = isChecked?mPartInfoList.size():0 ;
                    for(VideoPartInfo partInfo:mPartInfoList){
                        partInfo.checked = isChecked ;
                    }
                    mAdapter.notifyDataSetChanged();
                }
            }
        });
        mAdapter = new ListAdapter() ;
        RecyclerView.LayoutManager layoutManager  = new LinearLayoutManager(this,RecyclerView.VERTICAL,false);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mAdapter);
        Intent intent = getIntent();
        String action = intent.getAction();
        if(action == Intent.ACTION_SEND){
            disposeShare(intent);
        }else {
            String text = intent.getStringExtra(KEY_TEXT) ;
            disposeText(text) ;
        }

    }

    @SuppressLint("AutoDispose")
    private void share(){
        if(mSelectedPartCount == 0){
            Toast.makeText(this, R.string.no_select, Toast.LENGTH_SHORT).show();
            return;
        }
        ProgressDialog progressDialog = new ProgressDialog(this) ;
        progressDialog.setCancelable(false);
        Single.fromCallable(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        VideoInfo videoInfo = new VideoInfo() ;
                        videoInfo.action = VideoInfo.ACTION_ADD ;
                        videoInfo.bvid = mBVID ;
                        videoInfo.title = mTitle ;
                        videoInfo.skipable = !mCheckBoxSkip.isChecked() ;
                        videoInfo.partInfos = new ArrayList<>() ;
                        for(VideoPartInfo videoPartInfo:mPartInfoList){
                            if(videoPartInfo.checked){
                                PartInfo partInfo = new PartInfo() ;
                                partInfo.bvid = mBVID ;
                                partInfo.cid = videoPartInfo.cid ;
                                partInfo.title = videoPartInfo.titile ;
                                videoInfo.partInfos.add(partInfo) ;
                            }
                        }
                        Configs configs = new Configs(ShareActivity.this) ;

                        String sender = configs.getEmail() ;
                        String password = configs.getPassword() ;
                        String targetEmail = configs.getTargetEmail() ;
                        String smtpServer = configs.getSmtpServer() ;
                        String json = videoInfo.toJson() ;

                        int ret = Utils.sendEmail(sender, password,targetEmail, VideoInfo.SUBJECT,json, smtpServer) ;
                        if(ret == EMAIL_ERROR_SUCCESS){
                            mVideoDatabase.addShareInfo(videoInfo);
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
                                msg = getString(R.string.share_success) ;
                                break ;
                            default:
                                msg = getString(R.string.share_fail, ret) ;
                                break ;

                        }
                        new AlertDialog.Builder(ShareActivity.this)
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

    private void disposeShare(Intent intent){
        Uri uri = intent.getData() ;
        if(uri == null && intent.getExtras() != null) {
            Bundle extras = intent.getExtras();
            String text  = (String) extras.get(Intent.EXTRA_TEXT);
            disposeText(text);
        }
    }

    private boolean disposeText(String text){
        if(TextUtils.isEmpty(text)){
            return false;
        }
        if(!text.contains("http")){
            return false;
        }
        mTitle = text.substring(0, text.lastIndexOf("http")) ;
        String url = text.substring(text.lastIndexOf("http")) ;
        if(!isBilibliVideoUrl(url)){
            return false;
        }
        mUrl = url ;
        mTextViewTitile.setText(mTitle);
        mTextViewUrl.setText(mUrl);
        parseUrl(mUrl);
        return  true ;
    }


    private boolean isBilibliVideoUrl(String url){
        if(url.startsWith(REDIRECT_URL)){
            return true ;
        }
        if(url.startsWith(BASE_URL)){
            return true ;
        }
        return false ;
    }

    @SuppressLint("AutoDispose")
    private void parseUrl(String url)  {
        Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                String bvidUrl = url ;
                if(url.startsWith(REDIRECT_URL)){
                    Request request = new Request.Builder().url(url).build() ;
                    OkHttpClient okHttpClient = new OkHttpClient().newBuilder().followRedirects(false).build() ;
                    Response response = okHttpClient.newCall(request).execute() ;

                    if(response.isRedirect()) {
                        bvidUrl = response.header("location");
                    }
                }
                String bvid = "" ;
                if(bvidUrl.startsWith(BASE_URL)){
                    bvid = bvidUrl.substring(BASE_URL.length(), bvidUrl.indexOf("?")) ;
                }
                if(TextUtils.isEmpty(bvid)){
                    return false ;
                }
                mBVID = bvid ;
                parseVideo();
                return true ;
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Boolean s) {
                        if(s){
                            if(mPartInfoList.size() > 1){
                                mCheckBoxSelectAll.setVisibility(View.VISIBLE);
                            }
                            mSelectedPartCount = mPartInfoList.size() ;
                            mAdapter.notifyDataSetChanged();
                            mBtnShare.setEnabled(true);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                });

    }

    private void parseVideo() throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/web-interface/view?bvid=" +mBVID ;
        Request request = new Request.Builder().url(url).build() ;
        Response response = new OkHttpClient().newCall(request).execute() ;
        String json = response.body().string();
        JSONObject jsonObject = new JSONObject(json) ;
        JSONObject data = jsonObject.getJSONObject("data") ;

        JSONArray pages = data.getJSONArray("pages") ;
        mPartInfoList = new ArrayList<>() ;
        for(int i = 0 ;i < pages.length() ; ++i){
            JSONObject obj = pages.getJSONObject(i) ;
            VideoPartInfo partInfo = new VideoPartInfo() ;
            partInfo.titile = obj.optString("part") ;
            partInfo.cid = obj.optString("cid") ;
            mPartInfoList.add(partInfo) ;
        }

    }

    class ListAdapter extends  RecyclerView.Adapter<ViewHolder>{


        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(ShareActivity.this).inflate(R.layout.item_video, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            VideoPartInfo videoPartInfo = mPartInfoList.get(position) ;
            holder.mTextViewTitle.setText(videoPartInfo.titile);
            holder.mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(videoPartInfo.checked != isChecked) {
                        videoPartInfo.checked = isChecked;
                        mSelectedPartCount += (isChecked?1:-1) ;
                        if(mSelectedPartCount == mPartInfoList.size()){
                            mCheckBoxSelectAll.setChecked(true);
                        }else {
                            mCheckBoxSelectAll.setChecked(false);
                        }
                    }
                }
            });
            holder.mCheckBox.setChecked(videoPartInfo.checked);
        }

        @Override
        public int getItemCount() {
            return mPartInfoList == null?0: mPartInfoList.size() ;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private TextView mTextViewTitle ;
        private CheckBox mCheckBox ;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mTextViewTitle = itemView.findViewById(R.id.textView_title) ;
            mCheckBox = itemView.findViewById(R.id.checkBox_select) ;
        }
    }
    class VideoPartInfo {
        String titile ;
        String cid ;
        boolean checked = true ;
    }
}
