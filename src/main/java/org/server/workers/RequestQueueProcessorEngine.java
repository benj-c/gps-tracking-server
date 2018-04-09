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

import com.google.gson.Gson;
import com.mongodb.MongoException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import javax.jms.JMSException;
import org.server.Context.ExceptionLogger;
import org.server.connection.ActiveMq;
import org.server.db.DBOperationsHandler;
import org.server.dto.Message;
import org.server.dto.Location;
import org.server.parsers.Tk103Parser;
import org.server.util.DistanceCalculator;
import org.server.util.TimezoneUtil;

public class RequestQueueProcessorEngine implements Runnable {

    private final LinkedBlockingQueue<String> queue;
    private final ConcurrentHashMap<Long, Date> latest_reqs;
    private final LinkedBlockingQueue<Message> mq;
    private final boolean ampActive;

    /**
     *
     * @param q
     * @param mq
     * @param latest_reqs
     * @param ampActive
     */
    public RequestQueueProcessorEngine(
            LinkedBlockingQueue<String> q,
            LinkedBlockingQueue<Message> mq,
            ConcurrentHashMap<Long, Date> latest_reqs,
            boolean ampActive
    ) {
        this.queue = q;
        this.mq = mq;
        this.latest_reqs = latest_reqs;
        this.ampActive = ampActive;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Location parsed_data = Tk103Parser.parseRequest(queue.take());
                if (parsed_data != null) {
                    int objType = parsed_data.getType();
                    switch (objType) {
                        case Tk103Parser.LOCATION_OK: {
                            double[] last_location_op = DBOperationsHandler.findLastLocation(parsed_data.getImei());
                            double distance = 0;
                            if (last_location_op[0] != 0.0 && last_location_op[1] != 0.0) {
                                distance = DistanceCalculator.distance(
                                        last_location_op[0],
                                        last_location_op[1],
                                        parsed_data.getPoint().get(0),
                                        parsed_data.getPoint().get(1)
                                );
                            }
                            parsed_data.setDistance(distance);
                            DBOperationsHandler.insertCoodinates(parsed_data);
                            if (this.ampActive) {
                                ActiveMq.sendMessage("COO." + parsed_data.getImei(), new Gson().toJson(parsed_data));
                                ActiveMq.sendMessage("REQSTATUS." + parsed_data.getImei(), String.valueOf(Tk103Parser.LOCATION_OK));
                            }
                            this.latest_reqs.put(parsed_data.getImei(), parsed_data.getTimestamp());
                            break;
                        }
                        case Tk103Parser.LOCATION_UNAVAILABLE: {
                            if (this.ampActive) {
                                ActiveMq.sendMessage("REQSTATUS." + parsed_data.getImei(), String.valueOf(Tk103Parser.LOCATION_UNAVAILABLE));
                            }
                            break;
                        }
                        case Tk103Parser.LOCATION_UNDEFINED: {
                            if (this.ampActive) {
                                ActiveMq.sendMessage("REQSTATUS." + parsed_data.getImei(), String.valueOf(Tk103Parser.LOCATION_UNDEFINED));
                            }
                            break;
                        }
                        default: {
                            break;
                        }
                    }

                }
            } catch (IOException | ClassNotFoundException | InterruptedException | ParseException | JMSException | MongoException ex) {
                exceptionCaught(ex);
            }
        }
    }

    /**
     *
     * @param cause
     */
    public void exceptionCaught(Throwable cause) {
        ExceptionLogger.error(cause, getClass(), TimezoneUtil.getUtcTime().toString());
        try {
            mq.put(Message.getBuilder()
                    .type(Message.MessageType.CRITICAL_SERVER_FAILURE)
                    .message(cause.getLocalizedMessage())
                    .timestamp(TimezoneUtil.getGmtTime(TimezoneUtil.TIMEZONE_SL))
                    .payload(cause)
                    .build()
            );
        } catch (InterruptedException ex) {
            ExceptionLogger.error(ex, getClass(), TimezoneUtil.getUtcTime().toString());
        }
    }

}
