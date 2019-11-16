package com.visualfiredev.backgrounder;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;

public class Backgrounder {

	private static final String API_URL = "https://api.reddit.com/";
	private static final boolean DEBUG_MODE = false;

	public static void main(String[] args) throws Exception {
		try {
			// Build Endpoint
			final String subreddit = "EarthPorn";
			final SortType sort = SortType.TOP;
			final TimeType time = TimeType.HOUR;
			final int limit = 20;
			final String endpoint = String.format("%sr/%s/%s?limit=%s&t=%s", API_URL, subreddit,
					sort.toString().toLowerCase(), limit, time.toString().toLowerCase());
			debug(String.format("endpoint=%s", endpoint));

			// Settings
			final double upscaleFactorNumerator = 4.0;
			final double upscaleFactorDenominator = 3.0;
			final double upscaleFactor = upscaleFactorNumerator / upscaleFactorDenominator;
			final int screenWidth = 1920;
			final int screenHeight = 1080;
			final int minWidth = (int) Math.round(screenWidth * upscaleFactor);
			final int minHeight = (int) Math.round(screenHeight * upscaleFactor);
			int ignore = 2;

			// Make HTTP Request
			System.out.println("Searching reddit...");
			HttpClient client = HttpClient.newBuilder().build();
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).GET().build();
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			debug(String.format("response=%s", response.body()));
			ImageMeta[] images = parseApiResponse(response.body());
			debug(String.format("json=%s", Arrays.toString(images)));
			int selectedIndex = -1;

			// Find Appropriately Sized Image
			System.out.println("Found " + images.length + " images.");
			System.out.println("Parsing images...");
			for (int i = 0; i < images.length; i++) {
				if (images[i].getWidth() >= minWidth && images[i].getHeight() >= minHeight) {
					if (ignore > 0) {
						ignore--;
						continue;
					}
					selectedIndex = i;
					break;
				}
			}
			if (selectedIndex == -1) {
				throw new Exception("Could not find an image with minimum size!");
			}

			// Download Image
			System.out.println("Found a correctly-sized image!");
			System.out.println("Downloading image...");
			BufferedImage image = ImageIO.read(images[selectedIndex].getUrl());
			// @formatter:off
			Thumbnails.of(image)
				.crop(Positions.CENTER)
				.size(minWidth, minHeight)
				.outputFormat("png")
				.toFile(new File("image.png"));
			// @formatter:on

			// End Here if Debug Mode
			if (DEBUG_MODE) {
				return;
			}

			// Determine OS & Handle Appropriately
			System.out.println("Setting image as desktop wallpaper...");
			final Process proc;
			final String OS = System.getProperty("os.name").toLowerCase();
			if (OS.indexOf("win") >= 0) {
				proc = new ProcessBuilder(new File("win-wallpaper.exe").getAbsolutePath(),
						new File("image.png").getAbsolutePath()).start();
			} else if (OS.indexOf("mac") >= 0) {
				proc = new ProcessBuilder(new File("mac-wallpaper").getAbsolutePath(),
						new File("image.png").getAbsolutePath()).start();
			} else {
				throw new Exception("Unsupported platform detected!");
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				System.out.println(line);
			}
			proc.waitFor();
			System.out.println("Done!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static ImageMeta[] parseApiResponse(String body) throws MalformedURLException, JSONException {
		JSONObject data = new JSONObject(body);
		JSONArray posts = data.getJSONObject("data").getJSONArray("children");

		ImageMeta[] images = new ImageMeta[posts.length()];
		for (int i = 0; i < posts.length(); i++) {
			JSONObject post = posts.getJSONObject(i).getJSONObject("data");
			JSONObject source = post.getJSONObject("preview").getJSONArray("images").getJSONObject(0)
					.getJSONObject("source");
			images[i] = new ImageMeta(source);
		}
		return images;
	}

	private static void debug(String message) {
		if (DEBUG_MODE) {
			System.out.println("[DEBUG] " + message);
		}
	}

}
