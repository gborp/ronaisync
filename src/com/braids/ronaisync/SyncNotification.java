package com.braids.ronaisync;

import java.util.List;

import com.google.gdata.data.photos.AlbumEntry;

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
public interface SyncNotification {

	void albums(List<String> lstAlbumNames);

	void startAlbumSync(String albumName, int albumIndex, int photoCount);

	void startPhotoSync(String photoName, int photoIndex);

	void webLocalAlbums(List<AlbumEntry> lstWebAlbums,
			List<String> lstLocalAlbums);

	void bandwidth(int bandwidth);

}
