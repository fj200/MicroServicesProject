#!/bin/bash

echo "----------------------------------------------------------------------------------------------------"
echo "****************************************************************************************************"
echo "----------------------------------------------------------------------------------------------------"

echo "-----BEGIN IMPORTING TEST DATA-----"

[ -d "song_db_backup" ] || mkdir "song_db_backup"

echo "-----Backing up existing EECS3311 DB-----"
mongodump -d eecs3311-test -o ./"song_db_backup"/`date +%Y-%m-%d_%H-%M-%S` || (echo "[ERROR] Could not create DB backup" && exit 1)
echo "-----Deleting existing EECS3311 DB-----" 
# if the below command doesn't work, replace with mongo. However, mongo should be missing from 7.0, replaced with mongosh
mongosh eecs3311-test --eval "db.dropDatabase()" || (echo "[ERROR] Could not delete EECS3311 DB" && exit 1)
echo "-----Importing test data from ./MOCK_DATA to EECS3311 DB-----"
mongoimport --db eecs3311-test --jsonArray --collection songs --legacy "./MOCK_DATA.json" || (echo "[ERROR] Could not import test data" && exit 1)

echo "-----END IMPORTING TEST DATA-----"