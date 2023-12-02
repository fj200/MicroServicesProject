package com.eecs3311.profilemicroservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.qos.logback.core.net.server.Client;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import org.neo4j.driver.v1.exceptions.ClientException;
import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

import static org.neo4j.driver.v1.Values.parameters;

@Repository
public class ProfileDriverImpl implements ProfileDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitProfileDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.userName)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.password)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT nProfile.userName IS UNIQUE";
				trans.run(queryStr);

				trans.success();
			} catch (Exception e) {
				if (e.getMessage().contains("An equivalent constraint already exists")) {
					System.out.println("INFO: Profile constraints already exist (DB likely already initialized), should be OK to continue");
				} else {
					// something else, yuck, bye
					throw e;
				}
			}
			session.close();
		}
	}
	
	@Override
	public DbQueryStatus createUserProfile(String userName, String fullName, String password) {
        DbQueryStatus status = new DbQueryStatus("Error creating Profile",DbQueryExecResult.QUERY_ERROR_GENERIC);
        try (Session session = driver.session()) {
            String query = "create (p:profile{userName:$userName,fullName:$fullName, password:$password})-[r:created]->(pl:playlist{plName:$playName})";
            StatementResult result = session.writeTransaction(tx -> tx.run(query, parameters("userName", userName,"fullName",fullName, "password", password, "playName", userName+"-favorites")));
            status.setMessage(userName + " Profile and playlist created");
            status.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
            status.setData(result);
            session.close();
        }
        catch(ClientException e){ // user already exist
            status.setMessage(e.getMessage());
        }
        return status;
	}

	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
        DbQueryStatus status = new DbQueryStatus("Error following friend",DbQueryExecResult.QUERY_ERROR_GENERIC);
        try (Session session = driver.session()) {
            String query = "match (a:profile {userName: $userName}), (b:profile {userName: $frndUserName}) MERGE (a)-[r:follows]->(b)  RETURN r";
            StatementResult result = session.writeTransaction(tx -> tx.run(query, parameters("userName", userName,"frndUserName", frndUserName)));
            if (result.hasNext()) {
                status.setMessage(userName + " Follows " + frndUserName);
                status.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
            }
            else{
                status.setMessage(userName + " Cant Follow " + frndUserName+" or one of the users don't exists");
                status.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
            }
            status.setData(result);
            session.close();
        }
        catch(Exception e){
            status.setMessage(e.getMessage());
        }
		return status;
	}

	@Override
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
        DbQueryStatus status = new DbQueryStatus("Error unfollowing profile",DbQueryExecResult.QUERY_ERROR_GENERIC);
        try (Session session = driver.session()) {
            String query = "MATCH (a {userName: $userName})-[r:follows]->(b {userName: $frndUserName}) DELETE r;";
            StatementResult result = session.writeTransaction(tx -> tx.run(query, parameters("userName", userName,"frndUserName", frndUserName)));
            status.setMessage(userName + " unfollowed " + frndUserName);
            status.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
            session.close();
        }
        catch(Exception e){
            status.setMessage(e.getMessage());
        }
        return status;
	}

    public DbQueryStatus getAllSongFriendsLike(String userName) {
        DbQueryStatus status = new DbQueryStatus("Error fetching songs liked by friends", DbQueryExecResult.QUERY_ERROR_GENERIC);
        try (Session session = driver.session()) {
            String query = "MATCH (user:profile {userName: $userName})-[:friend]->(friend)-[:liked]->(song:Song) RETURN song";
            StatementResult result = session.writeTransaction(tx -> tx.run(query, parameters("userName", userName)));
            // Process the result and set the status accordingly
            List<Record> records = result.list();
            if (!records.isEmpty()) {
                status.setMessage("Songs liked by friends of " + userName + " retrieved successfully");
                status.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
                status.setData(records);
            } else {
                status.setMessage("No songs liked by friends of " + userName + " found");
            }
        } catch (ClientException e) {
            status.setMessage(e.getMessage());
        }
        return status;
    }
}
