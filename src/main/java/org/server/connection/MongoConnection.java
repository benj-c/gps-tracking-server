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
package org.server.connection;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import java.io.IOException;
import org.server.Context;
import org.server.dto.properties.DatabaseProperties;

/**
 *
 * @author NULL
 */
public class MongoConnection {

    private static MongoClient MONGO_CLIENT;
    private static DatabaseProperties db;

    public static DatabaseProperties getDb() {
        return db;
    }

    private MongoConnection() {
    }

    /**
     *
     * @return Returns a singleton instance of mongo client
     * @throws java.io.IOException
     */
    public static synchronized MongoClient getMongoClient() throws IOException {
        if (null == MONGO_CLIENT) {
            db = Context.getSystemProperties().getDb();
            MONGO_CLIENT = new MongoClient(new MongoClientURI(getDb().getServerAddress()));
            MongoCredential.createCredential(getDb().getUser(), getDb().getName(), getDb().getPasswd().toCharArray());
        }
        return MONGO_CLIENT;
    }

}
