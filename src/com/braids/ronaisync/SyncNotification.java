package com.braids.ronaisync;

import java.util.List;

public interface SyncNotification {

	void albums(List<String> lstAlbumNames);

	void startAlbumSync(String albumName, int albumIndex, int photoCount);

	void startPhotoSync(String photoName, int photoIndex);

}
