package com.braids.ronaisync;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.activation.MimetypesFileTypeMap;

import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.media.MediaFileSource;
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

	private static final String UTIL_FILE = ".ronay";

	private static final int CONNECT_TIMEOUT = 1000 * 60; // In
	// milliseconds
	private static final int READ_TIMEOUT = 1000 * 60; // In
	// milliseconds

	private final String user;
	private final String password;
	private final String baseDirectory;
	private final SyncNotification syncNotification;
	private boolean cancel;
	private PicasawebService picasawebService;
	private List<String> lstAlbumsToSync;
	private int bandwidth;
	private Timer timerBandwidth;
	private final SyncGuiCallback guiCallback;

	private boolean forceDownload;

	public Synchronizer(String baseDirectory, String user, String password,
			SyncNotification syncNotification, SyncGuiCallback guiCallback) {
		this.baseDirectory = baseDirectory;
		this.user = user;
		this.password = password;
		this.syncNotification = syncNotification;
		this.guiCallback = guiCallback;
	}

	public void setAlbumsToSync(List<String> lstAlbumsToSync) {
		this.lstAlbumsToSync = lstAlbumsToSync;
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
		startBandwidthMeter();
		try {
			syncNotification.webLocalAlbums(getWebAlbums(), getLocalAlbums());
		} finally {
			stopBandwidthMeter();
		}
	}

	public List<AlbumEntry> getWebAlbums() throws AuthenticationException,
			IOException, ServiceException {
		List<AlbumEntry> lstResult = getAlbumList(getPicasaService());
		Collections.sort(lstResult, new AlbumEntryComparator());

		return lstResult;
	}

	private void startBandwidthMeter() {
		bandwidth = 0;
		timerBandwidth = new Timer("Bandwidth", true);
		timerBandwidth.schedule(new TimerTask() {
			@Override
			public void run() {
				syncNotification.bandwidth(bandwidth);
				bandwidth = 0;
			}

		}, 500, 1000);
	}

	private void stopBandwidthMeter() {
		timerBandwidth.cancel();
		timerBandwidth = null;
	}

	private List<String> getAlreadyOnceDownloadedFiles(File dir) {
		File file = new File(dir, UTIL_FILE);
		ArrayList<String> result = new ArrayList<String>();
		if (!file.isFile()) {
			return result;
		}
		Scanner s = null;
		try {
			s = new Scanner(new FileInputStream(file), "utf-8");
			while (s.hasNext()) {
				String line = s.next();
				result.add(line);
				s.next();
				s.next();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (s != null) {
				s.close();
			}
		}
		return result;
	}

	private List<Long> getAlreadyOnceDownloadedFileSizes(File dir) {
		File file = new File(dir, UTIL_FILE);
		ArrayList<Long> result = new ArrayList<Long>();
		if (!file.isFile()) {
			return result;
		}
		Scanner s = null;
		try {
			s = new Scanner(new FileInputStream(file), "utf-8");
			while (s.hasNext()) {
				s.next();
				Long line = Long.valueOf(s.next());
				result.add(line);
				s.next();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (s != null) {
				s.close();
			}
		}
		return result;
	}

	private void addOnceSyncedFile(File dir, File photoFile) {
		File file = new File(dir, UTIL_FILE);

		BufferedWriter bw = null;

		try {
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file, true), Charset.forName("utf-8")));
			bw.write(photoFile.getName());
			bw.newLine();
			bw.write(Long.toString(photoFile.length()));
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

	// TODO handle mixed upload situation
	public void sync() throws IOException, ServiceException {
		startBandwidthMeter();
		try {

			PicasawebService picasaService = getPicasaService();

			List<AlbumEntry> lstAlbum = getAlbumList(picasaService);

			// List<String> lstAlbumNames = new ArrayList<String>();
			// for (AlbumEntry albumEntry : lstAlbum) {
			// String albumName = albumEntry.getTitle().getPlainText();
			// lstAlbumNames.add(albumName);
			// }

			syncNotification.albums(lstAlbumsToSync);
			for (int albumIndex = 0; albumIndex < lstAlbumsToSync.size(); albumIndex++) {
				if (cancel) {
					return;
				}
				String albumName = lstAlbumsToSync.get(albumIndex);
				AlbumEntry albumEntry = null;
				for (AlbumEntry liAlbumEntry : lstAlbum) {
					if (albumName
							.equals(liAlbumEntry.getTitle().getPlainText())) {
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

				List<String> lstDownloadedOnce = getAlreadyOnceDownloadedFiles(dir);

				if (dir.isFile()) {
					throw new RuntimeException(
							"A file with the name "
									+ dir.toString()
									+ " already exists, but I need a directory with that name!");
				}
				if (!dir.isDirectory()) {
					dir.mkdirs();
				}

				HashSet<File> setFilesOnTheWeb = new HashSet<File>();
				List<PhotoEntry> lstPhotos = getPhotos(picasaService,
						albumEntry);

				syncNotification.startAlbumSync(albumName, albumIndex,
						lstPhotos.size());

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

					setFilesOnTheWeb.add(photoFile);

					boolean notExists = !photoFile.isFile();
					boolean existsDifferentSize = photoEntry.hasSizeExt()
							&& (photoEntry.getSize() != photoFile.length());

					if (!lstDownloadedOnce.contains(photoName) || forceDownload) {
						if (notExists || existsDifferentSize) {
							// photoEntry.getFeedLink().getHref();
							String url = photoEntry.getMediaContents().get(0)
									.getUrl();
							downloadPhoto(new URL(url), photoFile);
							if (photoFile.isFile()) {
								addOnceSyncedFile(dir, photoFile);
							}
						}
					}
				}

				ArrayList<File> setLocalFiles = new ArrayList<File>();
				for (File f : dir.listFiles()) {

					String lcName = f.getName().toLowerCase();

					if (f.isFile()
							&& !f.getName().equals(UTIL_FILE)
							&& (lcName.endsWith(".jpg") || lcName
									.endsWith(".jpeg"))) {
						setLocalFiles.add(f);
					}
				}

				setLocalFiles.removeAll(setFilesOnTheWeb);

				// TODO ask the user whether upload or delete

				syncNotification.startAlbumSync(albumName, albumIndex,
						lstPhotos.size());
				photoIndex = 0;
				Collections.sort(setLocalFiles);
				for (File f : setLocalFiles) {
					String photoName = f.getName();
					syncNotification.startPhotoSync(photoName, photoIndex);
					boolean error = false;
					try {
						addPhoto(picasaService, albumEntry, f);
					} catch (Exception ex) {
						error = true;
						System.out
								.println("Error uploading file: " + photoName);
						// FIXME
						// if (!guiCallback.errorUploadingAPhoto(f, ex)) {
						// break;
						// }
					}
					if (!error) {
						addOnceSyncedFile(dir, f);
					}

					photoIndex++;
				}

				// TODO write syncedfiles
			}
		} finally {
			stopBandwidthMeter();
		}
	}

	private synchronized void addPhoto(PicasawebService service,
			AlbumEntry albumEntry, File file) throws MalformedURLException,
			IOException, ServiceException {
		String albumId = albumEntry.getGphotoId();
		URL albumPostUrl = new URL(
				"http://picasaweb.google.com/data/feed/api/user/" + user
						+ "/albumid/" + albumId);
		PhotoEntry photo = new PhotoEntry();
		photo.setTitle(new PlainTextConstruct(file.getName()));
		photo.setDescription(new PlainTextConstruct(""));
		photo.setClient("Ronai Sync");
		MediaFileSource myMedia = new MediaFileSource(file,
				new MimetypesFileTypeMap().getContentType(file));
		photo.setMediaSource(myMedia);
		service.insert(albumPostUrl, photo);
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
						bandwidth += bytesRead;
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

}
