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
package org.server.parsers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import org.server.dto.Location;
import org.server.util.TimezoneUtil;

public final class Tk103Parser {

    public static final String CMD_LOGIN = "BP05";
    public static final String CMD_HANDSHAKE_SIGNAL = "BP00";
    public static final String CMD_CONTINUES_FEEDBACK = "BR00";

    public static final String CMD_LOGIN_RESPONSE = "AP05";
    public static final String CMD_HANDSHAKE_SIGNAL_RESPONSE = "AP01HSO";

    public static final int LOCATION_OK = 1;
    public static final int LOCATION_UNAVAILABLE = 0;
    public static final int LOCATION_UNDEFINED = -1;
    public static final int DEVICE_OFFLINE = -2;
    public static final int INVALID_REQUEST = -3;

    private static final int[] DEVICE_ID_POSITION = {1, 13};
    private static final int[] COMMAND_POSITION = {13, 17};
    private static final int[] IMEI_POSITION = {17, 32};
    private static final int[] LATITUDE_POSITION = {39, 48};
    private static final int[] LONGITUDE_POSITION = {49, 59};
    private static final int[] SPEED_POSITION = {60, 65};
    private static final int[] HEADING_POSITION = {71, 77};

    /**
     *
     * @param req
     * @return
     */
    public static final boolean isValidRequest(String req) {
        if (req.contains("V")) {
            return false;
        }
        if (req.contains("{")) {
            return false;
        }
        if (req.contains("}")) {
            return false;
        }
        if (req.contains(",,")) {
            return false;
        }

        return true;
    }

    /**
     *
     * @param request
     * @return
     * @throws java.text.ParseException
     */
    public static Location parseRequest(String request) throws ParseException {
        if (request == null || request.length() <= 1) {
            return Location.builder().type(INVALID_REQUEST).build();
        }
        if (request.contains("V")) {
            return Location.builder().type(LOCATION_UNAVAILABLE).build();
        }
        if (request.contains("{") || request.contains("}")) {
            return Location.builder().type(LOCATION_UNDEFINED).build();
        }
        if (request.contains(",,")) {
            return Location.builder().type(LOCATION_UNDEFINED).build();
        }

        return decode(request);
    }

    /**
     *
     * @param channel
     * @return
     */
    private static Location decode(String channel) throws ParseException {
        String lat = channel.substring(LATITUDE_POSITION[0], LATITUDE_POSITION[1]);
        String lng = channel.substring(LONGITUDE_POSITION[0], LONGITUDE_POSITION[1]);

        List<Double> point = Arrays.asList(
                Double.valueOf(lat.substring(0, 2)) + Double.valueOf(lat.substring(2, lat.length())) / 60,
                Double.valueOf(lng.substring(0, 3)) + Double.valueOf(lng.substring(3, lng.length())) / 60
        );

        return Location.builder()
                .type(LOCATION_OK)
                .point(point)
                .timestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(TimezoneUtil.getGmtTime("Asia/Colombo")))
                .imei(Long.parseLong(channel.substring(IMEI_POSITION[0], IMEI_POSITION[1])))
                .speed(Double.valueOf(channel.substring(SPEED_POSITION[0], SPEED_POSITION[1])))
                .heading(Double.valueOf(channel.substring(HEADING_POSITION[0], HEADING_POSITION[1])))
                .build();
    }

}
