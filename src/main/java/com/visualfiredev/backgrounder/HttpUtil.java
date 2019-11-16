package com.visualfiredev.backgrounder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class HttpUtil {
	
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36";
	
	public static HttpsURLConnection connect(String endpoint, String method, boolean json) throws IOException {
		HttpsURLConnection conn = (HttpsURLConnection) new URL(endpoint).openConnection();
		conn.setRequestMethod(method);
		conn.setRequestProperty("User-Agent", USER_AGENT);
		if (json) {
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Accept", "application/json");
		}
		conn.setDoOutput(true);
		return conn;
	}
	
	public static String parseOutput(HttpsURLConnection connection, int code) throws IOException {
		InputStream instream;
		if (code < 400) {
			instream = connection.getInputStream();
		} else {
			instream = connection.getErrorStream();
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(instream, "UTF-8"))) {
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line.trim());
			}
			return builder.toString();
		}
	}

}
