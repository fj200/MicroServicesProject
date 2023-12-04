package com.eecs3311.profilemicroservice;

public interface PlaylistDriver {
	DbQueryStatus likeSong(String userName, String songId);
	DbQueryStatus unlikeSong(String userName, String songId);
	DbQueryStatus blend(String userName, String frndUserName);
}