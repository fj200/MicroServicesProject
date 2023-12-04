package com.eecs3311.songmicroservice;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;

	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}

	@Override
	public DbQueryStatus addSong(Song songToAdd) {
        Song song;
        DbQueryStatus queryStatus;
        try{
            if(db.exists(Query.query(Criteria.where("songName").is(songToAdd.getSongName())), Song.class)){
                queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_GENERIC);
            }
            else {
                song = db.insert(songToAdd);
                if(song != null){
                    queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
                    queryStatus.setData(song);
                }
                else{
                    queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_GENERIC);
                }
            }
        }
        catch(Exception e){
            queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_GENERIC);
        }
        return queryStatus;
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
        Song song;
        DbQueryStatus queryStatus;
        try{
            song = db.findById(new ObjectId(songId), Song.class);
            if (song != null) {
                db.remove(song, String.valueOf(Song.class));
                queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
            }
            else {
                queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
            }
        }
        catch(Exception e){
            queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_GENERIC);
        }
        return queryStatus;
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		// TODO Auto-generated method stub
        Song song;
        DbQueryStatus queryStatus = null;
        try{
            song= db.findById(new ObjectId(songId),Song.class);
            if (song != null){
                if (shouldDecrement && song.getSongAmountFavourites()>0){

                    song.setSongAmountFavourites(song.getSongAmountFavourites()-1);
                    queryStatus.setData(song.getSongAmountFavourites());
                    queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
                }
                else if(!shouldDecrement) {
                    song.setSongAmountFavourites(song.getSongAmountFavourites()+1);
                    queryStatus.setData(song.getSongAmountFavourites());
                    queryStatus= new DbQueryStatus("",DbQueryExecResult.QUERY_OK);
                }
            }
            else {
                queryStatus = new DbQueryStatus("",DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
            }
        }
        catch(Exception e){
            queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_GENERIC);
        }
        return queryStatus;
	}

    @Override
    public DbQueryStatus getMostFavoritesSong(List<String> songIds) {
        DbQueryStatus queryStatus = null;
        MatchOperation matchOperation = Aggregation.match(Criteria.where("_id").in(songIds));
        // Sort by songAmountFavourites in descending order
        SortOperation sortByFavouritesDesc = Aggregation.sort(Sort.Direction.DESC,"songAmountFavourites");
        // Limit to 1 result (the one with the maximum songAmountFavourites)
        Aggregation aggregation = Aggregation.newAggregation(matchOperation,sortByFavouritesDesc, Aggregation.limit(1));
        AggregationResults<Song> result = db.aggregate(aggregation, "songs", Song.class);
        List<Song> songs = result.getMappedResults();
        System.out.println(songs);
        queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
        queryStatus.setData(songs.get(0).getSongName());

        return queryStatus;
    }
}
