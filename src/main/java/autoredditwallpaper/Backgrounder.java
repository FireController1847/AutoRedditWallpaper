package autoredditwallpaper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;

public class Backgrounder {

	public static void main(String[] args) {
		try {
			// Options
			// TODO: Make changeable
			String subreddit = "r/EarthPorn";
			SortType sort = SortType.TOP;
			TimeType time = TimeType.DAY;
			int count = 20;

			// Advanced Options
			double upscaleFactor = 4.0 / 3.0;
			int screenWidth = 1920;
			int screenHeight = 1080;
			int minWidth = (int) Math.round(screenWidth * upscaleFactor);
			int minHeight = (int) Math.round(screenHeight * upscaleFactor);
			boolean debug = false;

			// Build Endpoint
			String endpoint = BuildEndpoint(subreddit, count, sort, time);

			// Make HTTP Request
			System.out.println("Searching reddit...");
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder(new URI(endpoint)).build();
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			Post[] posts = ParseApiResponse(response.body());
			System.out.println("Found " + posts.length + " posts!");

			// Find Image
			System.out.println("Parsing posts' images...");
			int selectedIndex = -1;
			for (int i = 0; i < posts.length; i++) {
				if (posts[i].getImageMeta().getWidth() >= minWidth && posts[i].getImageMeta().getHeight() >= minHeight) {
					selectedIndex = i;
					break;
				}
			}
			if (selectedIndex == -1) {
				throw new Exception("Could not find an image matching criteria!");
			}
			Post post = posts[selectedIndex];
			System.out.println("Found post matching image criteria!");
			System.out.println("\nTitle: " + post.getPostMeta().getTitle());
			System.out.println("Author: " + post.getPostMeta().getAuthor());
			System.out.println("Score: " + NumberHelper.format(post.getPostMeta().getScore()) + '\n');

			// Download Image
			System.out.println("Downloading image...");
			BufferedImage image = ImageIO.read(post.getImageMeta().getUrl());
			// @formatter:off
			Thumbnails.of(image)
				.crop(Positions.CENTER)
				.size(minWidth, minHeight)
				.outputFormat("png")
				.toFile(new File("image.png"));
			// @formatter:on
			System.out.println("Image downloaded!");

			// Return If Debug
			if (debug) {
				return;
			}

			// Determine OS & Set Wallpaper
			System.out.println("Setting image as wallpaper...");
			final Process PROCESS;
			final String OS = System.getProperty("os.name").toLowerCase();
			if (OS.indexOf("win") >= 0) {
				PROCESS = new ProcessBuilder(new File("win-wallpaper.exe").getAbsolutePath(),
						new File("image.png").getAbsolutePath()).start();
			} else if (OS.indexOf("mac") >= 0) {
				PROCESS = new ProcessBuilder(new File("mac-wallpaper").getAbsolutePath(),
						new File("image.png").getAbsolutePath()).start();
			} else {
				throw new Exception("Unsupported platform detected!");
			}
			PROCESS.waitFor();
			System.out.println("Done!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String BuildEndpoint(String subreddit, int count, SortType sort, TimeType time) {
		return String.format("https://api.reddit.com/%s/%s?limit=%s&t=%s", subreddit, sort.toString().toLowerCase(),
				count, time.toString().toLowerCase());
	}

	private static Post[] ParseApiResponse(String body) throws MalformedURLException, JSONException {
		JSONObject jData = new JSONObject(body);
		JSONArray jPosts = jData.getJSONObject("data").getJSONArray("children");
		
		Post[] posts = new Post[jPosts.length()];
		for (int i = 0; i < jPosts.length(); i++) {
			JSONObject jPost = jPosts.getJSONObject(i).getJSONObject("data");
			posts[i] = new Post(jPost);
		}
		return posts;
	}
	
	private static class Post {
		
		private ImageMeta image;
		private PostMeta meta;
		
		public Post(JSONObject jPost) throws MalformedURLException, JSONException {
			meta = new PostMeta(jPost);
			image = new ImageMeta(jPost.getJSONObject("preview").getJSONArray("images").getJSONObject(0).getJSONObject("source"));
		}
		
		public ImageMeta getImageMeta() {
			return image;
		}
		
		public PostMeta getPostMeta() {
			return meta;
		}
		
		public String toString() {
			return String.format("Post{image=%s,meta=%s}", image, meta);
		}
		
	}

	private static class PostMeta {

		private String title;
		private String author;
		private int score;
		private String id;

		public PostMeta(JSONObject jPost) {
			title = jPost.getString("title");
			author = jPost.getString("author");
			score = jPost.getInt("score");
			id = jPost.getString("id");
		}

		public String getTitle() {
			return title;
		}

		public String getAuthor() {
			return author;
		}

		public int getScore() {
			return score;
		}

		public String getId() {
			return id;
		}

		@Override
		public String toString() {
			return String.format("PostMeta{title=%s,author=%s,score=%s,id=%s}", title, author, score, id);
		}

	}

	private static class ImageMeta {

		private int width;
		private int height;
		private URL url;

		public ImageMeta(JSONObject jSource) throws MalformedURLException, JSONException {
			width = jSource.getInt("width");
			height = jSource.getInt("height");
			url = new URL(jSource.getString("url").replace("amp;", ""));
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
			return String.format("ImageMeta{width=%s,height=%s,url=%s}", width, height, url);
		}

	}

	private static enum SortType {
		TOP, HOT, NEW, CONTROVERSIAL
	}

	private static enum TimeType {
		HOUR, DAY, WEEK, MONTH, YEAR, ALL
	}

}
