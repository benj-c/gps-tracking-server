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
package org.server.dto.properties;

import java.util.Properties;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class EmailProperties {

    private String host;
    private int port;
    private String contentType;
    private String contentCharset;
    private String store_protocol;
    private String transport_protocol;
    private String socket_factory;
    private boolean smtp_socket_factory_fallback;
    private boolean pop3_socket_factory_fallback;
    private boolean smtp_starttls;
    private boolean smtp_auth;
    private boolean smtp_debug;
    private boolean debug_auth;
    private String sender;
    private String sender_id;
    private String sender_passwd;
    private String devs;
    private boolean devsEnable;
    private String production;
    private boolean productionEnable;

    /**
     *
     * @return
     */
    public Properties getEmailProperties() {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", this.getHost());
        properties.setProperty("mail.smtp.socketFactory.class", this.getSocket_factory());
        properties.setProperty("mail.smtp.socketFactory.fallback", String.valueOf(this.isSmtp_socket_factory_fallback()));
        properties.setProperty("mail.smtp.port", String.valueOf(this.getPort()));
        properties.setProperty("mail.smtp.socketFactory.port", String.valueOf(this.getPort()));
        properties.put("mail.smtp.starttls.enable", String.valueOf(this.isSmtp_starttls()));
        properties.put("mail.smtp.auth", String.valueOf(this.isSmtp_auth()));
        properties.put("mail.debug", String.valueOf(this.isSmtp_debug()));
        properties.put("mail.store.protocol", this.getStore_protocol());
        properties.put("mail.transport.protocol", this.getTransport_protocol());
        properties.put("mail.debug.auth", String.valueOf(this.isDebug_auth()));
        properties.setProperty("mail.pop3.socketFactory.fallback", String.valueOf(this.isPop3_socket_factory_fallback()));
        return properties;
    }

    /**
     * @return the devs
     * @throws javax.mail.internet.AddressException
     */
    public InternetAddress[] getDevsAddresses() throws AddressException {
        return InternetAddress.parse(this.getDevs());
    }

    /**
     * @return the production
     * @throws javax.mail.internet.AddressException
     */
    public InternetAddress[] getProductionAddresses() throws AddressException {
        return InternetAddress.parse(this.getProduction());
    }

}
