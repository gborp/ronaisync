package com.braids.ronaisync;

import java.io.IOException;
import java.util.List;

import com.google.gdata.data.photos.AlbumEntry;

public class GnomeSyncNotification implements SyncNotification {

	private static final int SHORT_MESSAGE = 1000;
	private static final int LONG_MESSAGE = 1000;

	public void showNotifiy(String title, String message, int period) {
		try {
			System.out.println(title + " - " + message);
			ProcessBuilder pb = new ProcessBuilder("notify-send", "-c",
					"transfer", "-t", Integer.toString(period), "-i", "finish",
					title, message);
			pb.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void albums(List<String> lstAlbumNames) {
	}

	@Override
	public void startAlbumSync(String albumName, int albumIndex, int photoCount) {
		showNotifiy("Album sync started", albumName, SHORT_MESSAGE);
	}

	@Override
	public void endAlbumSync(String albumName, int albumIndex) {
		showNotifiy("Album sync finished", albumName, SHORT_MESSAGE);
	}

	@Override
	public void startPhotoSync(String photoName, int photoIndex) {
	}

	@Override
	public void webLocalAlbums(List<AlbumEntry> lstWebAlbums,
			List<String> lstLocalAlbums) {
	}

}
