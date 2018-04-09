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
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import javax.jms.JMSException;
import org.server.Context.ExceptionLogger;
import org.server.connection.ActiveMq;
import org.server.db.DBOperationsHandler;
import org.server.dto.Message;
import org.server.dto.Vehicle;
import org.server.parsers.Tk103Parser;
import org.server.util.TimezoneUtil;

public final class RequestLookupEngine extends TimerTask {

    private final ConcurrentHashMap<Long, Date> latestReqs;
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
            ConcurrentHashMap<Long, Date> latestReqs,
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
    private ConcurrentHashMap<Long, Date> getLatestRequestsQueue() {
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
            LocalDateTime pre = LocalDateTime.ofInstant(v.toInstant(), ZoneId.of("Asia/Colombo"));
            LocalDateTime now = TimezoneUtil.getZonedTimestamp("Asia/Colombo");
            long minutes = ChronoUnit.MINUTES.between(pre, now);

            if (minutes > 20) {
                try {
                    Vehicle vehicle = DBOperationsHandler.getVehicle(k);
                    if (vehicle != null) {
                        offlineVehicles.add(vehicle);
                        if (isAmpActive()) {
                            ActiveMq.sendMessage("REQSTATUS." + k, String.valueOf(Tk103Parser.DEVICE_OFFLINE));
                        }
                    }
                } catch (IOException | ClassNotFoundException | JMSException | MongoException ex) {
                    ExceptionLogger.error(ex, getClass(), TimezoneUtil.getUtcTime().toString());
                }
            }
        });
        try {
            if (!offlineVehicles.isEmpty()) {
                getMessageQueue().put(Message.getBuilder()
                        .type(Message.MessageType.DEVICE_DOWN)
                        .message("")
                        .timestamp(TimezoneUtil.getGmtTime(TimezoneUtil.TIMEZONE_SL))
                        .payload(offlineVehicles)
                        .build()
                );
            }
        } catch (InterruptedException ex) {
            ExceptionLogger.error(ex, getClass(), TimezoneUtil.getUtcTime().toString());
        }
    }
}
