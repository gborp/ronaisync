package com.braids.ronaisync;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.AuthenticationException;
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

	// USEFUL!
	// http://java2s.com/Open-Source/Java-Document-2/File/figoo/figoo/fileManager/FigooPicasaClient.java.htm

	private static final String UTIL_FILE = ".rns-images";

	private static final int CONNECT_TIMEOUT = 1000 * 60; // In
	// milliseconds
	private static final int READ_TIMEOUT = 1000 * 60; // In
	// milliseconds

	private List<String> lstExcludeAlbums = Arrays.asList("Auto Backup");

	private final String user;
	private final String password;
	private final String baseDirectory;
	private final SyncNotification syncNotification;
	private boolean cancel;
	private PicasawebService picasawebService;

	private boolean forceDownload;

	private boolean verbose;

	public Synchronizer(String baseDirectory, String user, String password,
			boolean verbose, SyncNotification syncNotification) {
		this.baseDirectory = baseDirectory;
		this.user = user;
		this.password = password;
		this.syncNotification = syncNotification;
		this.verbose = verbose;
	}

	public synchronized PicasawebService getPicasaService()
			throws AuthenticationException {
		if (picasawebService == null) {
			picasawebService = new PicasawebService("braids-ronaisync-1");
			picasawebService.setUserCredentials(user, password);

			picasawebService.setConnectTimeout(CONNECT_TIMEOUT);
			picasawebService.setReadTimeout(READ_TIMEOUT);
		}
		return picasawebService;
	}

	public void getWebAndLocalAlbums() throws AuthenticationException,
			IOException, ServiceException {
		syncNotification.webLocalAlbums(getWebAlbums(), getLocalAlbums());
	}

	public List<AlbumEntry> getWebAlbums() throws AuthenticationException,
			IOException, ServiceException {
		List<AlbumEntry> lstResult = getAlbumList(getPicasaService());
		Collections.sort(lstResult, new AlbumEntryComparator());

		return lstResult;
	}

	public void sync() throws IOException, ServiceException {

		PicasawebService picasaService = getPicasaService();

		List<AlbumEntry> lstAlbum = getAlbumList(picasaService);

		List<String> lstAlbumsToSync = new ArrayList<String>();
		for (AlbumEntry albumEntry : lstAlbum) {
			String albumName = albumEntry.getTitle().getPlainText();
			if (!lstExcludeAlbums.contains(albumName)) {
				lstAlbumsToSync.add(albumName);
			}
		}

		syncNotification.albums(lstAlbumsToSync);
		for (int albumIndex = 0; albumIndex < lstAlbumsToSync.size(); albumIndex++) {
			if (cancel) {
				return;
			}
			String albumName = lstAlbumsToSync.get(albumIndex);
			AlbumEntry albumEntry = null;
			for (AlbumEntry liAlbumEntry : lstAlbum) {
				if (albumName.equals(liAlbumEntry.getTitle().getPlainText())) {
					albumEntry = liAlbumEntry;
				}
			}

			if (albumEntry == null) {
				AlbumEntry album = new AlbumEntry();

				URL postUrl = new URL(
						"http://picasaweb.google.com/data/feed/api/user/"
								+ user);

				album.setTitle(new PlainTextConstruct(albumName));
				album.setDescription(new PlainTextConstruct(""));

				albumEntry = picasaService.insert(postUrl, album);
			}

			File dir = new File(baseDirectory, albumName);

			if (dir.isFile()) {
				throw new RuntimeException(
						"A file with the name "
								+ dir.toString()
								+ " already exists, but I need a directory with that name!");
			}
			if (!dir.isDirectory()) {
				dir.mkdirs();
			}

			List<PhotoEntry> lstPhotos = getPhotos(picasaService, albumEntry);

			syncNotification.startAlbumSync(albumName, albumIndex,
					lstPhotos.size());

			HashMap<String, FileSlot> mapLocalStoredFiles = getAlreadyOnceDownloadedFiles(dir);

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
					throw new RuntimeException(
							"A directory with the name "
									+ photoFile.toString()
									+ " already exists, but I need a file with that name!");
				}

				FileSlot fileSlot = mapLocalStoredFiles.get(photoName);

				boolean doDownload = !photoFile.isFile() || fileSlot == null;

				if (!doDownload) {
					doDownload = fileSlot.date != photoEntry.getTimestamp()
							.getTime();
				}

				if (doDownload) {
					// photoEntry.getFeedLink().getHref();
					String url = photoEntry.getMediaContents().get(0).getUrl();
					downloadPhoto(new URL(url), photoFile);
					if (photoFile.isFile()) {
						addOnceSyncedFile(dir, photoFile, photoEntry
								.getTimestamp().getTime());
					}
				}
			}

			syncNotification.endAlbumSync(albumName, albumIndex);
		}

	}

	private List<AlbumEntry> getAlbumList(PicasawebService picasaService)
			throws IOException, ServiceException {
		URL feedUrl = new URL(
				"https://picasaweb.google.com/data/feed/api/user/" + user
						+ "?kind=album");

		UserFeed myUserFeed = picasaService.getFeed(feedUrl, UserFeed.class);

		return myUserFeed.getAlbumEntries();
	}

	private List<PhotoEntry> getPhotos(PicasawebService myService,
			AlbumEntry albumEntry) throws IOException, ServiceException {
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

	public List<String> getLocalAlbums() {
		ArrayList<String> result = new ArrayList<String>();

		File baseDir = new File(baseDirectory);
		if (baseDir.isFile()) {
			throw new RuntimeException("Error: Directory is a file!");
		}
		if (!baseDir.isDirectory()) {
			baseDir.mkdirs();
		}
		if (!baseDir.isDirectory()) {
			throw new RuntimeException("Error: Directory cannot be made.");
		}

		for (File f : baseDir.listFiles()) {
			if (f.isDirectory()) {
				result.add(f.getName());
			}
		}

		return result;
	}

	private static class FileSlot {
		String name;
		long size;
		long date;
	}

	private HashMap<String, FileSlot> getAlreadyOnceDownloadedFiles(File dir) {
		HashMap<String, FileSlot> result = new HashMap<String, FileSlot>();
		File file = new File(dir, UTIL_FILE);
		if (!file.isFile()) {
			return result;
		}
		List<FileSlot> lstSlot = new ArrayList<FileSlot>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));

			while (true) {
				FileSlot slot = new FileSlot();
				slot.name = reader.readLine();
				if (slot.name == null) {
					break;
				}
				slot.size = Long.valueOf(reader.readLine());
				slot.date = Long.valueOf(reader.readLine());
				reader.readLine();
				lstSlot.add(slot);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		HashSet<String> fileNames = new HashSet<String>();

		for (int i = 0; i < lstSlot.size(); i++) {
			FileSlot slot = lstSlot.get(i);
			if (fileNames.contains(slot.name)) {
				result.remove(slot.name);
				i++;
			} else {
				fileNames.add(slot.name);
			}
		}

		for (FileSlot slot : lstSlot) {
			result.put(slot.name, slot);
		}

		return result;
	}

	private void addOnceSyncedFile(File dir, File photoFile, long timestamp) {
		File file = new File(dir, UTIL_FILE);

		BufferedWriter bw = null;

		try {
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file, true), Charset.forName("utf-8")));
			bw.write(photoFile.getName());
			bw.newLine();
			bw.write(Long.toString(photoFile.length()));
			bw.newLine();
			bw.write(Long.toString(timestamp));
			bw.newLine();
			bw.write("--------");
			bw.newLine();
			bw.flush();
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException ex) {
				}
			}
		}
	}
}
