/*
 * Copyright 2018 NULL.
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
import java.util.List;
import org.server.dto.Location;

/**
 *
 * @author NULL
 */
public interface BaseProtocolDecoder {

    /**
     *
     * @param req
     * @return
     */
    public boolean isValidRequest(String req);

    /**
     *
     * @param request
     * @return
     * @throws ParseException
     * @throws IllegalArgumentException
     */
    public List<Location> parseRequest(String request) throws
            ParseException,
            IllegalArgumentException;
}
