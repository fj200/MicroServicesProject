package com.eecs3311.songmicroservice;

import okhttp3.Call;
import okhttp3.Request;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping
public class SongController {

	@Autowired
	private final SongDal songDal;

	private OkHttpClient client = new OkHttpClient();


	public SongController(SongDal songDal) {
		this.songDal = songDal;
	}

	/**
	 * This method is partially implemented for you to follow as an example of
	 * how to complete the implementations of methods in the controller classes.
	 * @param songId
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/getSongById/{songId}", method = RequestMethod.GET)
	public ResponseEntity<Map<String, Object>> getSongById(@PathVariable("songId") String songId, HttpServletRequest request) {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			response.put("path", String.format("GET %s", Utils.getUrl(request)));
			DbQueryStatus dbQueryStatus = songDal.findSongById(songId);
			response.put("message", dbQueryStatus.getMessage());
			return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		} catch (Exception e) {
			response.put("error", "An error occurred while processing the request");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}



	@RequestMapping(value = "/getSongTitleById/{songId}", method = RequestMethod.GET)
	public ResponseEntity<Map<String, Object>> getSongTitleById(@PathVariable("songId") String songId, HttpServletRequest request) {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			response.put("path", String.format("GET %s", Utils.getUrl(request)));
			// TODO: add any other values to the map following the example in getSongById
			DbQueryStatus dbQueryStatus = songDal.getSongTitleById(songId);

			return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		} catch (Exception e) {
			response.put("error", "An error occurred while processing the request");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@RequestMapping(value = "/deleteSongById/{songId}", method = RequestMethod.DELETE)
	public ResponseEntity<Map<String, Object>> deleteSongById(@PathVariable("songId") String songId, HttpServletRequest request) {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			response.put("path", String.format("DELETE %s", Utils.getUrl(request)));
			// TODO: add any other values to the map following the example in getSongById
			DbQueryStatus dbQueryStatus = songDal.deleteSongById(songId);

            if(DbQueryExecResult.QUERY_OK.equals(dbQueryStatus.getdbQueryExecResult()) ){
                Request req = new Request.Builder()
                        .url("http://localhost:3002/deleteSong/"+songId)
                        .delete()
                        .build();

                Call call = client.newCall(req);
                String res = call.execute().body().string();
                JSONObject jsonResponse = new JSONObject(res);
                System.out.println(res);
                response.put("data", jsonResponse);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else {
                return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
            }
		} catch (Exception e) {
			response.put("error", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@RequestMapping(value = "/addSong", method = RequestMethod.POST)
	public ResponseEntity<Map<String, Object>> addSong(@RequestBody Map<String, String> params, HttpServletRequest request) {
		Map<String, Object> response = new HashMap<>();
		response.put("path", String.format("POST %s", Utils.getUrl(request)));
		try {
			String songName = params.get("songName");
			String songArtistFullName = params.get("songArtistFullName");
			String songAlbum = params.get("songAlbum");
			if (songName == null || songArtistFullName == null || songAlbum == null) {
				response.put("error", "Missing required parameters");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}
			Song newSong = new Song(songName, songArtistFullName, songAlbum);
			DbQueryStatus savedSong = songDal.addSong(newSong);
			if (savedSong.getdbQueryExecResult().equals(DbQueryExecResult.QUERY_OK)) {
				response.put("message", "Song added successfully");
				return Utils.setResponseStatus(response, savedSong.getdbQueryExecResult(), savedSong.getData());
			} else {
				response.put("error", "Failed to add the song to the database");
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
			}
		} catch (Exception e) {
			response.put("error", "Internal Server Error");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}



	@RequestMapping(value = "/updateSongFavouritesCount", method = RequestMethod.PUT)
	public ResponseEntity<Map<String, Object>> updateFavouritesCount(@RequestBody Map<String, String> params, HttpServletRequest request) {
		Map<String, Object> response = new HashMap<>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		try {
			String songId = params.get("songId");
			String shouldDecrementStr = params.get("shouldDecrement");
			if (songId == null || shouldDecrementStr == null) {
				response.put("error", "Missing required parameters");
				return  ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}
			boolean shouldDecrement = Boolean.parseBoolean(shouldDecrementStr);
			DbQueryStatus updateStatus = songDal.updateSongFavouritesCount(songId, shouldDecrement);
            System.out.println("error is not from the database command");
            System.out.println(updateStatus.getData()+" "+ updateStatus.getdbQueryExecResult());
			if (updateStatus.getdbQueryExecResult().equals(DbQueryExecResult.QUERY_OK)) {
				response.put("message", "Song favorites count updated successfully");
                System.out.println("eroror is query ok");
				return Utils.setResponseStatus(response, updateStatus.getdbQueryExecResult(), updateStatus.getData());
			}
			else if (updateStatus.getdbQueryExecResult().equals(DbQueryExecResult.QUERY_ERROR_NOT_FOUND)) {
				response.put("error", "Song not found");
                System.out.println("eroror is query not found");

            }
			else {
				response.put("error", "Failed to update song favorites count");
                System.out.println("eroror is query kill me");

            }
            System.out.println("eroror is query second not found i dont understnat");

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
		}
		catch (Exception e) {
			response.put("error", "Internal Server Error");
			return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}


    @RequestMapping(value = "/getMostFavoritesSong", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> getMostFavoritesSong(@RequestBody List<String> params, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("path", String.format("PUT %s", Utils.getUrl(request)));
        try {
            DbQueryStatus updateStatus = songDal.getMostFavoriteSong(params);
            if (updateStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK) {
                response.put("message", "Song favorites count updated successfully");
                return Utils.setResponseStatus(response, updateStatus.getdbQueryExecResult(), updateStatus.getData());
            }
            else if (updateStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_ERROR_NOT_FOUND) {
                response.put("error", "Song not found");
            }
            else {
                response.put("error", "Failed to update song favorites count");
            }
            return Utils.setResponseStatus(response, updateStatus.getdbQueryExecResult(), null);
        }
        catch (Exception e) {
            response.put("error", "Internal Server Error");
            return Utils.setResponseStatus(response, null, null);
        }
    }
}
