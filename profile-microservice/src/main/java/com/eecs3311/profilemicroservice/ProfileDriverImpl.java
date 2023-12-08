package com.eecs3311.profilemicroservice;

import java.util.*;

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

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
        // MATCH (user:profile {userName: $userName})-[:follows]-(friend)-[:created]-(playlist)-[:includes]->(songs) RETURN friend.userName, songs.songId
        DbQueryStatus status = new DbQueryStatus("Error fetching songs liked by friends", DbQueryExecResult.QUERY_ERROR_GENERIC);
        try (Session session = driver.session()) {
            String query =  "MATCH (user:profile {userName: $userName})-[:follows]->(friend)\n" +
                            "OPTIONAL MATCH (friend)-[:created]->(playlist)-[:includes]->(songs) \n" +
                            "RETURN friend.userName, COALESCE(songs.songId, \"NSF\") AS songId;";
            StatementResult result = session.writeTransaction(tx -> tx.run(query, parameters("userName", userName)));
            // Process the result and set the status accordingly

            HashMap<String, List<String>> friendsSong = new HashMap<>();
            while (result.hasNext()) {
                Record record = result.next();
                String friendName = record.get(0).asString();
                String songId = (record.get(1)).asString();
                friendsSong.putIfAbsent(friendName, new ArrayList<>());
                //NSF no song found
                if(!songId.equals("NSF"))friendsSong.get(friendName).add(songId);
//                System.out.println(friendName +" "+ songId);
            }
//            System.out.println(friendsSong);
            status.setData(friendsSong);
            status.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
            if (!friendsSong.isEmpty()) {
                status.setMessage("Songs liked by friends of " + userName + " retrieved successfully");
            } else {
                status.setMessage("No songs liked by friends of " + userName + " found");
            }

        } catch (ClientException e) {
            status.setMessage(e.getMessage());
        }
        return status;
	}

    @Override
    public DbQueryStatus getSongsInPlayList(String userName) {
        DbQueryStatus status = new DbQueryStatus("Error fetching songs liked by user", DbQueryExecResult.QUERY_ERROR_GENERIC);
        try (Session session = driver.session()) {
            String query = "MATCH (p:playlist {plName: $plName})-[:includes]->(s:song) return (s.songId)";
            StatementResult result = session.writeTransaction(tx -> tx.run(query, parameters("plName", userName+"-favorites")));
            // Process the result and set the status accordingly
            List<String> songs = new ArrayList<>();

            while (result.hasNext()) {
                Record record = result.next();
                String songId = (record.get(0)).asString();
                songs.add(songId);
            }
            status.setData(songs);
            if (!songs.isEmpty()) {
                status.setMessage("Songs liked by " + userName + " retrieved successfully");
                status.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
            } else {
                status.setMessage("No songs liked by " + userName + " found");
            }
        } catch (ClientException e) {
            System.out.println("error "+e.getMessage());
            status.setMessage(e.getMessage());
        }
        return status;
    }

    @Override
    public DbQueryStatus blend(String userName) {
        DbQueryStatus status = new DbQueryStatus("Error making playlist",DbQueryExecResult.QUERY_ERROR_GENERIC);
        try (Session session = driver.session()){
            String query = "MATCH (a:playlist {plName: $plName})-[:includes]->(song:song) RETURN song.songId AS result" +
                    " UNION " +
                    "MATCH (user:profile {userName: $userName})-[:follows]->(friend)-[:created]->(playlist)-[:includes]->(songs:song) RETURN songs.songId AS result";
            StatementResult result = session.writeTransaction(tx -> tx.run(query,parameters("plName", userName+"-favorites","userName", userName)));
            status.setMessage(userName+ " and blend playlist created");
            status.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
            session.close();
            List<String> songIds = new ArrayList<>();

            while (result.hasNext()) {
                Record record = result.next();
                String songId = (record.get(0)).asString();
                songIds.add(songId);
            }
            status.setData(songIds);
        }
        catch (Exception e){
            status.setMessage(e.getMessage());
        }
        return status;
    }
}
