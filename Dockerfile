FROM openjdk:8-jre-alpine

COPY ./build/libs/websitechat.jar /app/websitechat.jar
WORKDIR /app

EXPOSE 1300

#URL of the database to which you want to connect, without any arguments like user or pass
ENV URL error
#The user as whom you want to connect to the database
ENV USER error
#The password of the user as whom you want to connect to the database
ENV PASS error

#The whole URL of a connection to a database with user and pass as arguments appended
ENV DATABASE error

#You can chose wether you want to provide the DATABASE variable or the 3 single variables
#If you provide both, the DATABASE variable gets prioritized

#The username as who an admin can connect to the server
ENV ADMINUSER error
#The password of the user as whom the admin can connect to the server
ENV ADMINPASS error

#The Salt which shall be used to hash the SocketIDs
ENV SALT ''

#Is the server behind a proxy? This enables or disables headerforwarding
ENV BEHINDPROXY true

#The port on which the server shall be started
ENV PORT 1300

CMD java -server -jar websitechat.jar  -P:DATABASE=$DATABASE -P:URL=$URL -P:PASS=$PASS -P:USER=$USER -P:ADMINUSER=$ADMINUSER -P:ADMINPASS=$ADMINPASS -P:SALT=$SALT -P:BEHINDPROXY=$BEHINDPROXY -port=$PORT