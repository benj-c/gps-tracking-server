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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.server.dto.Message;
import org.server.protocol.Tk103ProtocolDecoder;
import org.server.util.TimezoneUtil;

public final class InboundRequestHandlerEngine extends ChannelInboundHandlerAdapter {

    private static final Logger ERROR_LOGGER = LogManager.getLogger("ErrorLog");
    private static final Logger REQ_LOGGER = LogManager.getLogger("RequestLog");

    private final LinkedBlockingQueue<String> queue;
    private final LinkedBlockingQueue<Message> mq;
    private ByteBuf tmp;

    private static final int BYTE_BUFFER_INIT_CAPACITY = 190;
    private static final int[] DEVICE_ID_POSITION = {1, 13};
    private static final int[] COMMAND_POSITION = {13, 17};

    /**
     *
     * @param q
     * @param mq
     */
    public InboundRequestHandlerEngine(
            LinkedBlockingQueue<String> q,
            LinkedBlockingQueue<Message> mq
    ) {
        this.queue = q;
        this.mq = mq;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        tmp = ctx.alloc().buffer(BYTE_BUFFER_INIT_CAPACITY);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        tmp.release();
        tmp = null;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        tmp.writeBytes(in);
        in.release();
        StringBuilder sb = new StringBuilder();
        try {
            while (tmp.isReadable()) {
                sb.append((char) tmp.readByte());
            }

            String oriRequest = sb.toString();
            REQ_LOGGER.info(TimezoneUtil.nowLocal("Asia/Colombo") + "-" + oriRequest);
            ChannelFuture f = null;

            if (Objects.nonNull(oriRequest) && oriRequest.length() > 17) {
                String deviceId = oriRequest.substring(DEVICE_ID_POSITION[0], DEVICE_ID_POSITION[1]);
                String command = oriRequest.substring(COMMAND_POSITION[0], COMMAND_POSITION[1]);

                switch (command) {
                    //BP05
                    case Tk103ProtocolDecoder.CMD_LOGIN: {
                        this.queue.put(oriRequest);
                        f = ctx.writeAndFlush(Tk103ProtocolDecoder.getLoginCommandResponse(deviceId));
                        break;
                    }
                    //BP00
                    case Tk103ProtocolDecoder.CMD_HANDSHAKE_SIGNAL: {
                        f = ctx.writeAndFlush(Tk103ProtocolDecoder.getHandshakeResponse(deviceId));
                        break;
                    }
                    //BR00
                    case Tk103ProtocolDecoder.CMD_CONTINUES_FEEDBACK: {
                        f = ctx.writeAndFlush("No");
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
            //channel close
            if (f != null) {
                f.addListener(ChannelFutureListener.CLOSE);
            }
        } catch (InterruptedException ex) {
            try {
                exceptionCaught(null, ex);
            } catch (Exception ex1) {
                ERROR_LOGGER.error(getLogMetaInfo(), ex1);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ERROR_LOGGER.error(getLogMetaInfo(), cause);
        try {
            this.mq.put(Message.getBuilder()
                    .type(Message.MessageType.CRITICAL_SERVER_FAILURE)
                    .message(cause.getLocalizedMessage())
                    .timestamp(TimezoneUtil.nowLocal(TimezoneUtil.TIMEZONE_SL))
                    .payload(cause)
                    .build()
            );
        } catch (InterruptedException ex) {
            ERROR_LOGGER.error(getLogMetaInfo(), ex);
        }
    }

    private static String getLogMetaInfo() {
        return TimezoneUtil.nowUtc() + " [InboundRequestHandlerEngine.class]";
    }
}
