package com.braids.ronaisync;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ServiceException;

/**
 * RonaiSync is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.<br>
 * <br>
 * PagaVCS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.<br>
 * <br>
 * You should have received a copy of the GNU General Public License along with
 * RonaiSync; If not, see http://www.gnu.org/licenses/.
 */
public class Synchronizer {

	private static final int       CONNECT_TIMEOUT = 1000 * 60; // In
	// milliseconds
	private static final int       READ_TIMEOUT    = 1000 * 60; // In
	// milliseconds

	private final String           user;
	private final String           password;
	private final String           baseDirectory;
	private final SyncNotification syncNotification;
	private boolean                cancel;
	private SyncMode               syncMode;

	public Synchronizer(String baseDirectory, String user, String password, SyncNotification syncNotification, SyncMode syncMode) {
		this.baseDirectory = baseDirectory;
		this.user = user;
		this.password = password;
		this.syncNotification = syncNotification;
		this.syncMode = syncMode;

	}

	public void sync() throws IOException, ServiceException {
		PicasawebService myService = new PicasawebService("braids-ronaisync-1");
		myService.setUserCredentials(user, password);

		myService.setConnectTimeout(CONNECT_TIMEOUT);
		myService.setReadTimeout(READ_TIMEOUT);

		List<AlbumEntry> lstAlbum = getAlbumList(myService);

		List<String> lstAlbumNames = new ArrayList<String>();
		for (AlbumEntry albumEntry : lstAlbum) {
			String albumName = albumEntry.getTitle().getPlainText();
			lstAlbumNames.add(albumName);
		}

		syncNotification.albums(lstAlbumNames);
		for (int albumIndex = 0; albumIndex < lstAlbum.size(); albumIndex++) {
			if (cancel) {
				return;
			}
			AlbumEntry albumEntry = lstAlbum.get(albumIndex);
			String albumName = albumEntry.getTitle().getPlainText();
			File dir = new File(baseDirectory, albumName);

			if (dir.isFile()) {
				throw new RuntimeException("A file with the name " + dir.toString() + " already exists, but I need a directory with that name!");
			}
			if (!dir.isDirectory()) {
				dir.mkdirs();
			}

			HashSet<File> setFilesOnTheWeb = new HashSet<File>();
			List<PhotoEntry> lstPhotos = getPhotos(myService, albumEntry);

			syncNotification.startAlbumSync(albumName, albumIndex, lstPhotos.size());

			int photoIndex = 0;
			for (PhotoEntry photoEntry : lstPhotos) {
				if (cancel) {
					return;
				}

				String photoName = photoEntry.getTitle().getPlainText();
				File photoFile = new File(dir, photoName);

				syncNotification.startPhotoSync(photoName, photoIndex);
				photoIndex++;
				if (photoFile.isDirectory()) {
					throw new RuntimeException("A directory with the name " + photoFile.toString() + " already exists, but I need a file with that name!");
				}

				setFilesOnTheWeb.add(photoFile);

				boolean notExists = !photoFile.isFile();
				boolean existsDifferentSize = photoEntry.hasSizeExt() && (photoEntry.getSize() != photoFile.length());

				if (syncMode == SyncMode.DOWNLOAD) {
					if (notExists || existsDifferentSize) {
						// photoEntry.getFeedLink().getHref();
						String url = photoEntry.getMediaContents().get(0).getUrl();
						downloadPhoto(new URL(url), photoFile);
					}
				}
			}

			HashSet<File> setLocalFiles = new HashSet<File>();
			for (File f : dir.listFiles()) {
				if (f.isFile()) {
					setLocalFiles.add(f);
				}
			}
			setLocalFiles.removeAll(setFilesOnTheWeb);
			for (File f : setLocalFiles) {
				addFilesNotOnTheWeb(f);
			}
		}
	}

	private void addFilesNotOnTheWeb(File f) {
	// TODO Auto-generated method stub
	}

	private List<AlbumEntry> getAlbumList(PicasawebService myService) throws IOException, ServiceException {
		URL feedUrl = new URL("https://picasaweb.google.com/data/feed/api/user/" + user + "?kind=album");

		UserFeed myUserFeed = myService.getFeed(feedUrl, UserFeed.class);

		return myUserFeed.getAlbumEntries();
	}

	private List<PhotoEntry> getPhotos(PicasawebService myService, AlbumEntry albumEntry) throws IOException, ServiceException {
		String urlString = albumEntry.getFeedLink().getHref();
		if (urlString.contains("?")) {
			urlString += "&imgmax=d";
		} else {
			urlString += "?imgmax=d";
		}
		URL feedUrl = new URL(urlString);
		// System.out.println("Url: " + urlString);

		AlbumFeed feed = myService.getFeed(feedUrl, AlbumFeed.class);

		return feed.getPhotoEntries();

	}

	private void downloadPhoto(URL imageURL, File file) {
		boolean succesful = false;
		try {
			InputStream input = imageURL.openStream();
			if (input != null) {
				FileOutputStream output = null;
				try {
					output = new FileOutputStream(file);
					byte[] buffer = new byte[8192];
					int bytesRead;
					while (!cancel && (bytesRead = input.read(buffer)) > 0) {
						output.write(buffer, 0, bytesRead);
					}
					output.close();
				} finally {
					input.close();
					if (output != null) {
						output.close();
					}
				}
			}
			succesful = true;
		} catch (Exception e) {
			// TODO
			e.printStackTrace();
		} finally {
			if (!succesful) {
				file.delete();
			}
		}
	}

	public void cancel() {
		cancel = true;
	}
}
