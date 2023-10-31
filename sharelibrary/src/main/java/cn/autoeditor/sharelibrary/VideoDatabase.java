package cn.autoeditor.sharelibrary;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class VideoDatabase {
    private static final String TAG = VideoDatabase.class.getName() ;

    private static final String DB_NAME = "video_database" ;
    private static final String TABLE_VIDEO_INFO = "video_info" ;
    private static final String TABLE_VIDEO_PART_INFO = "video_part_info" ;

    public static final String KEY_ID = "_id" ;
    public static final String KEY_BVID = "bvid" ;
    public static final String KEY_CID = "cid" ;
    public static final String KEY_CID_LIST = "cids" ;
    public static final String KEY_TITLE = "title" ;
    public static final String KEY_SKIP_ABLE = "skip_able" ;
    public static final String KEY_PLAY_TIMES = "play_times" ;
    public static final String KEY_TIMESTAMP = "timestamp" ;

    private static VideoDatabase sInstance ;

    public static VideoDatabase getInstance(Context context){
        if(sInstance == null){
            sInstance = new VideoDatabase(context) ;
        }
        return sInstance;
    }

    private MyDataBaseHelper mDataBaseHelper ;


    public VideoDatabase(Context context){
        mDataBaseHelper = new MyDataBaseHelper(context, DB_NAME) ;
    }


    /**
     * 同步一条视频信息
     * */
    public void addShareInfo(VideoInfo videoInfo){
        switch (videoInfo.action){
            case VideoInfo.ACTION_ADD :
                addVideoInfo(videoInfo);
                break ;
            case VideoInfo.ACTION_DEL :
                delVideoInfo(videoInfo);
                break ;
        }
    }

    private static String listToString(List<String> list){
        StringBuffer sb = new StringBuffer() ;
        for(String s :list){
            if(sb.length() != 0){
                sb.append(",") ;
            }
            sb.append(s) ;
        }
        return sb.toString() ;
    }
    private static String partCidList(List<PartInfo> list){
        StringBuffer sb = new StringBuffer() ;
        for(PartInfo partInfo:list){
            if(sb.length() != 0){
                sb.append(",") ;
            }
            sb.append(partInfo.cid) ;
        }
        return sb.toString() ;
    }
    private static boolean listContainsCid(List<PartInfo> list, String cid){

        for(PartInfo partInfo:list){
            if(partInfo.cid.equals(cid)){
                return true ;
            }
        }
        return false ;
    }
    private static List<String> stringToList(String s){
        return new ArrayList<>(Arrays.asList( s.split(",")) ) ;
    }
    private void addVideoInfo(VideoInfo videoInfo){
        Log.i(TAG, "addVideoInfo:"+videoInfo.bvid) ;

        addPartInfo(videoInfo.bvid, videoInfo.partInfos) ;

        Cursor cursor = mDataBaseHelper.getReadableDatabase().query(
                TABLE_VIDEO_INFO,
                new String[]{
                        KEY_CID_LIST
                },
                String.format("%s = ?",KEY_BVID),
                new String[]{videoInfo.bvid},
                null,
                null,
                null) ;

        ContentValues values = new ContentValues();
        values.put(KEY_BVID, videoInfo.bvid);
        values.put(KEY_TITLE, videoInfo.title);
        values.put(KEY_PLAY_TIMES, 0);
        values.put(KEY_SKIP_ABLE, videoInfo.skipable);
        if(cursor.moveToFirst()){
            String cids = cursor.getString(cursor.getColumnIndex(KEY_CID_LIST)) ;
            String[] cidArray = cids.split(",") ;
            for(String cid:cidArray){
                if(listContainsCid(videoInfo.partInfos, cid)){
                    continue;
                }
                videoInfo.partInfos.add(new PartInfo(cid)) ;
            }
            String cvidStr = partCidList(videoInfo.partInfos);
            values.put(KEY_CID_LIST, cvidStr);
            mDataBaseHelper.getWritableDatabase().update(TABLE_VIDEO_INFO, values,String.format("%s=? ",KEY_BVID), new String[]{videoInfo.bvid}) ;
        }else {
            String cvidStr = partCidList(videoInfo.partInfos);
            values.put(KEY_CID_LIST, cvidStr);
            values.put(KEY_TIMESTAMP, videoInfo.getTimestamp());
            mDataBaseHelper.getWritableDatabase().insert(TABLE_VIDEO_INFO, null, values);
        }

    }

    private void addPartInfo(String bvid, List<PartInfo> partInfos){
        for(PartInfo partInfo:partInfos){
            ContentValues values = new ContentValues();
            values.put(KEY_BVID, bvid);
            values.put(KEY_CID, partInfo.cid);
            values.put(KEY_TITLE, partInfo.title);
            values.put(KEY_TIMESTAMP, 0);
            long id  = mDataBaseHelper.getWritableDatabase().insert(TABLE_VIDEO_PART_INFO, null, values);
            Log.i(TAG, "addPartInfo bvid:"+bvid+" cid:"+partInfo.cid+" databaseid:"+id) ;
        }
    }
    private void delVideoInfo(VideoInfo videoInfo){
        delPartInfo(videoInfo.bvid, videoInfo.partInfos);
        Cursor cursor = mDataBaseHelper.getReadableDatabase().query(
                TABLE_VIDEO_INFO,
                new String[]{
                        KEY_CID_LIST
                },
                String.format("%s = ?", KEY_BVID),
                new String[]{videoInfo.bvid},
                null,
                null,
                null) ;
        if(cursor.moveToFirst()){
            String cids = cursor.getString(cursor.getColumnIndex(KEY_CID_LIST)) ;
            String[] cidArray = cids.split(",") ;
            List<String> cidList = new ArrayList<>(Arrays.asList(cidArray)) ;

            for(PartInfo partInfo: videoInfo.partInfos){
                cidList.remove(partInfo.cid) ;
            }
            if(cidList.size() > 0){
                String cvidStr = listToString(cidList) ;
                ContentValues values = new ContentValues() ;
                values.put(KEY_BVID, videoInfo.bvid);
                values.put(KEY_TITLE, videoInfo.title);
                values.put(KEY_CID_LIST, cvidStr);
                values.put(KEY_SKIP_ABLE, videoInfo.skipable);
                mDataBaseHelper.getWritableDatabase().update(TABLE_VIDEO_INFO,  values, String.format("%s = ?", KEY_BVID), new String[]{videoInfo.bvid});
            }else{
                mDataBaseHelper.getWritableDatabase().delete(TABLE_VIDEO_INFO, String.format("%s = ?", KEY_BVID), new String[]{videoInfo.bvid}) ;
            }
        }
    }

    private void delPartInfo(String bvid, List<PartInfo> partInfos){
        for(PartInfo partInfo:partInfos){
            mDataBaseHelper.getWritableDatabase().delete(TABLE_VIDEO_PART_INFO, String.format("%s = ? and %s = ?", KEY_BVID, KEY_CID), new String[]{bvid, partInfo.cid}) ;
        }
    }

    /**
     * 播放信息,用户播放此视频的位置
     * */
    public void playInfo(String bvid, String cid, long timestamp){
        ContentValues values = new ContentValues() ;
        values.put(KEY_TIMESTAMP, timestamp) ;
        mDataBaseHelper.getWritableDatabase().update(TABLE_VIDEO_PART_INFO, values, String.format("%s = ? and %s = ?", KEY_BVID, KEY_CID), new String[]{bvid, cid}) ;
    }

    public List<VideoInfo> getVideos(int startId, int count){
        Cursor cursor = mDataBaseHelper.getReadableDatabase().query(
                TABLE_VIDEO_INFO,
                new String[]{
                        KEY_ID,
                        KEY_BVID,
                        KEY_TITLE,
                        KEY_CID_LIST,
                        KEY_SKIP_ABLE,
                },
                String.format("%s > ?", KEY_ID),
                new String[]{String.valueOf(startId)},
                null,
                null,
                KEY_ID,
                String.valueOf(count)) ;
        List<VideoInfo> result = new ArrayList<>(count) ;
        while (cursor.moveToNext()){
            VideoInfo videoInfo = new VideoInfo() ;
            int id = cursor.getInt(cursor.getColumnIndex(KEY_ID)) ;
            videoInfo.setId(id);
            videoInfo. bvid = cursor.getString(cursor.getColumnIndex(KEY_BVID));
            videoInfo. title = cursor.getString(cursor.getColumnIndex(KEY_TITLE)) ;
            videoInfo. skipable = cursor.getInt(cursor.getColumnIndex(KEY_SKIP_ABLE))==1?true:false ;
            String cidStr = cursor.getString(cursor.getColumnIndex(KEY_CID_LIST)) ;
            videoInfo.partInfos = getPartInfos(videoInfo.bvid, cidStr) ;
            result.add(videoInfo) ;
        }
        return result ;
    }

    public VideoInfo getNext(int currentId){

        if(currentId >= 0) {
            mDataBaseHelper.getWritableDatabase().execSQL("UPDATE " + TABLE_VIDEO_INFO + " SET play_times = play_times + 1 WHERE _id= ?", new String[]{String.valueOf(currentId)});
        }

        Cursor cursor = mDataBaseHelper.getReadableDatabase().query(
                TABLE_VIDEO_INFO,
                new String[]{
                        KEY_ID,
                        KEY_BVID,
                        KEY_TITLE,
                        KEY_CID_LIST,
                        KEY_SKIP_ABLE,
                },
                null,
                null,
                null,
                null,
                KEY_PLAY_TIMES,
                String.valueOf(1)) ;

        VideoInfo videoInfo = null ;
        if(cursor.moveToFirst()){
            videoInfo = new VideoInfo() ;
            int id = cursor.getInt(cursor.getColumnIndex(KEY_ID)) ;
            videoInfo.setId(id);
            videoInfo. bvid = cursor.getString(cursor.getColumnIndex(KEY_BVID));
            videoInfo. title = cursor.getString(cursor.getColumnIndex(KEY_TITLE)) ;
            videoInfo. skipable = cursor.getInt(cursor.getColumnIndex(KEY_SKIP_ABLE))==1?true:false ;
            String cidStr = cursor.getString(cursor.getColumnIndex(KEY_CID_LIST)) ;
            videoInfo.partInfos = getPartInfos(videoInfo.bvid, cidStr) ;
        }
        return videoInfo ;
    }


    private List<PartInfo> getPartInfos(String bvid, String cidStr){
//        Cursor cursor = mDataBaseHelper.getReadableDatabase().query(
//                TABLE_VIDEO_PART_INFO,
//                new String[]{
//                        KEY_BVID,
//                        KEY_CID,
//                        KEY_TITLE,
//                        KEY_TIMESTAMP,
//                },
//                String.format("%s=? and %s in (?)", KEY_BVID, KEY_CID),
//                new String[]{bvid, cidStr},
//                null, null,null) ;

        String sql = String.format("select * from %s where %s='%s' and %s in (%s) ", TABLE_VIDEO_PART_INFO, KEY_BVID, bvid, KEY_CID, cidStr) ;
        Cursor cursor = mDataBaseHelper.getReadableDatabase().rawQuery(sql, null) ;
        List<PartInfo> result =  new ArrayList<>(cursor.getCount()) ;
        Log.i(TAG, "cursor count:"+cursor.getCount()) ;
        while (cursor.moveToNext()){
            PartInfo partInfo = new PartInfo() ;
            partInfo. bvid = cursor.getString(cursor.getColumnIndex(KEY_BVID));
            partInfo. cid = cursor.getString(cursor.getColumnIndex(KEY_CID));
            partInfo. title = cursor.getString(cursor.getColumnIndex(KEY_TITLE)) ;
            partInfo. timestamp = cursor.getInt(cursor.getColumnIndex(KEY_TIMESTAMP)) ;
            result.add(partInfo) ;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            result.sort(new Comparator<PartInfo>() {
                @Override
                public int compare(PartInfo o1, PartInfo o2) {
                    long cid1 = Long.parseLong(o1.cid) ;
                    long cid2 = Long.parseLong(o2.cid);
                    return Long.compare(cid1, cid2);
                }
            });
        }
        return result ;
    }

    class MyDataBaseHelper  extends SQLiteOpenHelper {

        private static final int DB_VERSION = 1 ;
        public static final String CREATE_TABLE_VIDEO_INFO = "create table video_info (" +
                "_id integer primary key autoincrement, " +
                "bvid text UNIQUE, " +
                "title text, " +
                "cids text, " +
                "skip_able integer, " +
                "play_times integer, " +
                "timestamp integer)" ;
        public static final String CREATE_TABLE_VIDEO_PART_INFO = "create table video_part_info (" +
                "_id integer primary key autoincrement, " +
                "bvid text, " +
                "cid text, " +
                "title text, " +
                "timestamp integer," +
                "UNIQUE (bvid, cid))" ;

        public MyDataBaseHelper(@Nullable Context context, @Nullable String name) {
            super(context, name, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_VIDEO_INFO);
            db.execSQL(CREATE_TABLE_VIDEO_PART_INFO);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}
