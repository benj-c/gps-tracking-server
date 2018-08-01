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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import javax.jms.JMSException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.server.connection.ActiveMq;
import org.server.db.DBOperationsHandler;
import org.server.db.DatabaseHandler;
import org.server.dto.LastKnownLocationInfo;
import org.server.dto.Message;
import org.server.dto.Location;
import org.server.protocol.Tk103ProtocolDecoder;
import org.server.util.DistanceCalculator;
import org.server.util.TimezoneUtil;
import org.server.protocol.BaseProtocolDecoder;

public class RequestQueueProcessorEngine implements Runnable {

    private static final Logger ERROR_LOGGER = LogManager.getLogger("ErrorLog");
    private static final Logger DEBUG_LOGGER = LogManager.getLogger("DebugLog");

    private final DatabaseHandler databaseHandler;
    private final BaseProtocolDecoder protocolParser;
    private final LinkedBlockingQueue<String> queue;
    private final ConcurrentHashMap<Long, LastKnownLocationInfo> latest_reqs;
    private final LinkedBlockingQueue<Message> mq;
    private final boolean ampActive;

    private static final String AMQ_PREFIX_DEFAULT = "COO.";
    private static final String AMQ_PREFIX_REQUEST_STATUS = "REQSTATUS.";
    private static final double ABSOLUTE_LATITUDE = 0.0;
    private static final double ABSOLUTE_LONGITUDE = 0.0;

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
            ConcurrentHashMap<Long, LastKnownLocationInfo> latest_reqs,
            boolean ampActive
    ) {
        this.queue = q;
        this.mq = mq;
        this.latest_reqs = latest_reqs;
        this.ampActive = ampActive;
        this.databaseHandler = new DBOperationsHandler();
        this.protocolParser = new Tk103ProtocolDecoder();
    }

    /**
     *
     * @return
     */
    private DatabaseHandler getDatabaseHandler() {
        return databaseHandler;
    }

    /**
     *
     * @return
     */
    private BaseProtocolDecoder getProtocolParser() {
        return protocolParser;
    }

    /**
     *
     * @return
     */
    private ConcurrentHashMap<Long, LastKnownLocationInfo> getLatestRequests() {
        return latest_reqs;
    }

    @Override
    public void run() {
        while (true) {
            try {
                List<Location> locations = getProtocolParser().parseRequest(queue.take());
                if (!locations.isEmpty()) {
                    for (int i = 0; i < locations.size(); i++) {
                        int objType = locations.get(i).getType();
                        switch (objType) {
                            case Tk103ProtocolDecoder.LOCATION_OK: {
                                LastKnownLocationInfo lkt = getLatestRequests().get(locations.get(i).getImei());
                                if (lkt == null) {
                                    getLatestRequests().put(
                                            locations.get(i).getImei(),
                                            new LastKnownLocationInfo(
                                                    locations.get(i).getTimestamp(),
                                                    new Double[]{
                                                        locations.get(i).getPoint().get(0),
                                                        locations.get(i).getPoint().get(1)
                                                    }
                                            )
                                    );
                                }
                                LastKnownLocationInfo lastLocation = getLatestRequests().get(locations.get(i).getImei());
                                Double[] last_location_op = lastLocation.getCoordinates();
                                double distance = 0;
                                if (last_location_op[0] != ABSOLUTE_LATITUDE && last_location_op[1] != ABSOLUTE_LONGITUDE) {
                                    distance = DistanceCalculator.distance(
                                            last_location_op[0],
                                            last_location_op[1],
                                            locations.get(i).getPoint().get(0),
                                            locations.get(i).getPoint().get(1)
                                    );
                                }
                                locations.get(i).setDistance(distance);
                                getDatabaseHandler().insertCoodinates(locations.get(i));
                                getLatestRequests().put(
                                        locations.get(i).getImei(),
                                        new LastKnownLocationInfo(
                                                locations.get(i).getTimestamp(),
                                                new Double[]{
                                                    locations.get(i).getPoint().get(0),
                                                    locations.get(i).getPoint().get(1)
                                                }
                                        )
                                );

                                if (this.ampActive) {
                                    ActiveMq.sendMessage(AMQ_PREFIX_DEFAULT + locations.get(i).getImei(), new Gson().toJson(locations));
                                    ActiveMq.sendMessage(AMQ_PREFIX_REQUEST_STATUS + locations.get(i).getImei(), String.valueOf(Tk103ProtocolDecoder.LOCATION_OK));
                                }

                                break;
                            }
                            case Tk103ProtocolDecoder.LOCATION_UNAVAILABLE: {
                                if (this.ampActive) {
                                    ActiveMq.sendMessage(AMQ_PREFIX_REQUEST_STATUS + locations.get(i).getImei(), String.valueOf(Tk103ProtocolDecoder.LOCATION_UNAVAILABLE));
                                }
                                break;
                            }
                            case Tk103ProtocolDecoder.LOCATION_UNDEFINED: {
                                if (this.ampActive) {
                                    ActiveMq.sendMessage(AMQ_PREFIX_REQUEST_STATUS + locations.get(i).getImei(), String.valueOf(Tk103ProtocolDecoder.LOCATION_UNDEFINED));
                                }
                                break;
                            }
                            default: {
                                break;
                            }
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
        ERROR_LOGGER.error(getLogMetaInfo(), cause);
        try {
            mq.put(Message.getBuilder()
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

    /**
     *
     * @return
     */
    private static String getLogMetaInfo() {
        return TimezoneUtil.nowUtc() + " [RequestQueueProcessorEngine.class]";
    }
}
