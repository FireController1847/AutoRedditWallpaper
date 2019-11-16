package com.visualfiredev.backgrounder;

import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

public class ImageMeta {
	
	private int width;
	private int height;
	private URL url;
	
	public ImageMeta(JSONObject source) throws MalformedURLException, JSONException {
		width = source.getInt("width");
		height = source.getInt("height");
		url = new URL(source.getString("url").replace("amp;", ""));
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public URL getUrl() {
		return url;
	}
	
	@Override
	public String toString() {
		return String.format("Image{width=%s,height=%s,url=%s}", width, height, url);
	}

}
