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

import com.mongodb.MongoException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import javax.jms.JMSException;
import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.server.connection.ActiveMq;
import org.server.db.DBOperationsHandler;
import org.server.db.DatabaseHandler;
import org.server.dto.LastKnownLocationInfo;
import org.server.dto.Message;
import org.server.dto.Vehicle;
import org.server.protocol.Tk103ProtocolDecoder;
import org.server.util.TimezoneUtil;

public final class RequestLookupEngine extends TimerTask {

    private static final Logger ERROR_LOGGER = LogManager.getLogger("ErrorLog");
    private static final Logger DEBUG_LOGGER = LogManager.getLogger("DebugLog");

    private final DatabaseHandler databaseHandler = new DBOperationsHandler();
    private final ConcurrentHashMap<Long, LastKnownLocationInfo> latestReqs;
    private final LinkedBlockingQueue<Message> mq;
    private final boolean ampActive;

    /**
     *
     * @param mq
     * @param latestReqs
     * @param ampActive
     */
    public RequestLookupEngine(
            LinkedBlockingQueue<Message> mq,
            ConcurrentHashMap<Long, LastKnownLocationInfo> latestReqs,
            boolean ampActive
    ) {
        this.mq = mq;
        this.latestReqs = latestReqs;
        this.ampActive = ampActive;
    }

    /**
     *
     * @return
     */
    private ConcurrentHashMap<Long, LastKnownLocationInfo> getLatestRequestsQueue() {
        return latestReqs;
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
    private DatabaseHandler getDatabaseHandler() {
        return databaseHandler;
    }

    /**
     *
     * @return
     */
    private boolean isAmpActive() {
        return ampActive;
    }

    @Override
    public void run() {
        latestRequestLookup();
    }

    protected void latestRequestLookup() {
        List<Vehicle> offlineVehicles = new ArrayList<>();
        //k - imei
        //v - time str
        getLatestRequestsQueue().forEach((k, v) -> {
            LocalDateTime now = TimezoneUtil.getZonedTimestamp("Asia/Colombo");
            long minutes = ChronoUnit.MINUTES.between(v.getRequestTimestamp(), now);

            if (minutes > 20) {
                try {
                    Vehicle vehicle = getDatabaseHandler().getVehicle(k);
                    if (vehicle != null) {
                        offlineVehicles.add(vehicle);
                        if (isAmpActive()) {
                            ActiveMq.sendMessage("REQSTATUS." + k, String.valueOf(Tk103ProtocolDecoder.DEVICE_OFFLINE));
                        }
                    }
                } catch (IOException | ClassNotFoundException | JMSException | MongoException ex) {
                    ERROR_LOGGER.error(getLogMetaInfo(), ex);
                }
            }
        });
        try {
            if (!offlineVehicles.isEmpty()) {
                getMessageQueue().put(Message.getBuilder()
                        .type(Message.MessageType.DEVICE_DOWN)
                        .message("")
                        .timestamp(TimezoneUtil.nowLocal(TimezoneUtil.TIMEZONE_SL))
                        .payload(offlineVehicles)
                        .build()
                );
            }
        } catch (InterruptedException ex) {
            ERROR_LOGGER.error(getLogMetaInfo(), ex);
        }
    }

    private static String getLogMetaInfo() {
        return TimezoneUtil.nowUtc() + " [RequestLookupEngine.class]";
    }
}
