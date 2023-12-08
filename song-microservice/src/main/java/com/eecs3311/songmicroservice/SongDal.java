package com.eecs3311.songmicroservice;

import java.util.List;

public interface SongDal {
	DbQueryStatus addSong(Song songToAdd);
	DbQueryStatus findSongById(String songId);
	DbQueryStatus getSongTitleById(String songId);
	DbQueryStatus deleteSongById(String songId);	
	DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement);
    DbQueryStatus getMostFavoriteSong(List<String> params);
}
