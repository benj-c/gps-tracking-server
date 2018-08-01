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
package org.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.server.dto.LastKnownLocationInfo;
import org.server.dto.Message;
import org.server.dto.properties.ServerProperties;
import org.server.dto.properties.SystemProperties;
import org.server.util.TimezoneUtil;
import org.server.workers.MessageQueueProcessor;
import org.server.workers.RequestQueueProcessorEngine;
import org.server.workers.RequestLookupEngine;
import org.server.workers.InboundRequestHandlerEngine;

public class ServerInitializer {

    private static final Logger ERROR_LOGGER = LogManager.getLogger("ErrorLog");
    private static final Logger DEBUG_LOGGER = LogManager.getLogger("DebugLog");

    static {
        try {
            Context.checkSysConfigs();
        } catch (IllegalStateException e) {
            ERROR_LOGGER.error(getLogMetaInfo(), e);
            ERROR_LOGGER.error("System exiting on error - 101");
            System.exit(0);
        }
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        LinkedBlockingQueue<String> requestQueue = null;
        LinkedBlockingQueue<Message> messageQueue = null;
        ConcurrentHashMap<Long, LastKnownLocationInfo> latestRequests = null;

        try {
            SystemProperties systemProperties = Context.getSystemProperties();
            requestQueue = new LinkedBlockingQueue<>(systemProperties.getServer().getRequestQueueSize());
            messageQueue = new LinkedBlockingQueue<>(systemProperties.getServer().getMessageQueueSize());
            latestRequests = new ConcurrentHashMap<>();
            // initiate server
            new ServerInitializer().init(
                    systemProperties.getServer(),
                    requestQueue,
                    messageQueue
            );
            // initiate data stream consumer threadnew
            RequestQueueProcessorEngine requestQueueProcessorEngine = new RequestQueueProcessorEngine(
                    requestQueue,
                    messageQueue,
                    latestRequests,
                    systemProperties.getAmp().isActive()
            );

            Thread rqpeThread = new Thread(requestQueueProcessorEngine);
            rqpeThread.setName("requestQueueProcessorEngine");
            rqpeThread.start();

            // initiate event message excecution service thread
            if (systemProperties.getServer().isMessagingService()) {
                MessageQueueProcessor messageQueueProcessor = new MessageQueueProcessor(
                        messageQueue,
                        systemProperties.getMail()
                );
                Thread messageMessageQueueProcessorThread = new Thread(messageQueueProcessor);
                messageMessageQueueProcessorThread.setName("messageQueueProcessorThread");
                messageMessageQueueProcessorThread.start();
            }

            // initiate request lookup service
            if (systemProperties.getServer().isRequestLookupService()) {
                Timer reqListnerTimer = new Timer("RequestLookupEngine");
                RequestLookupEngine requestLookupEngine = new RequestLookupEngine(
                        messageQueue,
                        latestRequests,
                        systemProperties.getAmp().isActive()
                );
                reqListnerTimer.schedule(
                        requestLookupEngine,
                        0l,
                        systemProperties.getServer().getRequestLookupInterval()
                );
            }

            DEBUG_LOGGER.debug("Service started - " + ManagementFactory.getRuntimeMXBean().getName());
            messageQueue.put(Message.getBuilder()
                    .type(Message.MessageType.SERVER_STARTUP)
                    .payload(Arrays.asList(
                            systemProperties.getServer().getServerAddress(),
                            systemProperties.getDb().getServerAddress()
                    )).timestamp(TimezoneUtil.nowLocal(TimezoneUtil.TIMEZONE_SL))
                    .build()
            );
        } catch (IOException | InterruptedException | NumberFormatException | IllegalStateException ex) {
            ERROR_LOGGER.error(getLogMetaInfo(), ex);
            try {
                if (null != messageQueue) {
                    messageQueue.put(Message.getBuilder()
                            .type(Message.MessageType.CRITICAL_SERVER_FAILURE)
                            .payload(ex)
                            .timestamp(TimezoneUtil.nowLocal(TimezoneUtil.TIMEZONE_SL))
                            .build()
                    );
                }
            } catch (InterruptedException ex1) {
                ERROR_LOGGER.error(getLogMetaInfo(), ex);
            }
        }

    }

    /**
     *
     * @param port
     * @param queue
     * @param backlog
     * @param mq
     * @throws java.io.InterruptedException
     */
    private void init(
            final ServerProperties serverProperties,
            final LinkedBlockingQueue<String> queue,
            final LinkedBlockingQueue<Message> mq
    ) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new InboundRequestHandlerEngine(queue, mq));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, serverProperties.getBacklog())
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        // Bind and start to accept incoming connections.
        b.bind(serverProperties.getPort()).sync();
    }

    private static String getLogMetaInfo() {
        return TimezoneUtil.nowLocal("Asia/Colombo") + " [ServerInitializer.class]";
    }
}
