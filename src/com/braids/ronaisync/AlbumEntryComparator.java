package com.braids.ronaisync;

import java.util.Comparator;

import com.google.gdata.data.photos.AlbumEntry;

public class AlbumEntryComparator implements Comparator<AlbumEntry> {

	public int compare(AlbumEntry o1, AlbumEntry o2) {
		return o1.getTitle().getPlainText()
				.compareToIgnoreCase(o2.getTitle().getPlainText());
	}

}
