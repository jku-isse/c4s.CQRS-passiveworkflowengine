#!/bin/bash

echo "Clearing Event DB"
cd ./axonserver/AxonServer-4.4.8
rm -r ./data/

echo "Clearing Token"

mysql -u c4s -p --execute="USE token; DROP TABLE IF EXISTS TokenEntry;
DROP TABLE IF EXISTS SnapshotEventEntry; "

echo "Done"
