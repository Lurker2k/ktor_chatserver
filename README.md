# Ktor_chatserver

A server written in Kotlin to enable a chatting between a single person (e.g. admin of a website) and multiple users (e.g. visitors of a website).
The server is using a mysql database to save the userIDs of the connected users, infos added to a user and the messages sent between admin and a client.

There are multiple ways to connect the server with a database. The easiest way is to use the docker-compose file to start the server with a mysql database in a container,
for this method you need to change the database.env file and replace the placeholders with your data. 
The dockerfile shows, how the image, which will be pulled by the docker-compose is built.

The second way to connect to a database is, that you start the application with the needed parameters. 
The Parameters which will be needed to start this server are listed at the end of this readme.

## Functionality

When the server is started it is set up according to the provided parameters, which can be set with in the databaseEnvironment.env file when it is created with the
docker-compose file, which is the preffered method to create this server.
A list of the parameters is given later in the readme, or can be seen at the databaseEnvironment.env file.

The websocket connection to the server can be established as an admin or as a client.

When a client establishes a connection, the server checks if the client has sent a cookie identifying himself, if the client doesn't send a cookie named **ChatID**
the server creates a unique userID for the client, adds this userID to the database and adds the cookie in the response to the client before establishing
the upgrade of the http call to a websocket connection. When a client sends a cookie with a userID, the server checks if this userID is already in the database.
If the sent userID is in the database, the server sends the chatlog of this userID to the client. 

Connecting as an Admin to the server is secured by username and a password, which must be set when the server is started.
The admin can either connect to the server with a controlpanel, which is a desktop application and can be downloaded **here**, or you can create your own solution on how
to handle the chat as an admin using the API. When an admin is connected to the server he can activate or deactivate the chat, which is by default deactivated when no admin
is connected to the server to avoid users trying to write messages to the server when there is no one who receives the messages.
The admin can also request some data about the state of the server, this includes how many users are currently visitng the website and
how many users want to chat with an admin. This is separated, because users who haven't sent at least one message are count as unwilling to chat and though you can't
send messages to them. When interacting with an user, the admin receives the chatlog of the user, can add infos to the user
and can rename the user to recognize him when he returns, to receive the name instead of the userID from the server.

## Client API

When connecting as a client to the server the data sent is expected to be in the JSON format.

Every message has a key called **TYPE** which includes all other values.

The types are:

* ONLINE
* OFFLINE
* CHATLOG
* MESSAGE

When connecting to the server, the server sends either ONLINE or OFFLINE to the client communicating if the chat is enabled or disabled.
These two types are also sent, when the admin changes the status of the chat at the server.

CHATLOG is sent, when the client connects to the server and is recognized as a returning visitor, who already has chatted with the admin.
When the value of TYPE is CHATLOG, the Object contains another Object with the key **CHATLOG**.
The **CHATLOG** Object contains a JSONARRAY. Each Object in the JSONARRAY is an Object of the class Chatmessage. The class Chatmessage is defined as:

```
class ChatMessage(
    var nachrichtenID : Int,
    var gesendet_um : String = "",
    var user_gesendet : Boolean = false,
    var nachricht : String = ""
)
```

MESSAGE is used, when the server sends a message to the client or when the client sends a message to the server.
When the Type is MESSAGE, the JSON contains a Key called **MESSAGE** which contains a String with the sent message.

## Admin API

## Necessary parameter

To avoid the start of the server with default values for critical parameters like an admin password, some values are necessary to provide when started.
When these parameters aren't provided the server will throw exceptions and list which parameters are missing.

Necessary parameters are:

* ADMINUSER
* ADMINPASS
* DATABASE
* URL
* USER
* PASS

Adminuser and adminpass are the parameters which are needed, when you want to connect to the server as an admin.

Database is an url to connect to the server with user and password added behind the URL as GET Parameter.

e.g. jdbc:mysql://database:3306/chatserver?serverTimezone=MET;user=DatabaseUserPassword;passwort=DatabaseUserPassword

Alternative you can provied the URL separated from the username and the password as single parameters. When both are provided the Database Parameter takes priority.

The easiest way to provide these parameters is to change the values in the databaseEnvironment.env file and create the server with the docker-compose file.

When you are using the docker-compose file you can use the databaseEnvironment.env file to create your mysql database along with the server.
The mysql server also needs some parameters to start, which are also listed in the databaseEnvironment.env file. These parameters are

* MYSQL_USER
* MYSQL_PASSWORD
* MYSQL_ROOT_PASSWORD
* MYSQL_DATABASE

## Optional parameter

Optional Parameter for the server are :

* SALT
* BEHINDPROXY
* PORT

SALT defines the Salt, which will be used to hash the created userID, you can freely change this between starts of the server.

BEHINDPROXY defines if the server is behind a proxy, like NGINX. This is important for security reasons.

PORT defines the port on which the server will be started.

