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

When connecting as a client to the server the data sent is expected to be in the **JSON** format.

Every message has a key called **TYPE** which defines what type is sent.

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

**The classes are converted to JSON with the library GSON and the function toJsonTree**

MESSAGE is used, when the server sends a message to the client or when the client sends a message to the server.
When the Type is MESSAGE, the JSON contains a Key called **MESSAGE** which contains a String with the sent message.

## Admin API

The admin API is defined by data in the JSON format. 

the admin API can be split in data sent to the admin by the server and data sent to the server by the admin.

### Admin to server

Every message from the admin contains a key named **TYPE** which defines how the server handles the rest of the data sent. The types which can be sent are

* ONLINE
* OFFLINE
* SOCKETID
* RENAMEUSER
* ADDUSERINFO
* REMOVEINFO
* MESSAGE

- ONLINE and OFFLINE are sent, when the admin enables or disables the chat. These messages set a value at the server and are forwarded to all connected clients
through the client API

- SOCKETID contains an additional key of **SOCKETID** with a value of the selected userID. This tells the server, that the admin wants to retrieve all infos and the chatlog of
the user with this userID. The server will retrieve these data from the database and send them to the admin. 

- RENAMEUSER an Object with this type contains two values. One value has the key **SOCKETID** which defines, for which user the name will be changed in the database.
The other value has the key **RENAMEUSER** and contains the new name for the user.

- ADDUSERINFO This type has two additional values. One value has the key **SOCKETID** which contains the userID and the other value has the key **ADDUSERINFO** which contains
the info, which shall be added to the user as a String.

- REMOVEINFO contains the keys **SOCKETID** and **REMOVEINFO**. REMOVEINFO contains a value of the type Int which is the id of the info added to the user in the database, which
will be deleted.

- MESSAGE contains the keys **SOCKETID** and **MESSAGE**. MESSAGE contains a String which will be sent to the user and added to the database.

### Server to admin

Messages sent to the admin from the server are sent in the JSON format. The messages are designed to work with the controlpanel designed for the connection to this server
as admin and contain at least 2 values. One value has the key  **TYPE** which defines what shall be done with the data and another value has the key **VIEW** which
tells the controlpanel at which screen this shall be handled. **VIEW** can have two values **CHATSCREEN** and **MAINSCREEN**. 

The values of **TYPE** for **MAINSCREEN** are

* ONLINE
* OFFLINE

ONLINE and OFFLINE are sent to the admin, when the admin has successfully enabled or disabled the chat.

The values of **TYPE** for **CHATSCREEN** are

* CHATUSERS
* ADDCONNECTEDUSER
* REMOVECONNECTEDUSER
* ADDCHATUSER
* REMOVECHATUSER
* CHATLOG
* ADDUSERINFO
* GETUSERINFO
* APPENDMESSAGE
* NEWMESSAGE

CHATUSERS sends the data of all currently connected users who are willing to chat and the amount of all users connected to the admin. 
The data sent to the admin contain a key **CONNECTEDUSER** which holds the current amount of connected users as Int. Another key is
**CHATUSERS** which contains a JSONARRAY of objects of the class ChatUser which is defined as followed:

```
data class ChatUser(
    var socketID : String = "",
    var loginTime : String = "",
    var name : String? = null,
    var neueNachrichten : Int = 0
)
```

**The classes are converted to JSON with the library GSON and the function toJsonTree**

ADDCONNECTEDUSER and REMOVECONNECTEDUSER have no additional values and are used to tell the admin, that either one user recently connected or disconnected from the server
and the current usercount shall be increased or decreased by one.

ADDCHATUSER contains a key **ADDCHATUSER** which contains an object of the class ChatUser

REMOVECHATUSER contains a key **SOCKETID** which contains the userID of the user who disconnected from the server

CHATLOG contains a key **CHATLOG** which has a JSONARRAY of Chatmessage classes as value

ADDUSERINFO has a key **ADDUSERINFO** which contains an object of the class UserInfo which is defined as followed: 

```
data class UserInfo (
val info : String,
val infoID : Int
)
```

**The classes are converted to JSON with the library GSON and the function toJsonTree**

GETUSERINFO has a key **GETUSERINFO** which contains a JSONARRAY of UserInfo classes

APPENDMESSAGE has a key **APPENDMESSAGE** which contains an object of the class ChatMessage

NEWMESSAGE has no additional value and is sent when a user has sent a message to the admin. 



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

