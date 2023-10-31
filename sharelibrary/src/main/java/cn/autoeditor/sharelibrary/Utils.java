package cn.autoeditor.sharelibrary;

import android.text.TextUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import javax.mail.AuthenticationFailedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Utils {
    public static final int EMAIL_ERROR_SUCCESS = 0 ;
    public static final int EMAIL_ERROR_ARGS = -1 ;
    public static final int EMAIL_ERROR_FORMAT = -2 ;
    public static final int EMAIL_ERROR_AUTH_FAIL = -3 ;
    public static final int EMAIL_ERROR_UNKOWN = -4 ;
    public static boolean isEmail(String text){
        if(TextUtils.isEmpty(text)){
            return false ;
        }
        String check = "^([a-z0-9A-Z]+[-|_|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
        return text.matches(check) ;
    }

    public static String parseHostIPV4(String host){
        try {
            InetAddress[] inetAddressArr = InetAddress.getAllByName(host);
            if (inetAddressArr != null && inetAddressArr.length > 0) {
                for (int i = 0; i < inetAddressArr.length; i++) {
                    if(inetAddressArr[i] instanceof Inet4Address){
                        return inetAddressArr[i].getHostAddress() ;
                    }
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
    public static String emailDefaultPop3Server(String email){
        return "pop3." + email.substring(email.indexOf("@") + 1);
    }
    public static String emailDefaultSmtpServer(String email){
        return "smtp." + email.substring(email.indexOf("@") + 1);
    }
    public static int sendEmail(String sender, String passwd, String reciver, String subject, String content, String smtpServer) throws MessagingException {
        int ret = EMAIL_ERROR_SUCCESS ;
        if(Utils.isEmail(sender) && Utils.isEmail(reciver)) {
            if(TextUtils.isEmpty(smtpServer)){
                smtpServer = "smtp." + sender.substring(sender.indexOf("@") + 1);
            }
            Properties properties = new Properties();
            String ipAddressArr = Utils.parseHostIPV4(smtpServer) ;
            //默认使用ipv4地址发送
            if(ipAddressArr != null){
                properties.put("mail.smtp.host", ipAddressArr);// 主机名
            }else{
                properties.put("mail.smtp.host", smtpServer);// 主机名
            }
            properties.put("mail.transport.protocol", "smtp");// 连接协议
            properties.put("mail.smtp.port", 465);// 端口号
            //properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.ssl.enable", "true");//设置是否使用ssl安全连接 ---一般都使用
//        properties.put("mail.debug", "true");//设置是否显示debug信息 true 会在控制台显示相关信息
            //得到回话对象
            Session session = Session.getInstance(properties);
            // 获取邮件对象
            Message message = new MimeMessage(session);
            //设置发件人邮箱地址
            try {
                message.setFrom(new InternetAddress(sender));
                //设置收件人地址
                message.setRecipients(MimeMessage.RecipientType.TO, new InternetAddress[]{new InternetAddress(reciver)});
                //设置邮件标题
                message.setSubject(subject);
                //设置邮件内容
                message.setText(content);
                //得到邮差对象
                Transport transport = session.getTransport();
                //连接自己的邮箱账户
                transport.connect(sender, passwd);//密码为刚才得到的授权码
                transport.sendMessage(message, message.getAllRecipients());
            }catch (AuthenticationFailedException e){
                e.printStackTrace();
                ret = EMAIL_ERROR_AUTH_FAIL ;
            }
        }else{
            ret = EMAIL_ERROR_FORMAT ;
        }
        return ret ;
    }
}
