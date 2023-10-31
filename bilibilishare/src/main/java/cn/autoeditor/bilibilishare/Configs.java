package cn.autoeditor.bilibilishare;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.Set;

public class Configs {
    private static final String SETTINGS = "settings" ;
    private static final String KEY_EMAIL = "email" ;
    private static final String KEY_PASSWD = "password" ;
    private static final String KEY_SMTP_SERVER= "smtp_server" ;
    private static final String KEY_SMTP_PORT = "smtp_port" ;
    private static final String KEY_TARGET_EMAIL = "target_email" ;
    private SharedPreferences mSharedPreferences ;

    public Configs(Context context){
        mSharedPreferences = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
    }
    public void set(String email, String password, String smtpSever,  String targetEmail){
        mSharedPreferences.edit().putString(KEY_EMAIL, email).commit() ;
        mSharedPreferences.edit().putString(KEY_PASSWD, password).commit() ;
        if(!TextUtils.isEmpty(smtpSever)){
            mSharedPreferences.edit().putString(KEY_SMTP_SERVER, smtpSever).commit() ;
        }

        mSharedPreferences.edit().putString(KEY_TARGET_EMAIL, targetEmail).commit() ;
    }


    public String getEmail(){
        return mSharedPreferences.getString(KEY_EMAIL,"") ;
    }
    public String getPassword(){
        return mSharedPreferences.getString(KEY_PASSWD,"") ;
    }
    public String getSmtpServer(){
        return mSharedPreferences.getString(KEY_SMTP_SERVER, "") ;
    }

    public String getTargetEmail(){
        return mSharedPreferences.getString(KEY_TARGET_EMAIL,"") ;
    }


}
