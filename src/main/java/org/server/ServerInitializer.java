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
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import org.server.Context.ExceptionLogger;
import org.server.dto.Message;
import org.server.dto.properties.SystemProperties;
import org.server.util.TimezoneUtil;
import org.server.workers.MessageQueueProcessor;
import org.server.workers.RequestQueueProcessorEngine;
import org.server.workers.RequestLookupEngine;
import org.server.workers.InboundRequestHandlerEngine;

public class ServerInitializer {

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        LinkedBlockingQueue<String> requestQueue = null;
        LinkedBlockingQueue<Message> messageQueue = null;
        ConcurrentHashMap<Long, Date> latestRequests = null;

        Context.configureLogger();
        try {
            SystemProperties systemProperties = Context.getSystemProperties();

            requestQueue = new LinkedBlockingQueue<>(systemProperties.getServer().getRequestQueueSize());
            messageQueue = new LinkedBlockingQueue<>(systemProperties.getServer().getMessageQueueSize());
            latestRequests = new ConcurrentHashMap<>();
            // initiate server
            new ServerInitializer().init(
                    systemProperties.getServer().getPort(),
                    systemProperties.getServer().getBacklog(),
                    requestQueue,
                    messageQueue
            );
            // initiate data stream consumer thread
            Thread requestQueueProcessorEngine = new Thread(new RequestQueueProcessorEngine(
                    requestQueue,
                    messageQueue,
                    latestRequests,
                    systemProperties.getAmp().isActive()
            ));
            requestQueueProcessorEngine.setName("requestQueueProcessorEngine");
            requestQueueProcessorEngine.start();

            // initiate event message excecution service thread
            if (systemProperties.getServer().isMessagingService()) {
                Thread messageMessageQueueProcessorThread = new Thread(new MessageQueueProcessor(
                        messageQueue,
                        systemProperties.getMail()
                ));
                messageMessageQueueProcessorThread.setName("messageMessageQueueProcessorThread");
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

            messageQueue.put(Message.getBuilder()
                    .type(Message.MessageType.SERVER_STARTUP)
                    .payload(Arrays.asList(
                            systemProperties.getServer().getServerAddress(),
                            systemProperties.getDb().getServerAddress()
                    )).timestamp(TimezoneUtil.getGmtTime(TimezoneUtil.TIMEZONE_SL))
                    .build()
            );
        } catch (IOException | InterruptedException | NumberFormatException ex) {
            ExceptionLogger.error(ex, ServerInitializer.class, TimezoneUtil.getUtcTime().toString());
            try {
                if (null != messageQueue) {
                    messageQueue.put(Message.getBuilder()
                            .type(Message.MessageType.CRITICAL_SERVER_FAILURE)
                            .payload(ex)
                            .timestamp(TimezoneUtil.getGmtTime(TimezoneUtil.TIMEZONE_SL))
                            .build()
                    );
                }
            } catch (InterruptedException ex1) {
                ExceptionLogger.error(ex1, ServerInitializer.class, TimezoneUtil.getUtcTime().toString());
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
            int port,
            int backlog,
            LinkedBlockingQueue<String> queue,
            LinkedBlockingQueue<Message> mq
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
                .option(ChannelOption.SO_BACKLOG, backlog)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        // Bind and start to accept incoming connections.
        b.bind(port).sync();
    }
}
