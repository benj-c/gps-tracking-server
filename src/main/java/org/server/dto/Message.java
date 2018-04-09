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
package org.server.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class Message<T extends Object> {

    private MessageType messageType;
    private String message;
    private String timestamp;
    private T payload;

    public enum MessageType {
        MSG_EXCEPTION, CRITICAL_SERVER_FAILURE, DEVICE_DOWN, SERVER_STARTUP
    }

    public static Message.Builder getBuilder() {
        return new Message.Builder();
    }

    public Message(MessageType messageType, String message, String timestamp, T payload) {
        this.messageType = messageType;
        this.message = message;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public Message() {
    }

    /**
     *
     * @param <T>
     */
    public static class Builder<T extends Object> {

        private MessageType messageType;
        private String message;
        private String timestamp;
        private T payload;

        public Builder type(MessageType type) {
            this.messageType = type;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder timestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder payload(T payload) {
            this.payload = payload;
            return this;
        }

        public Message build() {
            return new Message(messageType, message, timestamp, payload);
        }
    }

}
