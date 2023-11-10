package cn.autoeditor.bilibili;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Callable;

import cn.autoeditor.sharelibrary.LtLog;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class LoginActivity extends AppCompatActivity {
    private TextView mTextViewScanInfo ;
    private ImageView imageViewQR ;
    private String mQRCode;
    private long mQRTimestamp ;
    private Handler mHandler ;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_qrlogin);
        mTextViewScanInfo = findViewById(R.id.textView_logininfo) ;
        imageViewQR = findViewById(R.id.imageView_qr) ;
        mHandler = new Handler() ;
        requestQRUrl();
    }

    @SuppressLint("AutoDispose")
    private void requestQRUrl(){
        Single.fromCallable(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        return BibiliVideoInfo.requestQR() ;
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<String>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull String s) {
                        JSONObject object = null;
                        try {
                            object = new JSONObject(s);
                            JSONObject dataObj = object.getJSONObject("data") ;
                            String url = dataObj.getString("url") ;
                            mQRCode = dataObj.getString("qrcode_key") ;
                            mQRTimestamp = System.currentTimeMillis() ;
                            imageViewQR.setImageBitmap(encodeBarcode(url, dip2px(200),dip2px(200))) ;
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    requestQrToken();
                                }
                            },3000) ;
                        }catch (JSONException e){

                        }

                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                    }
                });
    }

    public  int dip2px(  float dipValue)
    {
        float m=getResources().getDisplayMetrics().density ;

        return (int)(dipValue * m + 0.5f) ;

    }

    @SuppressLint("AutoDispose")
    private void requestQrToken(){
        Single.fromCallable(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        return BibiliVideoInfo.pollScanInfo(mQRCode) ;
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<String>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull String s) {
                        LtLog.i("pollscaninfo :"+s) ;
                        JSONObject object = null;
                        try {
                            object = new JSONObject(s);
                            JSONObject dataObj = object.getJSONObject("data") ;
                            int code = dataObj.getInt("code") ;
                            String message = dataObj.getString("message") ;
                            mTextViewScanInfo.setText(message);
                            switch (code){
                                case BibiliVideoInfo.SCAN_CODE_SCANED :
                                    break ;
                                case BibiliVideoInfo.SCAN_CODE_EXPIRE:
                                    break ;
                                case BibiliVideoInfo.SCAN_CODE_SUCCESS :
                                    BilibiliPlayer.launch(LoginActivity.this) ;
                                    finish();
                                    return;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                requestQrToken();
                            }
                        },2000);

                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                    }
                });
    }

    private static BitMatrix updateBit(BitMatrix matrix, int margin){

        int tempM = margin*2;

        int[] rec = matrix.getEnclosingRectangle();   //获取二维码图案的属性

        int resWidth = rec[2] + tempM;

        int resHeight = rec[3] + tempM;

        BitMatrix resMatrix = new BitMatrix(resWidth, resHeight); // 按照自定义边框生成新的BitMatrix

        resMatrix.clear();

        for(int i= margin; i < resWidth- margin; i++){   //循环，将二维码图案绘制到新的bitMatrix中

            for(int j=margin; j < resHeight-margin; j++){

                if(matrix.get(i-margin + rec[0], j-margin + rec[1])){
                    resMatrix.set(i,j);
                }

            }

        }

        return resMatrix;

    }
    public static Bitmap encodeBarcode(CharSequence data, int w, int h) {
        MultiFormatWriter writer = new MultiFormatWriter();
        Bitmap bitmap = null ;
        try {
            BitMatrix matrix = writer.encode(data.toString(), BarcodeFormat.QR_CODE,w,h) ;
            matrix = updateBit(matrix,25) ;
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if(matrix.get(x, y)){
                        pixels[y * width + x] = 0xff000000;
                    }else{
                        pixels[y * width + x] = 0xffffffff;
                    }
                }
            }
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            //通过像素数组生成bitmap,具体参考api
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return bitmap ;
    }
}
