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
package org.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.server.dto.properties.SystemProperties;
import org.yaml.snakeyaml.Yaml;

public class Context {

    private static final String BASE_PATH = "/opt/gps";
    private static final String SYSTEM_PROPERTIES_PATH = BASE_PATH + "/config/system.yml";
    private static final String LOGGER_CONFIG_FILE = BASE_PATH + "/config/log4j.properties";
    private static final String ALITA_SMILE = BASE_PATH + "/files/alita-cute.jpg";
    private static final String ALITA_SCARED = BASE_PATH + "/files/alita-scared.jpg";

    public static final String ALITA_FACE_SMILE = "sm";
    public static final String ALITA_FACE_SCARED = "sc";

    private static final Logger EXCEPTION_LOGGER = LogManager.getLogger("ExceptionLog");

    private static SystemProperties SYSTEM_PROPERTIES;

    private Context() {
    }

    /**
     *
     * @return @throws IOException
     */
    public static SystemProperties getSystemProperties() throws IOException {
        if (null == SYSTEM_PROPERTIES) {
            try (InputStream in = Files.newInputStream(Paths.get(SYSTEM_PROPERTIES_PATH))) {
                SYSTEM_PROPERTIES = new Yaml().loadAs(in, SystemProperties.class);
            }
        }
        return SYSTEM_PROPERTIES;
    }

    /**
     *
     * @return
     */
    public static File getAlitaSmile() {
        return Paths.get(ALITA_SMILE).toFile();
    }

    /**
     *
     * @return
     */
    public static File getAlitaScared() {
        return Paths.get(ALITA_SCARED).toFile();
    }

    /**
     *
     */
    public static void configureLogger() {
        PropertyConfigurator.configure(LOGGER_CONFIG_FILE);
    }

    /**
     *
     */
    public static class ExceptionLogger {

        private static final String STR_TIMESTAMP = "timestamp:";
        private static final String STR_CLASS = ", Class:";
        private static final String STR_MESSAGE = ", Message:";

        private ExceptionLogger() {
        }

        /**
         *
         * @param exception
         * @param c
         * @param timestamp
         */
        public static synchronized void debug(Throwable exception, Class c, String timestamp) {
            EXCEPTION_LOGGER.debug(new StringBuilder()
                    .append(STR_TIMESTAMP).append(timestamp)
                    .append(STR_CLASS).append(c.getName())
                    .append(STR_MESSAGE).append(exception.getMessage()).toString(),
                    exception
            );
        }

        /**
         *
         * @param exception
         * @param c
         * @param timestamp
         */
        public static synchronized void error(Throwable exception, Class c, String timestamp) {
            EXCEPTION_LOGGER.error(new StringBuilder()
                    .append(STR_TIMESTAMP).append(timestamp)
                    .append(STR_CLASS).append(c.getName())
                    .append(STR_MESSAGE).append(exception.getMessage()).toString(),
                    exception
            );
        }

        /**
         *
         * @param exception
         * @param c
         * @param timestamp
         */
        public static synchronized void info(Throwable exception, Class c, String timestamp) {
            EXCEPTION_LOGGER.info(new StringBuilder()
                    .append(STR_TIMESTAMP).append(timestamp)
                    .append(STR_CLASS).append(c.getName())
                    .append(STR_MESSAGE).append(exception.getMessage()).toString(),
                    exception
            );
        }
    }
}
