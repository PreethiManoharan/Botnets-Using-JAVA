
# Botnets Using Java

In this project, MasterBot commands the SlaveBots to perform Distributed Denial-of-service (DDos) attack on the target servers using java multithreading and socket concept.

-----------------------------------------------------------------------------------

## To Run the program:

The master is started by using the following command:
java masterbot -p <port number>
Eg: masterbot -p 9000

The slavebot is connected to master by using the following command:
java slavebot -h localhost -p 9000
Eg: slavebot -h localhost -p 9000

------------------------------------------------------------------------------------

Once connected the master can provide commands to the slaves, as follows:
#### To list all the available slaves connected to the master:
1. list

#### To make the slave to have a connection to target host:
2. connect (IPAddressOrHostNameOfTheSlave | all) (TargetHostName | IPAddress) TargetPortNumber [NumberOfConnections: 1 if not specified]

*Examples:
-To generate single connection: connect all www.sjsu.edu 80
-To generate multiple connection: connect all www.sjsu.edu 80 2*

#### To make the slave disconnect from target host:
3. disconnect (IPAddressOrHostNameOfTheSlave | all) (TargetHostName | IPAddress) [TargetPort:all if no port specified]

*Examples:
-To disconnect all slaves: disconnect all www.sjsu.edu*

#### To make the connect command to support keepalive option:
4. connect (IPAddressOrHostNameOfTheSlave | all) (TargetHostName | IPAddress) TargetPortNumber [NumberOfConnections: 1 if not specified] [Keepalive]

*Examples:
-connect all www.sjsu.edu 80 Keepalive*

#### To support the url option where the slave can generate an random string and attach to the url. Also, to drop all replies:
5. connect (IPAddressOrHostNameOfTheSlave | all) (TargetHostName | IPAddress) TargetPortNumber [NumberOfConnections: 1 if not specified] [url = path to be provided to the web server]

*Examples:
-connect all www.sjsu.edu 80 2 url=/#q=
// This will generate the random string and attach to the url = www.sjsu.edu.*

#### "Rise of fake bots and fall of the page-rank alogorithm":
6. rise-fake-url [TargetPort] [URL]
*Examples: 
-rise-fake-url 8585 www.maliciouslink.com*

Upon receiving the rise-fake-url command, the bots:
* behave like web-servers at the specified TargetPort.
* serve virtual html page that links to two other virtual html pages.
* each page will contain a link to the fake url with text, "check this out!", which will make anybody looking at the link to think it is important.
* So, the crawler will see a binary tree of fake pages, each containing 10 links to the fake url.

7. down-fake-url [TargetPort] [URL]
*Upon receiving the down command, all fake websites will be brought down by the docile bots which will not even respond to connections on the given port.*






