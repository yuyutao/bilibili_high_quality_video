package cn.autoeditor.bilibili;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.Set;

public class Configs {
    private static final String SETTINGS = "settings" ;
    private static final String KEY_EMAIL = "email" ;
    private static final String KEY_PASSWD = "password" ;
    private static final String KEY_POP3_SERVER= "pop3_server" ;
    private static final String KEY_APPROVE = "approve" ;
    private SharedPreferences mSharedPreferences ;

    public Configs(Context context){
        mSharedPreferences = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
    }
    public void set(String email, String password, String pop3Sever){
        mSharedPreferences.edit().putString(KEY_EMAIL, email).commit() ;
        mSharedPreferences.edit().putString(KEY_PASSWD, password).commit() ;
        if(!TextUtils.isEmpty(pop3Sever)){
            mSharedPreferences.edit().putString(KEY_POP3_SERVER, pop3Sever).commit() ;
        }
    }
    public void setApproves(Set<String> approves){
        mSharedPreferences.edit().putStringSet(KEY_APPROVE, approves).commit() ;
    }

    public Set<String> getApproves(){
        return mSharedPreferences.getStringSet(KEY_APPROVE, new HashSet<>()) ;
    }
    public String getEmail(){
        return mSharedPreferences.getString(KEY_EMAIL,"") ;
    }
    public String getPassword(){
        return mSharedPreferences.getString(KEY_PASSWD,"") ;
    }
    public String getPop3Server(){
        return mSharedPreferences.getString(KEY_POP3_SERVER, "") ;
    }

}
