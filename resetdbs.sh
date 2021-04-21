#!/bin/bash

echo "Clearing Event DB"
cd ./axonserver/AxonServer-4.4.8
rm -r ./data/

echo "Clearing Jama/Jira Cache and Tokens"
mysql -u c4s -p --execute="USE jamacache; DROP TABLE IF EXISTS CacheStatus;
DROP TABLE IF EXISTS JamaData;  DROP TABLE IF EXISTS JamaItems; DROP TABLE IF EXISTS MonitoredItemIds; USE jiracache; DROP TABLE IF EXISTS CacheStatus; DROP TABLE IF EXISTS JiraIssues; DROP TABLE IF EXISTS MonitoredKeys; USE token; DROP TABLE IF EXISTS TokenEntry;
DROP TABLE IF EXISTS SnapshotEventEntry;"

echo "Done"
