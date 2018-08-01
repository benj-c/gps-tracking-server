/*
 * Copyright 2017 Benjamin.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.server.workers;

import com.sun.mail.smtp.SMTPMessage;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.server.dto.properties.EmailProperties;
import org.server.dto.Message;
import org.server.util.MailMessageUtil;
import org.server.util.TimezoneUtil;

public class MessageQueueProcessor implements Runnable {

    private static final Logger ERROR_LOGGER = LogManager.getLogger("ErrorLog");
    private static final Logger DEBUG_LOGGER = LogManager.getLogger("DebugLog");

    private final LinkedBlockingQueue<Message> mq;
    private final EmailProperties emailProperties;
    private static long exceptionCount = 0;

    /**
     *
     * @param mq
     * @param emailProperties
     */
    public MessageQueueProcessor(
            LinkedBlockingQueue<Message> mq,
            EmailProperties emailProperties
    ) {
        this.mq = mq;
        this.emailProperties = emailProperties;
    }

    /**
     *
     * @return
     */
    private LinkedBlockingQueue<Message> getMessageQueue() {
        return mq;
    }

    /**
     *
     * @return
     */
    private EmailProperties getEmailProperties() {
        return emailProperties;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Message msg = getMessageQueue().take();
                if (null != msg) {
                    SMTPMessage message = new SMTPMessage(MailMessageUtil.createMailSession(
                            getEmailProperties().getSender_id(),
                            getEmailProperties().getSender_passwd(),
                            getEmailProperties().getEmailProperties()
                    ));
                    message.setFrom(new InternetAddress(getEmailProperties().getSender_id()));
                    if (getEmailProperties().isDevsEnable()) {
                        message.setRecipients(MimeMessage.RecipientType.TO, getEmailProperties().getDevsAddresses());
                    }
                    if (getEmailProperties().isProductionEnable()) {
                        message.setRecipients(MimeMessage.RecipientType.CC, getEmailProperties().getProduction());
                    }
                    sendEmail(msg, message, getEmailProperties());
                }
            }
        } catch (AddressException ex) {
            ERROR_LOGGER.error(getLogMetaInfo(), ex);
        } catch (MessagingException | InterruptedException ex) {
            ERROR_LOGGER.error(getLogMetaInfo(), ex);
        }
    }

    /**
     *
     * @param obj
     * @param message
     * @param template
     */
    private static synchronized void sendEmail(
            final Message obj,
            final SMTPMessage smtpMessage,
            final EmailProperties emailProperties
    ) {
        try {
            switch (obj.getMessageType()) {
                case SERVER_STARTUP: {
                    MimeMultipart content = MailMessageUtil.createStartupEmail(obj, emailProperties);
                    smtpMessage.setContent(content);
                    smtpMessage.setSubject(MailMessageUtil.EMAIL_STARTUP_SUBJECT);
                    break;
                }
                case CRITICAL_SERVER_FAILURE: {
                    MimeMultipart content = MailMessageUtil.createSosEmail(obj, emailProperties);
                    smtpMessage.setContent(content);
                    smtpMessage.setSubject(MailMessageUtil.EMAIL_SOS_SUBJECT);
                    exceptionCount++;
                    break;
                }
                case DEVICE_DOWN: {
                    MimeMultipart content = MailMessageUtil.createOfflineDeviceEmail(obj, emailProperties);
                    smtpMessage.setContent(content);
                    smtpMessage.setSubject(MailMessageUtil.EMAIL_OFFLINE_DEVICE_SUBJECT);
                    break;
                }
                default:
                    break;
            }

            Transport.send(smtpMessage);
        } catch (MessagingException | IOException e) {
            ERROR_LOGGER.error(getLogMetaInfo(), e);
        }
    }

    private static String getLogMetaInfo() {
        return TimezoneUtil.nowUtc() + " [MessageQueueProcessor.class]";
    }
}
