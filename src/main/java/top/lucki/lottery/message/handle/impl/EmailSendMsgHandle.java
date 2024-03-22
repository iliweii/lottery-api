package top.lucki.lottery.message.handle.impl;


import cn.hutool.core.util.StrUtil;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import top.lucki.lottery.common.config.StaticConfig;
import top.lucki.lottery.common.utils.SpringContextUtils;
import top.lucki.lottery.message.handle.ISendMsgHandle;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

public class EmailSendMsgHandle implements ISendMsgHandle {

    static String emailFrom;


    public static void setEmailFrom(String emailFrom) {
        EmailSendMsgHandle.emailFrom = emailFrom;
    }

    @Async
    @Override
    public void SendMsg(String es_receiver, String es_title, String es_content) {
        JavaMailSender mailSender = (JavaMailSender) SpringContextUtils.getBean("mailSender");
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = null;
        if (StrUtil.isEmpty(emailFrom)) {
            StaticConfig staticConfig = SpringContextUtils.getBean(StaticConfig.class);
            setEmailFrom(staticConfig.getEmailFrom());
        }
        try {
            helper = new MimeMessageHelper(message, true);
            // 设置发送方邮箱地址
            helper.setFrom(emailFrom);
            helper.setTo(es_receiver);
            helper.setSubject(es_title);
            helper.setText(es_content, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }

    }
}
