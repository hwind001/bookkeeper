/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.usecases;

import java.util.List;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.hedwig.jms.SessionImpl;
import org.apache.hedwig.jms.spi.HedwigConnectionFactoryImpl;

import org.apache.activemq.test.JmsSendReceiveTestSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompositePublishTest extends JmsSendReceiveTestSupport {
    private static final Logger LOG = LoggerFactory.getLogger(CompositePublishTest.class);

    protected Connection sendConnection;
    protected Connection receiveConnection;
    protected Session receiveSession;
    protected MessageConsumer[] consumers;
    protected List[] messageLists;

    @SuppressWarnings("unchecked")
    protected void setUp() throws Exception {
        super.setUp();

        connectionFactory = createConnectionFactory();

        sendConnection = createConnection(false);
        sendConnection.start();

        receiveConnection = createConnection(false);
        receiveConnection.start();

        LOG.info("Created sendConnection: " + sendConnection);
        LOG.info("Created receiveConnection: " + receiveConnection);

        session = sendConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        receiveSession = receiveConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        LOG.info("Created sendSession: " + session);
        LOG.info("Created receiveSession: " + receiveSession);

        producer = session.createProducer(null);

        LOG.info("Created producer: " + producer);

        consumerDestination = session.createTopic(getConsumerSubject());
        producerDestination = session.createTopic(getProducerSubject());

        LOG.info("Created  consumer destination: " + consumerDestination
                 + " of type: " + consumerDestination.getClass());
        LOG.info("Created  producer destination: " + producerDestination
                 + " of type: " + producerDestination.getClass());

        Destination[] destinations = getDestinations();
        consumers = new MessageConsumer[destinations.length];
        messageLists = new List[destinations.length];
        for (int i = 0; i < destinations.length; i++) {
            Destination dest = destinations[i];
            messageLists[i] = createConcurrentList();
            consumers[i] = receiveSession.createConsumer(dest);
            consumers[i].setMessageListener(createMessageListener(i, messageLists[i]));
        }

        LOG.info("Started connections");
    }

    protected MessageListener createMessageListener(int i, final List<Message> messageList) {
        return new MessageListener() {
            public void onMessage(Message message) {
                consumeMessage(message, messageList);
            }
        };
    }

    /**
     * Returns the subject on which we publish
     */
    protected String getSubject() {
        // return getPrefix() + "FOO.BAR," + getPrefix() + "FOO.X.Y";
        return getPrefix() + "FOO.BAR";
    }

    /**
     * Returns the destinations to which we consume
     */
    protected Destination[] getDestinations() {
        // return new Destination[] {SessionImpl.asTopic(getPrefix() + "FOO.BAR"),
        // SessionImpl.asTopic(getPrefix() + "FOO.*"), SessionImpl.asTopic(getPrefix() + "FOO.X.Y")};
        return new Destination[] {SessionImpl.asTopic(getPrefix() + "FOO.BAR")};
    }

    protected String getPrefix() {
        return super.getSubject() + ".";
    }

    @SuppressWarnings("unchecked")
    protected void assertMessagesAreReceived() throws JMSException {
        waitForMessagesToBeDelivered();
        int size = messageLists.length;
        for (int i = 0; i < size; i++) {
            LOG.info("Message list: " + i + " contains: " + messageLists[i].size() + " message(s)");
        }
        size = messageLists.length;
        for (int i = 0; i < size; i++) {
            assertMessagesReceivedAreValid(messageLists[i]);
        }
    }

    protected HedwigConnectionFactoryImpl createConnectionFactory() {
        return new HedwigConnectionFactoryImpl();
    }

    protected void tearDown() throws Exception {
        session.close();
        receiveSession.close();

        sendConnection.close();
        receiveConnection.close();
        super.tearDown();
    }
}
