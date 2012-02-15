package com.braids.ronaisync;

import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.BackingStoreException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.braids.ronaisync.ux.AlbumListItem;
import com.braids.ronaisync.ux.AlbumListItem.Type;
import com.braids.ronaisync.ux.Crypter;
import com.braids.ronaisync.ux.Frame;
import com.braids.ronaisync.ux.GuiHelper;
import com.braids.ronaisync.ux.Table;
import com.braids.ronaisync.ux.TableModel;
import com.google.gdata.data.photos.AlbumEntry;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

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
public class Workbench2 {

	private static final String APPLICATION_NAME = "";

	private JTextField sfUserName;
	private JPasswordField sfPassword;
	private JTextField sfDirectory;
	private JButton btnStartStop;
	private JTextArea taFlow;
	private JProgressBar prgAlbums;
	private JProgressBar prgPhotos;
	private boolean downloadInProgress;
	private Synchronizer syncDown;
	private JButton btnRefresh;
	private Table<AlbumListItem> tblAlbums;
	private TableModel<AlbumListItem> tmdlAlbums;
	private Frame frame;

	public Workbench2() throws GeneralSecurityException, IOException {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		GuiHelper.initGui();

		CellConstraints cc = new CellConstraints();

		JPanel pnlMain = new JPanel(new FormLayout("f:200dlu:g",
				"p:g,2dlu,f:100dlu:g"));

		JPanel pnlControl = new JPanel(new FormLayout("r:p,2dlu,f:100dlu:g",
				"p,2dlu,p,2dlu,p,2dlu,p,2dlu,p"));

		EspManager espManager = new EspManager();

		sfUserName = new JTextField(20);
		sfUserName.getDocument().addDocumentListener(espManager);

		sfPassword = new JPasswordField(20);
		sfPassword.getDocument().addDocumentListener(espManager);

		sfDirectory = new JTextField(20);
		sfDirectory.getDocument().addDocumentListener(espManager);

		btnRefresh = new JButton(new RefreshAction(espManager));
		btnStartStop = new JButton(new StartStopAction(espManager));

		pnlControl.add(new JLabel("Username"), cc.xy(1, 1));
		pnlControl.add(sfUserName, cc.xy(3, 1));

		pnlControl.add(new JLabel("Password"), cc.xy(1, 3));
		pnlControl.add(sfPassword, cc.xy(3, 3));

		pnlControl.add(new JLabel("Directory"), cc.xy(1, 5));
		pnlControl.add(sfDirectory, cc.xy(3, 5));

		pnlControl.add(btnRefresh, cc.xywh(3, 7, 1, 1, CellConstraints.RIGHT,
				CellConstraints.DEFAULT));
		pnlControl.add(btnStartStop, cc.xywh(3, 9, 1, 1, CellConstraints.RIGHT,
				CellConstraints.DEFAULT));

		JPanel pnlFlow = new JPanel(new FormLayout("f:200dlu:g",
				"f:10dlu:g,p,p"));
		taFlow = new JTextArea();

		tmdlAlbums = new TableModel<AlbumListItem>(new AlbumListItem());
		tblAlbums = new Table<AlbumListItem>(tmdlAlbums);
		prgAlbums = new JProgressBar();
		prgPhotos = new JProgressBar();
		pnlFlow.add(new JScrollPane(tblAlbums), cc.xy(1, 1));
		pnlFlow.add(prgAlbums, cc.xy(1, 2));
		pnlFlow.add(prgPhotos, cc.xy(1, 3));

		pnlMain.add(pnlControl, cc.xy(1, 1));
		pnlMain.add(pnlFlow, cc.xy(1, 3));

		sfUserName.setText(Crypter.decrypt(GuiHelper.getSettings()
				.getUserName()));
		sfPassword.setText(Crypter.decrypt(GuiHelper.getSettings()
				.getPassword()));
		sfDirectory.setText(Crypter.decrypt(GuiHelper.getSettings()
				.getDirectory()));

		espManager.check();

		frame = GuiHelper.createAndShowFrame(pnlMain, APPLICATION_NAME);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				GuiHelper.getSettings().setWindowBounds(APPLICATION_NAME,
						frame.getBounds());
				try {
					GuiHelper.getSettings().save();
				} catch (BackingStoreException e1) {
					e1.printStackTrace();
				} catch (GeneralSecurityException e1) {
					e1.printStackTrace();
				}
			}
		});
	}

	private class EspManager implements DocumentListener {

		boolean insideACheck;

		public void check() {
			if (insideACheck) {
				return;
			}
			insideACheck = true;
			try {

				if (downloadInProgress) {
					sfUserName.setEnabled(false);
					sfPassword.setEnabled(false);
					sfDirectory.setEnabled(false);
				} else {

					String dir = sfDirectory.getText();
					if (!dir.isEmpty()) {
						if (dir.startsWith("file://")) {
							try {
								File dirFile = new File(new URI(dir.trim()));
								if (dirFile.isFile()) {
									dir = dirFile.getParent();
								} else {
									dir = dirFile.getPath();
								}
								final String newDirValue = dir;
								SwingUtilities.invokeLater(new Runnable() {

									public void run() {
										sfDirectory.setText(newDirValue);
									}
								});

							} catch (URISyntaxException ex) {
								ex.printStackTrace();
								// ignore
							} catch (IllegalArgumentException ex) {
								ex.printStackTrace();
								// ignore
							}
						}

					}

					sfUserName.setEnabled(true);
					sfPassword.setEnabled(true);
					sfDirectory.setEnabled(true);

					boolean enableStart = sfUserName.getText().length() >= 6
							&& sfPassword.getPassword().length >= 6
							&& new File(dir).isDirectory();
					btnStartStop.setEnabled(enableStart);
				}
			} finally {
				insideACheck = false;
			}

		}

		public void changedUpdate(DocumentEvent e) {
			check();
		}

		public void insertUpdate(DocumentEvent e) {
			check();
		}

		public void removeUpdate(DocumentEvent e) {
			check();
		}

	}

	private class RefreshAction extends ThreadAction {

		private final EspManager espManager;

		public RefreshAction(EspManager espManager) {
			super("Refresh");
			this.espManager = espManager;
		}

		public void actionProcess(ActionEvent e) throws Exception {

			SwingWorker<List<AlbumEntry>, Object> worker = new SwingWorker<List<AlbumEntry>, Object>() {

				protected List<AlbumEntry> doInBackground() throws Exception {
					syncDown = new Synchronizer(sfDirectory.getText(),
							sfUserName.getText(), new String(
									sfPassword.getPassword()), new Notif(),
							SyncMode.DOWNLOAD);
					return syncDown.getWebAlbums();
				}

			};

			frame.waitFor(worker);
			worker.execute();

			tmdlAlbums.clear();

			ArrayList<AlbumListItem> lstAlbums = new ArrayList<AlbumListItem>();

			for (AlbumEntry album : worker.get()) {
				AlbumListItem li = new AlbumListItem();
				li.setName(album.getTitle().getPlainText());
				li.setType(Type.SERVER_ONLY);
				lstAlbums.add(li);
			}

			for (String localAlbumName : syncDown.getLocalAlbums()) {
				AlbumListItem foundLi = null;
				for (AlbumListItem li : lstAlbums) {
					if (li.getName().equals(localAlbumName)) {
						foundLi = li;
						break;
					}
				}
				if (foundLi != null) {
					foundLi.setType(Type.BOTH_SIDE);
				} else {
					AlbumListItem newLi = new AlbumListItem();
					newLi.setType(Type.LOCAL_ONLY);
					newLi.setName(localAlbumName);
					lstAlbums.add(newLi);
				}
			}

			Collections.sort(lstAlbums);
			for (AlbumListItem li : lstAlbums) {
				tmdlAlbums.addLine(li);
			}
		}
	}

	private class StartStopAction extends ThreadAction {

		private final EspManager espManager;

		public StartStopAction(EspManager espManager) {
			super("Start");
			this.espManager = espManager;
		}

		public void actionProcess(ActionEvent e) throws Exception {
			if (!downloadInProgress) {
				downloadInProgress = true;
				espManager.check();
				btnStartStop.setText("Cancel");
				taFlow.setText("");

				GuiHelper.getSettings().setUserName(
						Crypter.encrypt(sfUserName.getText()));
				GuiHelper.getSettings().setPassword(
						Crypter.encrypt(new String(sfPassword.getPassword())));
				GuiHelper.getSettings().setDirectory(
						Crypter.encrypt(sfDirectory.getText()));

				syncDown = new Synchronizer(sfDirectory.getText(),
						sfUserName.getText(), new String(
								sfPassword.getPassword()), new Notif(),
						SyncMode.DOWNLOAD);

				ArrayList<String> lstAlbums = new ArrayList<String>();
				for (AlbumListItem li : tmdlAlbums.getAllData()) {
					if (li.isSelected()) {
						lstAlbums.add(li.getName());
					}
				}
				syncDown.setAlbumsToSync(lstAlbums);

				syncDown.sync();
				downloadInProgress = false;
				taFlow.append("\nFinished!");
				espManager.check();
				btnStartStop.setText("Start");

			} else {
				syncDown.cancel();
				downloadInProgress = false;
				espManager.check();
				btnStartStop.setText("Start");
			}
		}

	}

	private class Notif implements SyncNotification {

		private List<String> lstAlbumNames;

		public void albums(List<String> lstAlbumNames) {
			this.lstAlbumNames = lstAlbumNames;
			SwingUtilities.invokeLater(new Runnable() {

				public void run() {
					prgAlbums.setMinimum(0);
					prgAlbums.setMaximum(Notif.this.lstAlbumNames.size());
					prgAlbums.setIndeterminate(false);
				}
			});
		}

		public void startAlbumSync(final String albumName,
				final int albumIndex, final int photoCount) {
			SwingUtilities.invokeLater(new Runnable() {

				public void run() {
					prgAlbums.setValue(albumIndex + 1);

					prgPhotos.setMinimum(0);
					prgPhotos.setMaximum(photoCount);
					prgPhotos.setIndeterminate(false);

					taFlow.append("Syncing album (" + (albumIndex + 1) + "/"
							+ lstAlbumNames.size() + "): " + albumName + "\n");
				}
			});
		}

		public void startPhotoSync(String photoName, int photoIndex) {
			prgPhotos.setValue(photoIndex + 1);
		}

	}

}
