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
package org.server.db;

import com.mongodb.MongoException;
import java.io.IOException;
import org.server.dto.Vehicle;

/**
 *
 * @author NULL
 */
public interface DatabaseHandler {

    /**
     *
     * @param decodedRequest
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws MongoException
     */
    public boolean insertCoodinates(Object decodedRequest) throws
            IOException,
            ClassNotFoundException,
            MongoException;

    /**
     *
     * @param imei
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws MongoException
     */
    public double[] findLastLocation(long imei) throws
            IOException,
            ClassNotFoundException,
            MongoException;

    /**
     *
     * @param imei
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws MongoException
     */
    public Vehicle getVehicle(long imei) throws
            IOException,
            ClassNotFoundException,
            MongoException;

    /**
     *
     * @return @throws IOException
     * @throws ClassNotFoundException
     * @throws MongoException
     */
    public boolean retryDatabaseConnectivity() throws
            IOException,
            ClassNotFoundException,
            MongoException;
}
