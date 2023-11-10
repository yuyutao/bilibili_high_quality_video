package cn.autoeditor.bilibili;

import androidx.annotation.NonNull;

import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.autoeditor.sharelibrary.LtLog;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BibiliVideoInfo {

    public static final int SCAN_CODE_SUCCESS = 0 ;
    public static final int SCAN_CODE_EXPIRE = 86038 ;
    public static final int SCAN_CODE_SCANED = 86090 ;
    public static final int SCAN_CODE_UNSCANED = 86101 ;

    public static List<Long> getVideoCIDList(String bvid) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/web-interface/view?bvid=" +bvid ;
        Request request = new Request.Builder().url(url).build() ;
        Response response = new OkHttpClient().newCall(request).execute() ;
        String json = response.body().string();
        JSONObject jsonObject = new JSONObject(json) ;
        JSONObject data = jsonObject.getJSONObject("data") ;
        List<Long> result = new ArrayList<>() ;
        JSONArray pages = data.getJSONArray("pages") ;
        for(int i = 0 ;i < pages.length() ; ++i){
            JSONObject obj = pages.getJSONObject(i) ;
            result.add(obj.optLong("cid")) ;
        }
        return result ;
    }

    public static String getVideoUrl(String bvid, String cid) throws IOException, JSONException {
        String url =  String.format("https://api.bilibili.com/x/player/playurl?cid=%s&bvid=%s&qn=80", cid, bvid) ;
        Request request = new Request.Builder().url(url)
                .build() ;
        Response response = new OkHttpClient.Builder()
                .cookieJar(new PersistentCookieJar(new SetCookieCache(), App.getCookieJar())).build()
                .newCall(request).execute() ;
        String json = response.body().string();
        JSONObject jsonObject = new JSONObject(json) ;
        JSONObject data = jsonObject.getJSONObject("data") ;
        JSONArray durlArray = data.getJSONArray("durl") ;
        if(durlArray.length() >=1){
            return durlArray.getJSONObject(0).getString("url") ;
        }
        return null ;
    }

    public static String checkLogin() throws IOException {
        String url = "https://passport.bilibili.com/x/passport-login/web/cookie/info" ;
        Request request = new Request.Builder().url(url)
                .build() ;
        Response response = new OkHttpClient.Builder()
                .cookieJar(new PersistentCookieJar(new SetCookieCache(), App.getCookieJar())).build()
                .newCall(request).execute() ;
        return response.body().string();
    }
    public static String requestQR() throws IOException {
        String url = "https://passport.bilibili.com/x/passport-login/web/qrcode/generate" ;
        Request request = new Request.Builder().url(url)
                .build() ;
        Response response = new OkHttpClient.Builder().build()
                .newCall(request).execute() ;
        return response.body().string();
    }
    public static String pollScanInfo(String qrcode_key) throws IOException {
        String url = String.format("https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=%s", qrcode_key) ;
        LtLog.i("url:"+url) ;
        Request request = new Request.Builder().url(url)
                .build() ;
        Response response = new OkHttpClient.Builder()
                .cookieJar(new PersistentCookieJar(new SetCookieCache(), App.getCookieJar())).build()
                .newCall(request).execute() ;
        return response.body().string();
    }

}
