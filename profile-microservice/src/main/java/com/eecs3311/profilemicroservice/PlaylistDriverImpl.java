package com.eecs3311.profilemicroservice;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

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
            String query = "match (a:playlist {plName: $plName}) merge (b:song {songId: $songId}) merge (a)-[r:includes]->(b)  RETURN r";
            StatementResult result = session.writeTransaction(tx -> tx.run(query, parameters("plName", userName+"-favorites","songId", songId)));
            if (result.hasNext()) {
                status.setMessage(userName + " likes " + songId);
                status.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
            }
            else{
                status.setMessage(userName + " Cant like song user don't exists");
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
	public DbQueryStatus unlikeSong(String userName, String songId) {
        DbQueryStatus status = new DbQueryStatus("Error unliking song",DbQueryExecResult.QUERY_ERROR_GENERIC);
        try (Session session = driver.session()) {
            String query = "MATCH (a :playlist{plName: $plName})-[r:includes]->(b:song {songId: $songId}) DELETE r;";
            StatementResult result = session.writeTransaction(tx -> tx.run(query, parameters("plName", userName+"-favorites","songId", songId)));
            status.setMessage(userName + " unliked " + songId);
            status.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
            session.close();
        }
        catch(Exception e){
            status.setMessage(e.getMessage());
        }
        return status;
	}
}
