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
package org.server.util;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import org.server.Context;
import org.server.dto.Message;
import org.server.dto.Vehicle;
import org.server.dto.mail.SosMessage;
import org.server.dto.mail.StartUpMessage;
import org.server.dto.properties.EmailProperties;

public final class MailMessageUtil {

    public static final String EMAIL_STARTUP_TEMPLATE = "mail-alita-startup.mustache";
    public static final String EMAIL_SOS_TEMPLATE = "mail-alita-sos.mustache";
    public static final String EMAIL_OFFLINE_DEVICE_TEMPLATE = "mail-alita-offline-device.mustache";

    public static final String EMAIL_STARTUP_SUBJECT = "Alita is waking up";
    public static final String EMAIL_SOS_SUBJECT = "SOS !";
    public static final String EMAIL_OFFLINE_DEVICE_SUBJECT = "Wanna see offline devices ?";

    public static final MustacheFactory MUSTACHE_FACTORY = new DefaultMustacheFactory();

    /**
     *
     * @param faceType
     * @param cid
     * @return
     * @throws IOException
     * @throws MessagingException
     */
    public static final MimeBodyPart getAlitaFaces(final String faceType, final long cid) throws IOException, MessagingException {
        final MimeBodyPart imagePart = new MimeBodyPart();
        switch (faceType) {
            case Context.ALITA_FACE_SMILE: {
                imagePart.attachFile(Context.getAlitaSmile());
                imagePart.setContentID("<" + cid + ">");
                imagePart.setDisposition(MimeBodyPart.INLINE);
                break;
            }
            case Context.ALITA_FACE_SCARED: {
                imagePart.attachFile(Context.getAlitaScared());
                imagePart.setContentID("<" + cid + ">");
                imagePart.setDisposition(MimeBodyPart.INLINE);
                break;
            }
            default:
                break;
        }
        return imagePart;

    }

    /**
     *
     * @param templateUrl
     * @param object
     * @return
     */
    private static String getEmailTemplateContent(final String templateUrl, final Object object) {
        final StringWriter writer = new StringWriter();
        final Mustache compile = MUSTACHE_FACTORY.compile(templateUrl);
        compile.execute(writer, object);
        return writer.toString();
    }

    /**
     *
     * @param obj
     * @param emailProperties
     * @return
     * @throws MessagingException
     * @throws IOException
     */
    public static final MimeMultipart createStartupEmail(
            final Message obj,
            final EmailProperties emailProperties
    ) throws MessagingException, IOException {
        final MimeMultipart content = new MimeMultipart("related");
        final MimeBodyPart textPart = new MimeBodyPart();
        final long cid = System.currentTimeMillis();

        final List<String> list = (List<String>) obj.getPayload();

        textPart.setText(
                getEmailTemplateContent(
                        MailMessageUtil.EMAIL_STARTUP_TEMPLATE,
                        new StartUpMessage(list.get(0), list.get(1), cid, obj.getTimestamp())
                ),
                emailProperties.getContentCharset(),
                emailProperties.getContentType()
        );
        content.addBodyPart(textPart);

        //image
        content.addBodyPart(MailMessageUtil.getAlitaFaces(Context.ALITA_FACE_SMILE, cid));
        return content;
    }

    /**
     *
     * @param obj
     * @param emailProperties
     * @return
     * @throws MessagingException
     * @throws IOException
     */
    public static MimeMultipart createSosEmail(
            final Message obj,
            final EmailProperties emailProperties
    ) throws MessagingException, IOException {
        final MimeMultipart content = new MimeMultipart("related");
        final MimeBodyPart textPart = new MimeBodyPart();
        final long cid = System.currentTimeMillis();

        textPart.setText(
                getEmailTemplateContent(
                        MailMessageUtil.EMAIL_SOS_TEMPLATE,
                        new SosMessage(obj.getMessage(), (Throwable) obj.getPayload(), cid, obj.getTimestamp())
                ),
                emailProperties.getContentCharset(),
                emailProperties.getContentType()
        );
        content.addBodyPart(textPart);

        //image
        content.addBodyPart(MailMessageUtil.getAlitaFaces(Context.ALITA_FACE_SCARED, cid));
        return content;
    }

    /**
     *
     * @param obj
     * @param emailProperties
     * @return
     * @throws MessagingException
     * @throws IOException
     */
    public static MimeMultipart createOfflineDeviceEmail(
            final Message obj,
            final EmailProperties emailProperties
    ) throws MessagingException, IOException {
        final MimeMultipart content = new MimeMultipart("related");
        final MimeBodyPart textPart = new MimeBodyPart();
        final long cid = System.currentTimeMillis();

        final List<Vehicle> offlineDevices = (List<Vehicle>) obj.getPayload();
        final Map<String, Object> context = new HashMap<>();
        context.put("offlineDevices", offlineDevices);
        context.put("cid", cid);

        textPart.setText(
                getEmailTemplateContent(
                        MailMessageUtil.EMAIL_SOS_TEMPLATE,
                        context
                ),
                emailProperties.getContentCharset(),
                emailProperties.getContentType()
        );
        content.addBodyPart(textPart);

        //image
        content.addBodyPart(MailMessageUtil.getAlitaFaces(Context.ALITA_FACE_SMILE, cid));
        return content;
    }

    /**
     *
     * @param usermail
     * @param password
     * @param properties
     * @return
     */
    public static Session createMailSession(String usermail, String password, Properties properties) {
        return Session.getDefaultInstance(
                properties,
                new AuthenticatorImpl(usermail, password)
        );
    }

    /**
     *
     */
    private static class AuthenticatorImpl extends Authenticator {

        private final String usermail;
        private final String password;

        public AuthenticatorImpl(String usermail, String password) {
            this.usermail = usermail;
            this.password = password;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(usermail, password);
        }
    }

}
