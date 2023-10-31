package cn.autoeditor.bilibili;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import cn.autoeditor.sharelibrary.VideoDatabase;
import cn.autoeditor.sharelibrary.VideoInfo;

public class EmailSyncer {
    private static final String TAG = EmailSyncer.class.getName() ;
    private static final String EMAIL_SYNCER = "email_syncer" ;
    private static final String KEY_LAST_TIMESTAMP = "last_timestamp" ;
    private Timer mTimer ;
    private Configs mConfigs ;
    private VideoDatabase mVideoDatabase ;

    private SharedPreferences mSharedPreferences ;

    private long mLastTimestamp ;

    private OnNewVideoInfoCallback mCallback ;


    public EmailSyncer(Context context){
        mVideoDatabase = VideoDatabase.getInstance(context) ;
        mConfigs = new Configs(context) ;
        mSharedPreferences = context.getSharedPreferences(EMAIL_SYNCER, Context.MODE_PRIVATE) ;
        mLastTimestamp = mSharedPreferences.getLong(KEY_LAST_TIMESTAMP, 1693664585000L) ; //以软件的开发时间开始
    }

    public void setOnNewVideoInfoCallback(OnNewVideoInfoCallback callback){
        mCallback = callback ;
    }

    private void receiveEmail() throws MessagingException {
        String user = mConfigs.getEmail() ;
        String pwd = mConfigs.getPassword() ;

        Set<String> approve = mConfigs.getApproves() ;
        String pop3Server = mConfigs.getPop3Server() ;
        if(TextUtils.isEmpty(pop3Server)){
            pop3Server = "pop3." + user.substring(user.indexOf("@") + 1);
        }
        String protocol = "pop3";
// 创建一个有具体连接信息的Properties对象
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", protocol);
        props.setProperty("mail.pop3.host", pop3Server);

        Session session = Session.getInstance(props);
        session.setDebug(false);
// 利用Session对象获得Store对象，并连接pop3服务器
        Store store = session.getStore();
        store.connect(pop3Server, user, pwd);
// 获得邮箱内的邮件夹Folder对象，以"读-写"打开
        Folder folder = store.getFolder("inbox");
        folder.open(Folder.READ_ONLY);
        Message[] messages = folder.getMessages();
        long newTime = mLastTimestamp ;
        Stack<VideoInfo> shareList = new Stack<>() ;
        for(int i = messages.length -1 ; i >=0 ; --i){
            Message message = messages[i] ;
            Date sentDate  = null ;
            try {
                sentDate = message.getSentDate();
            }catch (Exception e){
                e.printStackTrace();
            }
            if(sentDate == null){
                continue;
            }
            long timestamp = sentDate.getTime() ;
            Log.i(TAG, "Message timestamp:"+timestamp+" lastTimestamp:"+mLastTimestamp) ;
            if(mLastTimestamp >= timestamp){
                break ;
            }
            if(newTime < timestamp){
                newTime = timestamp ;
            }
            try {
                String sender = getForm(message) ;

                Log.i(TAG, "Message sender:"+sender) ;
                if(!approve.contains(sender)){
                    continue;
                }
            } catch (UnsupportedEncodingException | AddressException e) {
                continue;
            }


            StringBuffer contentSB = new StringBuffer() ;
            try {
                getMailTextContent(message, contentSB);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String content = contentSB.toString() ;
            Log.i(TAG, "Message content:"+content) ;

            VideoInfo videoInfo = VideoInfo.fromJson(content) ;
            if(videoInfo == null){
                continue;
            }
            if(!TextUtils.isEmpty(videoInfo.bvid) && videoInfo.partInfos != null){
                shareList.push(videoInfo) ; //先将所有邮件放入栈中,邮件是从最新开始读取，但是需要按发送时间处理邮件
            }
        }
        folder.close(false);
        store.close();

        disposeShareList(shareList);
        mLastTimestamp = newTime;
        mSharedPreferences.edit().putLong(KEY_LAST_TIMESTAMP, newTime).commit() ;

        Log.i(TAG, "Message newTime:"+newTime+" lastTimestamp:"+mLastTimestamp) ;
    }

    private void disposeShareList(Stack<VideoInfo> shareList){
        while (!shareList.empty()){
            VideoInfo videoInfo = shareList.pop() ;
            mVideoDatabase.addShareInfo(videoInfo);
            if(mCallback != null){
                mCallback.onNewVideo(videoInfo);
            }
        }
    }

    public static void getMailTextContent(Part part, StringBuffer content) throws MessagingException, IOException {
        //如果是文本类型的附件，通过getContent方法可以取到文本内容，但这不是我们需要的结果，所以在这里要做判断
        boolean isContainTextAttach = part.getContentType().indexOf("name") > 0;
        if (part.isMimeType("text/*") && !isContainTextAttach) {
            content.append(part.getContent().toString());
        } else if (part.isMimeType("message/rfc822")) {
            getMailTextContent((Part)part.getContent(),content);
        } else if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            int partCount = multipart.getCount();
            for (int i = 0; i < partCount; i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                getMailTextContent(bodyPart,content);
            }
        }
    }
    private static String getForm(Message message) throws UnsupportedEncodingException, MessagingException {
        Address[] froms = message.getFrom();
        if (froms.length < 1)
            throw new MessagingException("没有发件人!");

        InternetAddress address = (InternetAddress) froms[0];

        return address.getAddress() ;
    }

    public void start(){
        mTimer = new Timer() ;
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    receiveEmail();
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        },0, 60*1000);
    }
    public void stop(){
        if(mTimer == null){
            return;
        }
        mTimer.cancel();
        mTimer.purge() ;
    }

    public interface OnNewVideoInfoCallback{
        void onNewVideo(VideoInfo info) ;
    }

}
