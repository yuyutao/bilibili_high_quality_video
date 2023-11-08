package cn.autoeditor.sharelibrary;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;

import java.net.PortUnreachableException;
import java.util.List;

public class VideoInfo {
    public static final String SUBJECT = "B站优质视频分享" ;
    public static final int ACTION_ADD = 1 ;
    public static final int ACTION_DEL = 2 ;

    public static final int ACTION_EDIT = 3 ;

    private int id ;

    @Expose(serialize = false)
    private long timestamp ;

    public int getId(){
        return id ;
    }
    public void setId(int id){
        this.id = id ;
    }

    public void setTimestamp(long timestamp){
        this.timestamp = timestamp ;
    }
    public long getTimestamp(){
        return timestamp ;
    }
    public int action ;
    public String bvid ;

    public String title ;

    public boolean skipable = true ;
    public boolean nextplay = false ;
    public List<PartInfo> partInfos;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoInfo videoInfo = (VideoInfo) o;
        return bvid.equals(videoInfo.bvid) ;
    }


    public String toJson(){
        return new Gson().toJson(this) ;
    }
    public static VideoInfo fromJson(String json){
        try {
            return new Gson().fromJson(json, VideoInfo.class);
        }catch (JsonSyntaxException e){
            e.printStackTrace();
        }
        return null ;
    }
}
