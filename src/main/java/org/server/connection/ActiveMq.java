/*
 * Copyright 2018 NULL.
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
package org.server.connection;

import java.io.IOException;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.server.Context;

/**
 *
 * @author NULL
 */
public class ActiveMq {

    public static Connection CONNECTION;

    private ActiveMq() {
    }

    /**
     *
     * @return @throws JMSException
     * @throws java.io.IOException
     */
    public static Connection getConnectionInstance() throws JMSException, IOException {
        if (null == CONNECTION) {
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(Context.getSystemProperties().getAmp().getUrl());
            CONNECTION = connectionFactory.createConnection();
            CONNECTION.start();
        }
        return CONNECTION;
    }

    /**
     *
     * @param topic
     * @param message
     * @throws JMSException
     * @throws java.io.IOException
     */
    public static synchronized void sendMessage(String topic, String message) throws JMSException, IOException {
        Session session = getConnectionInstance().createSession(false, Session.AUTO_ACKNOWLEDGE);

        Topic createTopic = session.createTopic(topic);
        MessageProducer producer = session.createProducer(createTopic);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);

        TextMessage txtMessage = session.createTextMessage(message);
        producer.send(txtMessage);

        producer.close();
        session.close();
    }
}
