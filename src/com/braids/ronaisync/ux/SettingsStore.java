package com.braids.ronaisync.ux;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * PagaVCS is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.<br>
 * <br>
 * PagaVCS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.<br>
 * <br>
 * You should have received a copy of the GNU General Public License along with
 * PagaVCS; If not, see http://www.gnu.org/licenses/.
 */
public class SettingsStore {

	private Preferences prefs = Preferences.userNodeForPackage(this.getClass());
	private static SettingsStore singleton;

	private static final String KEY_USERNAME = "username";
	private static final String KEY_PASSWORD = "password";
	private static final String KEY_DIRECTORY = "directory";
	private static final String KEY_WINDOW_BOUNDS = "window-bounds";

	private String userName;
	private String password;
	private String directory;
	private Map<String, String> mapWindowBounds = new HashMap<String, String>();

	public static SettingsStore getInstance() {
		if (singleton == null) {
			singleton = new SettingsStore();
		}
		return singleton;
	}

	public void save() throws BackingStoreException, GeneralSecurityException {
		storeString(KEY_USERNAME, userName);
		storeString(KEY_PASSWORD, password);
		storeString(KEY_DIRECTORY, directory);
		storeMap(KEY_WINDOW_BOUNDS, mapWindowBounds, false);
		prefs.flush();
	}

	public void load() throws BackingStoreException, GeneralSecurityException,
			IOException {
		userName = loadString(KEY_USERNAME);
		password = loadString(KEY_PASSWORD);
		directory = loadString(KEY_DIRECTORY);
		mapWindowBounds = loadMap(KEY_WINDOW_BOUNDS, false);
	}

	private List<String> loadList(String listName) throws BackingStoreException {
		Map<String, String> result = new HashMap<String, String>();
		List<String> resultList = new ArrayList<String>();
		Preferences node = prefs.node(listName);
		for (String key : node.keys()) {
			result.put(key, node.get(key, null));
		}
		Object[] keys = result.keySet().toArray();
		Arrays.sort(keys);
		for (Object key : keys) {
			resultList.add(result.get(key));
		}
		return resultList;
	}

	private void storeList(String mapName, List<String> data)
			throws BackingStoreException {
		Preferences node = prefs.node(mapName);
		node.clear();
		int index = 0;
		for (String li : data) {
			node.put(String.format("%05d", index), li);
			index++;
		}
	}

	private Map<String, String> loadMap(String mapName, boolean encoded)
			throws BackingStoreException, GeneralSecurityException, IOException {
		Map<String, String> result = new HashMap<String, String>();
		Preferences node = prefs.node(mapName);
		for (String key : node.keys()) {
			String value = node.get(key, null);
			if (encoded) {
				value = Crypter.decrypt(value);
			}
			result.put(key, value);
		}
		return result;
	}

	private void storeMap(String mapName, Map<String, String> data,
			boolean encoded) throws BackingStoreException,
			GeneralSecurityException {
		Preferences node = prefs.node(mapName);
		node.clear();
		for (Entry<String, String> entry : data.entrySet()) {
			String value = entry.getValue();
			if (encoded) {
				value = Crypter.encrypt(value);
			}
			node.put(entry.getKey(), value);
		}
	}

	private Boolean loadBoolean(String name) {
		Integer intValue = loadInteger(name);
		if (intValue == null) {
			return null;
		}
		return intValue == 1;
	}

	private void storeBoolean(String name, Boolean value) {
		Integer valueToStore = null;
		if (value != null) {
			valueToStore = value ? 1 : 0;
		}
		storeInteger(name, valueToStore);
	}

	private Integer loadInteger(String name) {
		String value = prefs.get(name, null);
		if (value == null) {
			return null;
		}
		return Integer.valueOf(value);
	}

	private void storeInteger(String name, Integer value) {
		if (value == null) {
			prefs.remove(name);
		} else {
			prefs.put(name, Integer.toString(value));
		}
	}

	private String loadString(String name) {
		return prefs.get(name, null);
	}

	private void storeString(String name, String value) {
		if (value == null) {
			prefs.remove(name);
		} else {
			prefs.put(name, value);
		}
	}

	// private Double loadDouble(String name) {
	// String value = prefs.get(name, null);
	// if (value == null) {
	// return null;
	// }
	// return Double.valueOf(value);
	// }
	//
	// private void storeDouble(String name, Double value) {
	// if (value == null) {
	// prefs.remove(name);
	// } else {
	// prefs.put(name, Double.toString(value));
	// }
	// }

	private String rectangleToString(Rectangle r) {
		return "" + r.x + " " + r.y + " " + r.width + " " + r.height;
	}

	private Rectangle stringToRectangle(String s) {
		String[] v = s.split(" ");
		return new Rectangle(Integer.valueOf(v[0]), Integer.valueOf(v[1]),
				Integer.valueOf(v[2]), Integer.valueOf(v[3]));
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public Rectangle getWindowBounds(Window parent, String windowName) {
		if (parent != null) {
			Rectangle result = getWindowBounds(parent.getName() + "->"
					+ windowName);
			if (result != null) {
				Point parentLocation = parent.getLocationOnScreen();
				result.x += parentLocation.getX();
				result.y += parentLocation.getY();
			}
			return result;
		} else {
			return getWindowBounds(windowName);
		}
	}

	public Rectangle getWindowBounds(String windowName) {
		if (mapWindowBounds.containsKey(windowName)) {
			return stringToRectangle(mapWindowBounds.get(windowName));
		} else {
			return null;
		}
	}

	public void setWindowBounds(Window parent, String windowName,
			Rectangle bounds) {
		if (parent != null) {
			Point parentLocation = parent.getLocationOnScreen();
			bounds.x -= parentLocation.getX();
			bounds.y -= parentLocation.getY();
			setWindowBounds(parent.getName() + "->" + windowName, bounds);
		} else {
			setWindowBounds(windowName, bounds);
		}
	}

	public void setWindowBounds(String windowName, Rectangle bounds) {
		mapWindowBounds.put(windowName, rectangleToString(bounds));
	}

}
