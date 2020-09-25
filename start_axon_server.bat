ECHO reset token
cmd < reset_token.txt

ECHO start axon server
cd ../AxonServer
rmdir /s "./data"
java -jar axonserver-4.2.4.jar

