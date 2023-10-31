package cn.autoeditor.bilibili;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.autoeditor.sharelibrary.Utils;

public class Settings extends AppCompatActivity {

    public static void launchActivity(Activity from){
        Intent intent = new Intent(from, Settings.class) ;
        from.startActivity(intent);
    }

    private EditText mEditTextEmail ;
    private EditText mEditTextPasswd ;

    private EditText mEditTextServer ;
    private RecyclerView mRecyclerViewApprove ;
    private ListAdapter mAdapter ;
    private View mBtnConfirm ;
    private View mBtnAddEmail ;
    private View mBtnMoreSettings ;
    private List<String> mApproveEmails ;
    private Configs mConfigs ;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(R.string.settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mConfigs = new Configs(this) ;
        setContentView(R.layout.activity_settings);
        mEditTextEmail = findViewById(R.id.editText_email) ;
        mEditTextPasswd = findViewById(R.id.editText_password) ;
        mEditTextServer = findViewById(R.id.editText_server) ;
        mBtnConfirm = findViewById(R.id.textView_confirm) ;
        mBtnAddEmail = findViewById(R.id.textView_add_email) ;
        mRecyclerViewApprove = findViewById(R.id.recyclerView) ;

        mEditTextEmail.setText(mConfigs.getEmail());
        mEditTextPasswd.setText(mConfigs.getPassword());
        mEditTextServer.setText(mConfigs.getPop3Server());
        hintDefaultServer();
        mEditTextEmail.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    return;
                }
                hintDefaultServer();
            }
        });

        mApproveEmails =  new ArrayList<>(mConfigs.getApproves()) ;

        mAdapter = new ListAdapter();
        RecyclerView.LayoutManager layoutManager  = new LinearLayoutManager(this,RecyclerView.VERTICAL,false);

        mRecyclerViewApprove.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mRecyclerViewApprove.setLayoutManager(layoutManager);
        mRecyclerViewApprove.setAdapter(mAdapter);
        mBtnAddEmail.setOnClickListener(v -> addEmailDialog());
        mBtnConfirm.setOnClickListener(v -> {
            String email = mEditTextEmail.getText().toString() ;
            String password = mEditTextPasswd.getText().toString() ;
            String pop3Server = mEditTextServer.getText().toString() ;
            mConfigs.set(email, password, pop3Server);
            new AlertDialog.Builder(Settings.this)
                    .setMessage(R.string.setting_success)
                    .setTitle(R.string.hint)
                    .setPositiveButton(R.string.confirm, null).show() ;

        });
    }

    private void hintDefaultServer(){
        String text = mEditTextEmail.getText().toString() ;
        if(Utils.isEmail(text)){
            mEditTextServer.setHint(Utils.emailDefaultPop3Server(text)) ;
        }
    }

    private void addEmailDialog(){
        EditText editText = new EditText(this) ;
        AlertDialog.Builder builder = new AlertDialog.Builder(this) ;
        builder.setView(editText)
                .setTitle(R.string.input_approve_email)
                .setNegativeButton(R.string.cancel,null)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String email = editText.getText().toString() ;
                    mApproveEmails.add(email) ;
                    mAdapter.notifyItemInserted(mApproveEmails.size()-1);
                    mConfigs.setApproves(new HashSet<>(mApproveEmails));
                }) ;
        builder.create().show();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    class ListAdapter extends  RecyclerView.Adapter<ViewHolder>{

        private void showDeleteEmailDialog(String email){
            AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this) ;
            builder.setMessage(getString(R.string.delete_approve_email, email))
                    .setTitle(R.string.delete_hint)
                    .setNegativeButton(R.string.cancel,null)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        int index = mApproveEmails.indexOf(email) ;
                        if(index >=0){
                            mApproveEmails.remove(index) ;
                            mAdapter.notifyItemRemoved(index);
                            mConfigs.setApproves(new HashSet<>(mApproveEmails));
                        }
                    }) ;
            builder.create().show();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            View view = LayoutInflater.from(Settings.this).inflate(R.layout.item_approve_email, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

            String email = mApproveEmails.get(position) ;
            holder.mTextViewEmail.setText(email);
            holder.mBtnDelete.setOnClickListener(v -> showDeleteEmailDialog(email));
        }

        @Override
        public int getItemCount() {
            return mApproveEmails.size();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private TextView mTextViewEmail ;
        private View mBtnDelete ;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mTextViewEmail = itemView.findViewById(R.id.textView_email) ;
            mBtnDelete = itemView.findViewById(R.id.textView_delete) ;
        }
    }

}
