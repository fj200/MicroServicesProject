package com.eecs3311.songmicroservice;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;

	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}

	@Override
	public DbQueryStatus addSong(Song songToAdd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DbQueryStatus findSongById(String songId) {
		// TODO Auto-generated method stub
        Song song;
        DbQueryStatus queryStatus = null;

        try{
            song = db.findById(new ObjectId(songId), Song.class);
            if (song != null){
                queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
                queryStatus.setData(song);
            } else if (song == null) {
                queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
            }
        }
        catch (Exception e){
            queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_GENERIC);
        }
        return queryStatus;
    }

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		// TODO Auto-generated method stub
        Song song;
        DbQueryStatus queryStatus = null;

        try{
            song = db.findById(new ObjectId(songId), Song.class);
            if (song != null){
                queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
                queryStatus.setData(song.getSongName());
            } else if (song == null) {
                queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
            }
        }
        catch (Exception e){
            queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_GENERIC);
        }
        return queryStatus;
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		// TODO Auto-generated method stub
		return null;
	}
}
