package cn.autoeditor.sharelibrary;

import android.util.Log;

/**
 * Created by litao on 14-12-30.
 */
public class LtLog {
    public static final String TAG = "LT-Log";

    public static void i(String info) {
        i(TAG, info);
    }

    public static void d(String info) {
        d(TAG, info);
    }

    public static void e(String info) {
        d(TAG, info);
    }

    public static void w(String info) {
        d(TAG, info);
    }

    public static void i(String tag, String info) {
        Log.i(tag,"lt-->" + info);
    }

    public static void e(String tag, String info) {
        Log.e(tag,"lt-->" + info);
    }

    public static void d(String tag, String info) {
        Log.d(tag, "lt-->" + info);
    }

    public static void w(String tag, String info) {
        Log.w(tag, "lt-->" + info);
    }

    public static void setLogprefix(String s) {
    }
}
