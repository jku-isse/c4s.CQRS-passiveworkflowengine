#!/bin/bash

echo "Clearing Jama and Jira Cache"
mysql -u c4s -p --execute="USE jamacache; DROP TABLE IF EXISTS CacheStatus;
DROP TABLE IF EXISTS JamaData;  DROP TABLE IF EXISTS JamaItems; DROP TABLE IF EXISTS MonitoredItemIds; USE jiracache; DROP TABLE IF EXISTS CacheStatus; DROP TABLE IF EXISTS JiraIssues; DROP TABLE IF EXISTS MonitoredKeys;"

echo "Done"


