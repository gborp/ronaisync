package com.braids.ronaisync;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import com.braids.ronaisync.ux.AlbumListItem;
import com.braids.ronaisync.ux.AlbumListItem.Type;
import com.google.gdata.data.photos.AlbumEntry;
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
public class WorkbenchWindow extends ApplicationWindow {

	private enum STATUS {
		PREINIT, IDLE, REFRESH, SYNC
	}

	private Table tblAlbums;
	private CheckboxTableViewer checkboxTableViewer;
	private Button btnRefresh;
	private ProgressBar progressBar;
	private Button btnSync;
	private Synchronizer syncer;
	private Button btnCancel;
	private STATUS status;
	private String progressBarString;
	private final Manager manager;
	private Button btnSettings;
	private Label lblUser;

	/**
	 * Create the application window.
	 * 
	 * @param manager
	 */
	public WorkbenchWindow(Manager manager) {
		super(null);
		this.manager = manager;
	}

	/**
	 * Create contents of the application window.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new FormLayout());

		progressBar = new ProgressBar(container, SWT.NONE);
		FormData fd_progressBar = new FormData();
		fd_progressBar.bottom = new FormAttachment(100, -21);
		fd_progressBar.left = new FormAttachment(0, 10);
		progressBar.setLayoutData(fd_progressBar);

		progressBar.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				if (progressBarString != null) {
					Point point = progressBar.getSize();

					FontMetrics fontMetrics = e.gc.getFontMetrics();
					int width = fontMetrics.getAverageCharWidth()
							* progressBarString.length();
					int height = fontMetrics.getHeight();
					e.gc.setForeground(getShell().getDisplay().getSystemColor(
							SWT.COLOR_WIDGET_FOREGROUND));
					e.gc.drawString(progressBarString, (point.x - width) / 2,
							(point.y - height) / 2, true);
				}
			}
		});

		checkboxTableViewer = CheckboxTableViewer.newCheckList(container,
				SWT.BORDER | SWT.FULL_SELECTION);
		tblAlbums = checkboxTableViewer.getTable();
		fd_progressBar.right = new FormAttachment(100, -10);
		FormData fd_tblAlbums = new FormData();
		fd_tblAlbums.left = new FormAttachment(0, 10);
		fd_tblAlbums.right = new FormAttachment(100, -10);
		tblAlbums.setLayoutData(fd_tblAlbums);

		btnRefresh = new Button(container, SWT.NONE);
		fd_tblAlbums.top = new FormAttachment(btnRefresh, 4);
		FormData fd_btnRefresh = new FormData();
		fd_btnRefresh.right = new FormAttachment(100, -10);
		btnRefresh.setLayoutData(fd_btnRefresh);
		btnRefresh.setText("Refresh");

		btnRefresh.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				doRefresh();
			}
		});

		checkboxTableViewer.setContentProvider(ArrayContentProvider
				.getInstance());
		checkboxTableViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent e) {
				if (e.getSelection() instanceof IStructuredSelection) {
					AlbumListItem selected = (AlbumListItem) ((IStructuredSelection) e
							.getSelection()).getFirstElement();
					if (selected.getType() != Type.SERVER_ONLY) {
						String openDir = manager.getDirectory()
								+ File.separator + selected.getName();
						manager.showFolder(openDir);
					}

				}

			}
		});

		// TODO
		// checkboxTableViewer.setLabelProvider()

		btnSync = new Button(container, SWT.NONE);
		fd_tblAlbums.bottom = new FormAttachment(btnSync, -6);
		FormData fd_btnSync = new FormData();
		fd_btnSync.left = new FormAttachment(progressBar, -123);
		fd_btnSync.bottom = new FormAttachment(progressBar, -6);
		fd_btnSync.right = new FormAttachment(progressBar, 0, SWT.RIGHT);
		btnSync.setLayoutData(fd_btnSync);
		btnSync.setText("Sync!");

		btnSync.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {

				status = STATUS.SYNC;
				espManager();

				final List<String> lstAlbums = new ArrayList<String>();

				for (Object li : checkboxTableViewer.getCheckedElements()) {
					AlbumListItem liAlbum = (AlbumListItem) li;
					lstAlbums.add(liAlbum.getName());
				}

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							syncer.setAlbumsToSync(lstAlbums);
							syncer.sync();
						} catch (IOException e) {
							e.printStackTrace();
						} catch (ServiceException e) {
							e.printStackTrace();
						} finally {
							getShell().getDisplay().asyncExec(new Runnable() {
								public void run() {
									doRefresh();
								}
							});
							manager.showNotifiy("Synchronization finished", "");
						}
					}

				}).start();

			}
		});
		// TODO
		// syncer.cancel();

		btnCancel = new Button(container, SWT.NONE);
		FormData fd_btnCancel = new FormData();
		fd_btnCancel.bottom = new FormAttachment(progressBar, -6);
		fd_btnCancel.left = new FormAttachment(0, 10);
		btnCancel.setLayoutData(fd_btnCancel);
		btnCancel.setText("cancel");

		lblUser = new Label(container, SWT.NONE);
		fd_btnRefresh.top = new FormAttachment(0, 10);
		FormData fd_lblUser = new FormData();
		fd_lblUser.top = new FormAttachment(0, 10);
		fd_lblUser.left = new FormAttachment(0, 10);
		lblUser.setLayoutData(fd_lblUser);
		lblUser.setText("User");

		btnSettings = new Button(container, SWT.NONE);
		FormData fd_btnSettings = new FormData();
		fd_btnSettings.top = new FormAttachment(btnRefresh, 0, SWT.TOP);
		fd_btnSettings.left = new FormAttachment(lblUser, 6);
		btnSettings.setLayoutData(fd_btnSettings);
		btnSettings.setText("Settings");

		lblUser.setText(manager.getUsername());
		btnSettings.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				SettingsDialog dialog = new SettingsDialog(getShell(),
						SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL, manager);
				dialog.open();
				lblUser.setText(manager.getUsername());
			}
		});

		btnCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (syncer != null) {
					syncer.cancel();
					syncer = null;
				}
			}
		});

		status = STATUS.PREINIT;
		espManager();

		return container;
	}

	private void espManager() {
		progressBar.redraw();
		switch (status) {
		case PREINIT:
			btnSettings.setEnabled(true);
			btnRefresh.setEnabled(true);
			tblAlbums.setEnabled(false);
			btnSync.setEnabled(false);
			btnCancel.setEnabled(false);
			break;
		case IDLE:
			btnSettings.setEnabled(true);
			btnRefresh.setEnabled(true);
			tblAlbums.setEnabled(true);
			btnSync.setEnabled(true);
			btnCancel.setEnabled(false);
			break;
		case REFRESH:
			btnSettings.setEnabled(false);
			btnRefresh.setEnabled(false);
			tblAlbums.setEnabled(false);
			btnSync.setEnabled(false);
			btnCancel.setEnabled(true);
			break;
		case SYNC:
			btnSettings.setEnabled(false);
			tblAlbums.setEnabled(false);
			btnSync.setEnabled(false);
			btnRefresh.setEnabled(false);
			btnCancel.setEnabled(true);
			break;
		}
	}

	private void doRefresh() {
		status = STATUS.REFRESH;
		espManager();

		final String userName = manager.getUsername();
		final String password = manager.getPassword();
		final String directory = manager.getDirectory();

		progressBar.setSelection(50);
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					progressBarString = "refreshing";
					syncer = new Synchronizer(directory, userName, password,
							new Notif(), new GuiCallback());
					syncer.getWebAndLocalAlbums();
				} catch (final AuthenticationException e) {
					e.printStackTrace();

					getShell().getDisplay().asyncExec(new Runnable() {
						public void run() {
							AuthErrorDialog dlg = new AuthErrorDialog(
									getShell(), "Error", e.getMessage());
							dlg.open();
						}
					});

				} catch (IOException e) {
					e.printStackTrace();
				} catch (ServiceException e) {
					e.printStackTrace();
				} finally {
					progressBarString = null;
					getShell().getDisplay().asyncExec(new Runnable() {
						public void run() {
							status = STATUS.IDLE;
							espManager();
						}
					});
				}
			}

		}).start();

	}

	/**
	 * Configure the shell.
	 * 
	 * @param newShell
	 */
	@Override
	protected void configureShell(Shell newShell) {
		newShell.setImage(SWTResourceManager.getImage(WorkbenchWindow.class,
				"/com/braids/ronaisync/resources/logo256.png"));
		super.configureShell(newShell);
		newShell.setText("Rónai Sync");
	}

	/**
	 * Return the initial size of the window.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(419, 431);
	}

	private class Notif implements SyncNotification {

		private int progressIndicator;
		private String albumName;

		public void albums(List<String> lstAlbumNames) {
		}

		public void startAlbumSync(final String albumName,
				final int albumIndex, final int photoCount) {
			this.albumName = albumName;
		}

		public void startPhotoSync(String photoName, int photoIndex) {
			// prgPhotos.setValue(photoIndex + 1);
			progressBarString = albumName + " : " + photoName;
		}

		@Override
		public void webLocalAlbums(List<AlbumEntry> lstWebAlbums,
				List<String> lstLocalAlbums) {

			final ArrayList<AlbumListItem> lstAlbums = new ArrayList<AlbumListItem>();

			for (AlbumEntry album : lstWebAlbums) {
				AlbumListItem li = new AlbumListItem();
				li.setName(album.getTitle().getPlainText());
				li.setType(Type.SERVER_ONLY);
				lstAlbums.add(li);
			}

			for (String localAlbumName : lstLocalAlbums) {
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

			getShell().getDisplay().syncExec(new Runnable() {
				public void run() {
					checkboxTableViewer.setInput(lstAlbums);
					progressBar.setSelection(0);
				}
			});

		}

		@Override
		public void bandwidth(int bandwidth) {
			Shell shell = getShell();
			if (shell != null) {

				Display disp = shell.getDisplay();
				if (disp != null) {
					disp.asyncExec(new Runnable() {
						public void run() {
							progressBar.setSelection(progressIndicator);
							progressIndicator += 10;
							if (progressIndicator > 90) {
								progressIndicator = 10;
							}

						}
					});
				}
			}
		}

	}

	private class GuiCallback implements SyncGuiCallback {

		@Override
		public boolean errorUploadingAPhoto(final File f, final Exception ex) {
			getShell().getDisplay().syncExec(new Runnable() {
				public void run() {
					errorDialogWithStackTrace("Error uploading",
							"File: " + f.getAbsolutePath(), ex);
				}
			});
			return true;
		}
	}

	public static void errorDialogWithStackTrace(String title, String msg,
			Throwable t) {

		t.printStackTrace();

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);

		final String trace = sw.toString();

		List<Status> childStatuses = new ArrayList<Status>();

		for (String line : trace.split(System.getProperty("line.separator"))) {
			childStatuses.add(new Status(IStatus.ERROR, "plugin id", line));
		}

		MultiStatus ms = new MultiStatus("plugin id", IStatus.ERROR,
				childStatuses.toArray(new Status[] {}),
				t.getLocalizedMessage(), t);

		ErrorDialog.openError(null, title, msg, ms);
	}
}
