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
package org.server.protocol;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.server.dto.Location;

public final class Tk103ProtocolDecoder implements BaseProtocolDecoder {

    private static final Logger ERROR_LOGGER = LogManager.getLogger("ErrorLog");
    //protocol specific commands
    public static final String CMD_LOGIN = "BP05";
    public static final String CMD_HANDSHAKE_SIGNAL = "BP00";
    public static final String CMD_CONTINUES_FEEDBACK = "BR00";

    public static final String CMD_LOGIN_RESPONSE = "AP05";
    public static final String CMD_HANDSHAKE_SIGNAL_RESPONSE = "AP01HSO";

    //identify parsed data object
    public static final int LOCATION_OK = 1;
    public static final int LOCATION_UNAVAILABLE = 0;
    public static final int LOCATION_UNDEFINED = -1;
    public static final int DEVICE_OFFLINE = -2;
    public static final int INVALID_REQUEST = -3;

    private static final String[] LOCATION_UNDEFINED_CHARS = {"V", "{", "}", ",,", ","};

    /**
     *
     * @param deviceId
     * @return
     */
    public static String getHandshakeResponse(String deviceId) {
        return "(" + deviceId + CMD_HANDSHAKE_SIGNAL_RESPONSE + ")";
    }

    /**
     *
     * @param deviceId
     * @return
     */
    public static String getLoginCommandResponse(String deviceId) {
        return "(" + deviceId + CMD_LOGIN_RESPONSE + ")";
    }

    /**
     *
     * @param req
     * @return
     */
    @Override
    public boolean isValidRequest(String req) {
        for (String ch : LOCATION_UNDEFINED_CHARS) {
            if (req.contains(ch)) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @return
     */
    private Location getInvalidLocationObj() {
        return Location.builder().type(INVALID_REQUEST).build();
    }

    /**
     *
     * @param request
     * @return
     * @throws java.text.ParseException
     */
    @Override
    public List<Location> parseRequest(String request) throws ParseException, IllegalArgumentException {
        List<Location> locations = new ArrayList<>();
        if (request == null || request.length() <= 1) {
            locations.add(getInvalidLocationObj());
            return locations;
        }

        for (String ch : LOCATION_UNDEFINED_CHARS) {
            if (request.contains(ch)) {
                locations.add(getInvalidLocationObj());
                return locations;
            }
        }

        if (request.contains(CMD_LOGIN)) {
            String[] channels = request.split("\\)");
            LocalDateTime timestamp = LocalDateTime.now();
            long imei = 0;
            for (String channel : channels) {
                String replace = channel.replace("(", "");
                if (replace.contains(CMD_LOGIN)) {
                    try {
                        Location decoded = decodeBp05Message(replace);
                        timestamp = decoded.getTimestamp();
                        imei = decoded.getImei();
                        locations.add(decoded);
                    } catch (IllegalArgumentException e) {
                        ERROR_LOGGER.warn(replace, e);
                    }
                } else if (replace.contains(CMD_CONTINUES_FEEDBACK)) {
                    try {
                        Location decoded = decodeBr00Message(replace);
                        timestamp.plusSeconds(10);

                        decoded.setTimestamp(timestamp);
                        decoded.setImei(imei);
                        locations.add(decoded);
                    } catch (IllegalArgumentException e) {
                        ERROR_LOGGER.warn(replace, e);
                    }
                }
            }
        }
        return locations;
    }

    /**
     *
     * @param channel
     * @return
     */
    private static Location decodeBp05Message(String channel) throws ParseException {
        if (null == channel) {
            throw new IllegalArgumentException("Channel value should not be null");
        }
        if (channel.isEmpty()) {
            throw new IllegalArgumentException("Channel value should not be empty");
        }
        if (channel.length() != 93) {
            throw new IllegalArgumentException("Not a BP05 message: BP05 message must contains 95 characters");
        }

        String lat = channel.substring(38, 47);
        String lng = channel.substring(48, 58);

        System.out.println(lat + " " + lng);
        List<Double> point = Arrays.asList(
                Double.valueOf(lat.substring(0, 2)) + Double.valueOf(lat.substring(2, lat.length())) / 60,
                Double.valueOf(lng.substring(0, 3)) + Double.valueOf(lng.substring(3, lng.length())) / 60
        );

        return Location.builder()
                .type(LOCATION_OK)
                .point(point)
                .timestamp(LocalDateTime.now())
                .imei(Long.parseLong(channel.substring(16, 31)))
                .speed(Double.valueOf(channel.substring(59, 64)))
                .heading(Double.valueOf(channel.substring(70, 76)))
                .build();
    }

    /**
     *
     * @param channel
     * @return
     */
    private static Location decodeBr00Message(String channel) throws ParseException {
        if (null == channel) {
            throw new IllegalArgumentException("Channel value should not be null");
        }
        if (channel.isEmpty()) {
            throw new IllegalArgumentException("Channel value should not be empty");
        }
        if (channel.length() != 78) {
            throw new IllegalArgumentException("Not a BR00 message: BR00 message must contains 78 characters");
        }

        String lat = channel.substring(23, 32);
        String lng = channel.substring(33, 43);

        List<Double> point = Arrays.asList(
                Double.valueOf(lat.substring(0, 2)) + Double.valueOf(lat.substring(2, lat.length())) / 60,
                Double.valueOf(lng.substring(0, 3)) + Double.valueOf(lng.substring(3, lng.length())) / 60
        );

        return Location.builder()
                .type(LOCATION_OK)
                .point(point)
                .timestamp(null)
                .imei(0)
                .speed(Double.valueOf(channel.substring(45, 50)))
                .heading(Double.valueOf(channel.substring(56, 62)))
                .build();
    }

}
