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
package org.server.db;

import org.server.connection.MongoConnection;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import java.io.IOException;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.bson.Document;
import org.server.Context;
import org.server.dto.Location;
import org.server.dto.Vehicle;

public class DBOperationsHandler implements DatabaseHandler {

    /**
     *
     * @param decodedRequest
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws MongoException
     */
    @Override
    public boolean insertCoodinates(Object decodedRequest) throws
            IOException,
            ClassNotFoundException,
            MongoException {
        if (Objects.isNull(decodedRequest)) {
            return Boolean.FALSE;
        }

        Location data = (Location) decodedRequest;
        Date timestamp = Date.from(data.getTimestamp().atZone(ZoneId.systemDefault()).toInstant());

        MongoClient mongoClient = MongoConnection.getMongoClient();
        MongoCollection<Document> locationCollection = mongoClient
                .getDatabase(Context.getSystemProperties().getDb().getName()).getCollection("location");
        Document document = new Document()
                .append("imei", data.getImei())
                .append("point", data.getPoint())
                .append("heading", data.getHeading())
                .append("speed", data.getSpeed())
                .append("timestamp", timestamp)
                .append("consecutive_point_distance", data.getDistance());
//        System.out.println(document.toJson());
        locationCollection.insertOne(document);
        return true;
    }

    /**
     *
     * @param imei
     * @return
     * @throws IOException
     * @throws java.lang.ClassNotFoundException
     * @throws MongoException
     */
    @Override
    public double[] findLastLocation(long imei) throws
            IOException,
            ClassNotFoundException,
            MongoException {
        double[] d = new double[2];
        MongoClient mongoClient = MongoConnection.getMongoClient();
        MongoCollection<Document> locationCollection = mongoClient
                .getDatabase(Context.getSystemProperties().getDb().getName()).getCollection("location");
        MongoCursor<Document> doc = locationCollection
                .find(new BasicDBObject().append("imei", imei))
                .sort(new BasicDBObject().append("_id", -1))
                .limit(1)
                .iterator();
        if (doc.hasNext()) {
            List<Double> point = (List<Double>) doc.next().get("point");
            d[0] = point.get(0);
            d[1] = point.get(1);
        } else {
            d[0] = 0.0;
            d[1] = 0.0;
        }

        return d;
    }

    /**
     *
     * @param imei
     * @return
     * @throws IOException
     * @throws MongoException
     * @throws java.lang.ClassNotFoundException
     */
    @Override
    public Vehicle getVehicle(long imei) throws
            IOException,
            ClassNotFoundException,
            MongoException {
        MongoClient mongoClient = MongoConnection.getMongoClient();
        MongoCollection<Document> locationCollection = mongoClient
                .getDatabase(Context.getSystemProperties().getDb().getName()).getCollection("user");
        MongoCursor<Document> doc = locationCollection
                .find(new BasicDBObject().append("vehicles.imei", imei))
                .iterator();
        Vehicle vehicle_ = null;

        while (doc.hasNext()) {
            Document next = doc.next();
            List<Document> vehicles = (List<Document>) next.get("vehicles");
            for (int i = 0; i < vehicles.size(); i++) {
                Document vehicle = vehicles.get(i);
                if ((vehicle.getLong("imei") == imei) && vehicle.getBoolean("active")) {
                    vehicle_ = new Vehicle(
                            next.getString("username"),
                            vehicle.getLong("imei"),
                            vehicle.getString("key"),
                            vehicle.getString("trackerSimNumber"),
                            vehicle.getString("numberPlate")
                    );
                }
            }
        }

        return vehicle_;
    }

    /**
     *
     * @return @throws IOException
     * @throws ClassNotFoundException
     * @throws MongoException
     */
    @Override
    public boolean retryDatabaseConnectivity() throws
            IOException,
            ClassNotFoundException,
            MongoException {
        return true;
    }
}
