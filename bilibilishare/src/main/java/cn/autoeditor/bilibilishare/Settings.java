package cn.autoeditor.bilibilishare;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import cn.autoeditor.sharelibrary.Utils;

public class Settings extends AppCompatActivity {

    private Configs mConfigs ;
    private EditText mEditTextEmail ;
    private EditText mEditTextPasswd ;

    private EditText mEditTextServer ;
    private EditText mEditTextTargetEmail ;
    private View mBtnConfirm ;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.settings);
        mConfigs = new Configs(this) ;
        setContentView(R.layout.activity_settings);
        mEditTextEmail = findViewById(R.id.editText_email) ;
        mEditTextPasswd = findViewById(R.id.editText_password) ;
        mEditTextServer = findViewById(R.id.editText_server) ;
        mBtnConfirm = findViewById(R.id.textView_confirm) ;
        mEditTextTargetEmail = findViewById(R.id.textView_target_email) ;
        mEditTextEmail.setText(mConfigs.getEmail());
        mEditTextPasswd.setText(mConfigs.getPassword());
        mEditTextServer.setText(mConfigs.getSmtpServer());
        mEditTextTargetEmail.setText(mConfigs.getTargetEmail());
        hintDefaultServer() ;
        mEditTextEmail.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    return;
                }
                hintDefaultServer() ;
            }
        });

        mBtnConfirm.setOnClickListener(v -> {
            String email = mEditTextEmail.getText().toString() ;
            String password = mEditTextPasswd.getText().toString() ;
            String smtpServer = mEditTextServer.getText().toString() ;
            String targetEmail = mEditTextTargetEmail.getText().toString() ;
            mConfigs.set(email, password, smtpServer, targetEmail);
            new AlertDialog.Builder(Settings.this)
                    .setMessage(R.string.setting_success)
                    .setTitle(R.string.hint)
                    .setPositiveButton(R.string.confirm, null).show() ;
        });
    }
    private void hintDefaultServer(){
        String text = mEditTextEmail.getText().toString() ;
        if(Utils.isEmail(text)){
            mEditTextServer.setHint(Utils.emailDefaultSmtpServer(text)) ;
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                this.finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
