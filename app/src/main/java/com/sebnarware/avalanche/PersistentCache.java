package com.sebnarware.avalanche;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONArray;

import android.util.Log;

public class PersistentCache {
	
    private static final String TAG = "PersistentCache";

	public void putJsonArray(JSONArray jsonArray, String key) {
		try {
			File file = MainActivity.getMainActivity().getFileStreamPath(key);
			String contents = jsonArray.toString();
			writeTextFile(file, contents);
			Log.i(TAG, "putJsonArray success; key: " + key);
		} catch (Exception e) {
			// I/O error or other issue...
			Log.w(TAG, "putJsonArray error: " + e);
		}
	}

	public JSONArray getJsonArray(String key) {
		
		JSONArray jsonArray = null;
		
		try {
			File file = MainActivity.getMainActivity().getFileStreamPath(key);
			if (file.exists()) {
				String contents = readTextFile(file);
				jsonArray = new JSONArray(contents);
				Log.i(TAG, "getJsonArray cache hit; key: " + key);
			} else {
				Log.i(TAG, "getJsonArray cache miss; key: " + key);				
			}
		} catch (Exception e) {
			// I/O error, JSON didn't parse, or other issue...
			Log.w(TAG, "getJsonArray error: " + e);
		}

		return jsonArray;
	}
	
    private String readTextFile(File file) throws IOException {
        FileReader in = new FileReader(file);
        StringBuilder contents = new StringBuilder();
        char[] buffer = new char[4096];
        int read = 0;
        do {
            contents.append(buffer, 0, read);
            read = in.read(buffer);
        } while (read >= 0);
        in.close();
        return contents.toString();
    }
	
    private void writeTextFile(File file, String contents) throws IOException {
        FileWriter out = new FileWriter(file);
        out.write(contents);
        out.close();
    }

}
