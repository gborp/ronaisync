package com.braids.ronaisync;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.prefs.BackingStoreException;

import org.eclipse.swt.widgets.Display;

import com.braids.ronaisync.ux.Crypter;
import com.braids.ronaisync.ux.GuiHelper;

public class Manager {

	private static final String GNOME_ENV_VAR = "GNOME_DESKTOP_SESSION_ID";
	private static final String XFCE_ENV_VAR = "XDG_MENU_PREFIX";

	public String username;
	public String password;
	public String directory;

	public Manager() {

	}

	public boolean isGnome() {
		String var = System.getenv(GNOME_ENV_VAR);

		return var != null && !var.trim().equals("");
	}

	public boolean isXfce() {
		String var = System.getenv(XFCE_ENV_VAR);

		return var != null && var.trim().equals("xfce-");
	}

	private String nullSafe(String text) {
		return text != null ? text : "";
	}

	public void loadSettings() throws BackingStoreException,
			GeneralSecurityException, IOException {
		GuiHelper.getSettings().load();
		username = nullSafe(Crypter.decrypt(GuiHelper.getSettings()
				.getUserName()));
		password = nullSafe(Crypter.decrypt(GuiHelper.getSettings()
				.getPassword()));
		directory = nullSafe(Crypter.decrypt(GuiHelper.getSettings()
				.getDirectory()));
	}

	public void saveSettings() throws BackingStoreException,
			GeneralSecurityException, IOException {
		GuiHelper.getSettings().setUserName(Crypter.encrypt(username));
		GuiHelper.getSettings().setPassword(Crypter.encrypt(password));
		GuiHelper.getSettings().setDirectory(Crypter.encrypt(directory));
		GuiHelper.getSettings().save();
	}

	public void start() {
		try {
			loadSettings();
			WorkbenchWindow window = new WorkbenchWindow(this);
			window.setBlockOnOpen(true);
			window.open();
			saveSettings();
			Display.getCurrent().dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
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

	public void showFolder(String openDir) {
		try {
			if (isXfce()) {
				ProcessBuilder pb = new ProcessBuilder("exo-open", "--launch",
						"FileManager", openDir);
				pb.start();
			} else if (isGnome()) {
				ProcessBuilder pb = new ProcessBuilder("xdg-open", openDir);
				pb.start();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void showNotifiy(String title, String message) {
		try {
			ProcessBuilder pb = new ProcessBuilder("notify-send", "-c",
					"transfer", "-t", "5000", "-i", "finish", title, message);
			pb.start();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}