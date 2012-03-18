package com.braids.ronaisync;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class SettingsDialog extends Dialog {

	protected Object result;
	protected Shell shell;
	private Text sfName;
	private Text sfPassword;
	private Text sfDir;
	private Button btnDir;
	private final Manager manager;
	private Button btnSave;

	/**
	 * Create the dialog.
	 * 
	 * @param parent
	 * @param style
	 */
	public SettingsDialog(Shell parent, int style, Manager manager) {
		super(parent, style);
		this.manager = manager;
		setText("Settings");
	}

	/**
	 * Open the dialog.
	 * 
	 * @return the result
	 */
	public Object open() {
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.RESIZE);
		shell.setMinimumSize(new Point(424, 180));
		shell.setSize(424, 180);
		shell.setText(getText());

		Composite container = shell;
		FormLayout fl_shell = new FormLayout();
		fl_shell.marginWidth = 4;
		fl_shell.marginTop = 4;
		fl_shell.marginRight = 4;
		fl_shell.marginLeft = 4;
		fl_shell.marginHeight = 4;
		fl_shell.marginBottom = 4;
		container.setLayout(fl_shell);
		sfName = new Text(container, SWT.BORDER);
		FormData fd_sfName = new FormData();
		fd_sfName.right = new FormAttachment(100, -6);
		fd_sfName.top = new FormAttachment(0, 1);
		fd_sfName.left = new FormAttachment(0, 70);
		sfName.setLayoutData(fd_sfName);

		sfPassword = new Text(container, SWT.PASSWORD | SWT.BORDER);
		FormData fd_sfPassword = new FormData();
		fd_sfPassword.top = new FormAttachment(0, 34);
		fd_sfPassword.right = new FormAttachment(100, -6);
		sfPassword.setLayoutData(fd_sfPassword);

		sfDir = new Text(container, SWT.BORDER);
		FormData fd_sfDir = new FormData();
		fd_sfDir.left = new FormAttachment(0, 71);
		fd_sfDir.top = new FormAttachment(0, 74);
		sfDir.setLayoutData(fd_sfDir);

		btnDir = new Button(container, SWT.NONE);
		fd_sfDir.right = new FormAttachment(btnDir, -194);
		fd_sfPassword.right = new FormAttachment(btnDir, -6);
		btnDir.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DirectoryDialog dialog = new DirectoryDialog(shell);
				// dialog.setFilterPath("c:\\");

				String result = dialog.open();
				if (result != null) {
					sfDir.setText(result);
				}
			}
		});
		FormData fd_btnDir = new FormData();
		fd_btnDir.bottom = new FormAttachment(sfDir, 0, SWT.BOTTOM);
		fd_btnDir.right = new FormAttachment(100);
		fd_btnDir.top = new FormAttachment(0, 74);
		fd_btnDir.left = new FormAttachment(0, 333);
		btnDir.setLayoutData(fd_btnDir);
		btnDir.setText("dir");

		DragSource dragSource = new DragSource(sfDir, DND.DROP_MOVE);

		Label lblName = new Label(container, SWT.RIGHT);
		FormData fd_lblName = new FormData();
		fd_lblName.left = new FormAttachment(0);
		fd_lblName.top = new FormAttachment(0, 10);
		lblName.setLayoutData(fd_lblName);
		lblName.setText("Username");

		Label lblPassword = new Label(container, SWT.NONE);
		fd_sfPassword.left = new FormAttachment(0, 70);
		lblPassword.setText("Password");
		FormData fd_lblPassword = new FormData();
		fd_lblPassword.top = new FormAttachment(lblName, 7);
		fd_lblPassword.left = new FormAttachment(0);
		lblPassword.setLayoutData(fd_lblPassword);

		sfName.setText(manager.getUsername());
		sfPassword.setText(manager.getPassword());
		sfDir.setText(manager.getDirectory());

		Label lblFolder = new Label(container, SWT.RIGHT);
		lblFolder.setText("Folder");
		FormData fd_lblFolder = new FormData();
		fd_lblFolder.left = new FormAttachment(0, 10);
		lblFolder.setLayoutData(fd_lblFolder);
		fd_lblFolder.top = new FormAttachment(lblPassword, 23);
		fd_lblPassword.right = new FormAttachment(lblFolder, 0, SWT.RIGHT);

		btnSave = new Button(shell, SWT.NONE);
		FormData fd_btnSave = new FormData();
		fd_btnSave.bottom = new FormAttachment(100);
		fd_btnSave.left = new FormAttachment(0, 170);
		btnSave.setLayoutData(fd_btnSave);
		btnSave.setText("Save");
		btnSave.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				doSave();
			}
		});
	}

	private void doSave() {
		manager.setUsername(sfName.getText());
		manager.setPassword(sfPassword.getText());
		manager.setDirectory(sfDir.getText());
		shell.dispose();
	}

}
