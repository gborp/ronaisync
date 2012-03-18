package com.braids.ronaisync;

import java.io.File;

public interface SyncGuiCallback {

	boolean errorUploadingAPhoto(File f, Exception ex);

}
