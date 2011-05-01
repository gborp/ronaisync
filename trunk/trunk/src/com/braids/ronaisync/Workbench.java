package com.braids.ronaisync;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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
public class Workbench {

	private JTextField     sfUserName;
	private JPasswordField sfPassword;
	private JTextField     sfDirectory;
	private JButton        btnStartStop;
	private JTextArea      taFlow;
	private JProgressBar   prgAlbums;
	private JProgressBar   prgPhotos;
	private boolean        downloadInProgress;
	private SyncDown       syncDown;

	public Workbench() {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		JFrame frame = new JFrame("Rónai Sync");
		List<Image> lstIcons = new ArrayList<Image>();
		lstIcons.add(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/braids/ronaisync/resources/logo16.png")));
		lstIcons.add(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/braids/ronaisync/resources/logo32.png")));
		lstIcons.add(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/braids/ronaisync/resources/logo64.png")));
		lstIcons.add(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/braids/ronaisync/resources/logo128.png")));
		lstIcons.add(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/braids/ronaisync/resources/logo256.png")));

		frame.setIconImages(lstIcons);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		CellConstraints cc = new CellConstraints();

		JPanel pnlMain = new JPanel(new FormLayout("f:200dlu:g", "p:g,2dlu,f:100dlu:g"));

		JPanel pnlControl = new JPanel(new FormLayout("r:p,2dlu,f:100dlu:g", "p,2dlu,p,2dlu,p,2dlu,p"));

		EspManager espManager = new EspManager();

		sfUserName = new JTextField(20);
		sfUserName.getDocument().addDocumentListener(espManager);

		sfPassword = new JPasswordField(20);
		sfPassword.getDocument().addDocumentListener(espManager);

		sfDirectory = new JTextField(20);
		sfDirectory.getDocument().addDocumentListener(espManager);

		btnStartStop = new JButton(new StartStopAction(espManager));

		pnlControl.add(new JLabel("Username"), cc.xy(1, 1));
		pnlControl.add(sfUserName, cc.xy(3, 1));

		pnlControl.add(new JLabel("Password"), cc.xy(1, 3));
		pnlControl.add(sfPassword, cc.xy(3, 3));

		pnlControl.add(new JLabel("Directory"), cc.xy(1, 5));
		pnlControl.add(sfDirectory, cc.xy(3, 5));

		pnlControl.add(btnStartStop, cc.xywh(3, 7, 1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));

		JPanel pnlFlow = new JPanel(new FormLayout("f:200dlu:g", "f:10dlu:g,p,p"));
		taFlow = new JTextArea();
		prgAlbums = new JProgressBar();
		prgPhotos = new JProgressBar();
		pnlFlow.add(taFlow, cc.xy(1, 1));
		pnlFlow.add(prgAlbums, cc.xy(1, 2));
		pnlFlow.add(prgPhotos, cc.xy(1, 3));

		pnlMain.add(pnlControl, cc.xy(1, 1));
		pnlMain.add(pnlFlow, cc.xy(1, 3));

		frame.add(pnlMain);

		espManager.check();

		frame.pack();
		frame.setVisible(true);
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
							}
						}

					}

					sfUserName.setEnabled(true);
					sfPassword.setEnabled(true);
					sfDirectory.setEnabled(true);

					boolean enableStart = sfUserName.getText().length() >= 6 && sfPassword.getPassword().length >= 6 && new File(dir).isDirectory();
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

				syncDown = new SyncDown(sfDirectory.getText(), sfUserName.getText(), new String(sfPassword.getPassword()), new Notif());
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

		public void startAlbumSync(final String albumName, final int albumIndex, final int photoCount) {
			SwingUtilities.invokeLater(new Runnable() {

				public void run() {
					prgAlbums.setValue(albumIndex + 1);

					prgPhotos.setMinimum(0);
					prgPhotos.setMaximum(photoCount);
					prgPhotos.setIndeterminate(false);

					taFlow.append("Syncing album (" + (albumIndex + 1) + "/" + lstAlbumNames.size() + "): " + albumName + "\n");
				}
			});
		}

		public void startPhotoSync(String photoName, int photoIndex) {
			prgPhotos.setValue(photoIndex + 1);
		}

	}

}
