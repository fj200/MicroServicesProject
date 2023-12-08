package com.eecs3311.profilemicroservice;

import org.neo4j.driver.v1.*;
import org.springframework.stereotype.Repository;

import static org.neo4j.driver.v1.Values.parameters;

@Repository
public class PlaylistDriverImpl implements PlaylistDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitPlaylistDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nPlaylist:playlist) ASSERT exists(nPlaylist.plName)";
				trans.run(queryStr);
				trans.success();
			} catch (Exception e) {
				if (e.getMessage().contains("An equivalent constraint already exists")) {
					System.out.println("INFO: Playlist constraint already exist (DB likely already initialized), should be OK to continue");
				} else {
					// something else, yuck, bye
					throw e;
				}
			}
			session.close();
		}
	}

	@Override
	public DbQueryStatus likeSong(String userName, String songId) {
        DbQueryStatus status = new DbQueryStatus("Error liking song",DbQueryExecResult.QUERY_ERROR_GENERIC);
        try (Session session = driver.session()) {
            String query = "MATCH (a:playlist {plName: $plName}) MERGE (b:song {songId: $songId})\n" +
                            "WITH a, b OPTIONAL MATCH (a)-[r:includes]->(b:song) \n" +
                            "WITH a, b, r MERGE (a)-[:includes]->(b)\n" +
                            "RETURN CASE WHEN r IS NOT NULL THEN \"Relationship already exists\" ELSE \"Relationship created\" END AS result;";
            StatementResult result = session.writeTransaction(tx -> tx.run(query, parameters("plName", userName+"-favorites","songId", songId)));
            if (result.hasNext()) {
                Record record = result.next();
                String message = (record.get(0)).asString();
                status.setMessage(message);
                status.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
            }
            else{
                status.setMessage(userName + " Cant like song User does not exists");
                status.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
            }
        }
        catch(Exception e){
            status.setMessage(e.getMessage());
        }
        return status;
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
        DbQueryStatus status = new DbQueryStatus("Error unliking song",DbQueryExecResult.QUERY_ERROR_GENERIC);
        try (Session session = driver.session()) {
            String query = "MATCH (a :playlist{plName: $plName})-[r:includes]->(b:song {songId: $songId}) with r DELETE r return r";
            StatementResult result = session.writeTransaction(tx -> tx.run(query, parameters("plName", userName+"-favorites","songId", songId)));

            if (result.hasNext()) {
                status.setMessage("Song unliked");
                status.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
            }
            else{
                status.setMessage("Relationship doesn't exists");
                status.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
            }
        }
        catch(Exception e){
            status.setMessage(e.getMessage());
        }
        return status;
	}

    @Override
    public DbQueryStatus deleteSong(String songId) {
        DbQueryStatus status = new DbQueryStatus("Error removing song",DbQueryExecResult.QUERY_ERROR_GENERIC);
        try (Session session = driver.session()) {
            String query = "MATCH (a :playlist)-[r:includes]->(b:song {songId: $songId}) DELETE r, b;";
            session.writeTransaction(tx -> tx.run(query, parameters("songId", songId)));
            status.setMessage("deleted " + songId);
            status.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
        }
        catch(Exception e){
            status.setMessage(e.getMessage());
        }
        return status;
    }
}
