package cn.autoeditor.bilibili;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BibiliVideoInfo {

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
        String url =  String.format("https://api.bilibili.com/x/player/playurl?cid=%s&bvid=%s", cid, bvid) ;
        Request request = new Request.Builder().url(url).build() ;
        Response response = new OkHttpClient().newCall(request).execute() ;
        String json = response.body().string();
        JSONObject jsonObject = new JSONObject(json) ;
        JSONObject data = jsonObject.getJSONObject("data") ;
        JSONArray durlArray = data.getJSONArray("durl") ;
        if(durlArray.length() >=1){
            return durlArray.getJSONObject(0).getString("url") ;
        }
        return null ;
    }

}
