# gps-tracking-server
Java and MongoDB based GPS object tracking server (TCP). This server can be used to handle TCP requests coming from GPS devices compatible with TK103 GPS data communication protocol.

###### Functionalities
 - Read incoming request and parse data stream.
 - Handle handshake signals and continues feedback requests.
 - Process location requests.
 - Find last known location within 30 seconds of time period and calculate distance between those two coordinates.
 - Insert processed coordinate to database.
 - Publish the coordinate to topic *COO.* ActiveMQ broker for live streaming, another service may subscribe to message topic */topic/COO.* to retrieve data.
 - Publish message to *REQSTATUS.* topic (for analytics purposes).
 - Sends periodic reports about system status to given emails.
 - Sends periodic reports to given emails to notify recent offline devices.
 
 (In order to make it work, ActiveMQ message publishing and email service must be enabled in *system.yml*)

###### Support
 - Supports for TK103 protocol.
 - Netty 4.0
 - MongoDB 3.6 as database.
 - ActiveMQ 5.14.5 as messaging server.
 
###### Installation
 - Maven clean and build project.
 - Copy *gps* folder to */opt* directory.
 - Update *system.yml* file.
 - Run jar file named *GpsServer2-2.0.0*.
 - Additionally it is possible to use *ubuntu upstart* config file to run as background service, for that,
   - Rename *GpsServer2-2.0.0-jar-with-dependencies.jar* into *GpsServer.jar*.
   - Place this jar file on */opt* directory.
   - Place *gpsserver.conf* file on */etc/init* directiry, then start the service using *service gpsserver start*.
   - You can change this settings.
