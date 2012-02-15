package com.braids.ronaisync.ux;

import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class GuiHelper {

	public static final long REVALIDATE_DELAY = 500;
	public static final long TABLE_RESIZE_DELAY = 50;
	private static SettingsStore settingsStore;

	private static boolean guiInitialized;
	private static ArrayList<Image> lstApplicationIcons;
	private static ExceptionHandler exceptionHandler;

	public static void initGui() {
		if (!guiInitialized) {
			try {
				UIManager.setLookAndFeel(UIManager
						.getSystemLookAndFeelClassName());
			} catch (Exception e) {
			}

			lstApplicationIcons = new ArrayList<Image>();
			lstApplicationIcons.add(GuiHelper
					.loadImage("/com/braids/ronaisync/resources/logo16.png"));
			lstApplicationIcons.add(GuiHelper
					.loadImage("/com/braids/ronaisync/resources/logo32.png"));
			lstApplicationIcons.add(GuiHelper
					.loadImage("/com/braids/ronaisync/resources/logo64.png"));
			lstApplicationIcons.add(GuiHelper
					.loadImage("/com/braids/ronaisync/resources/logo128.png"));
			lstApplicationIcons.add(GuiHelper
					.loadImage("/com/braids/ronaisync/resources/logo256.png"));

			try {
				getSettings().load();
			} catch (BackingStoreException e) {
				e.printStackTrace();
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			guiInitialized = true;
		}
	}

	public static SettingsStore getSettings() {
		if (settingsStore == null) {
			settingsStore = new SettingsStore();
		}
		return settingsStore;
	}

	public static void handle(Exception ex) {
		if (exceptionHandler == null) {
			exceptionHandler = new ExceptionHandler();
		}
		exceptionHandler.handle(ex);
	}

	public static void addUndoRedo(JTextComponent comp) {
		final UndoManager undo = new UndoManager();
		Document doc = comp.getDocument();
		doc.addUndoableEditListener(new UndoableEditListener() {

			public void undoableEditHappened(UndoableEditEvent evt) {
				undo.addEdit(evt.getEdit());
			}
		});
		ActionMap actionMap = comp.getActionMap();
		InputMap inputMap = comp.getInputMap();
		actionMap.put("Undo", new AbstractAction("Undo") {

			public void actionPerformed(ActionEvent evt) {
				try {
					if (undo.canUndo()) {
						undo.undo();
					}
				} catch (CannotUndoException e) {
				}
			}
		});
		inputMap.put(KeyStroke.getKeyStroke("control Z"), "Undo");
		actionMap.put("Redo", new AbstractAction("Redo") {

			public void actionPerformed(ActionEvent evt) {
				try {
					if (undo.canRedo()) {
						undo.redo();
					}
				} catch (CannotRedoException e) {
				}
			}
		});
		inputMap.put(KeyStroke.getKeyStroke("control Y"), "Redo");
	}

	/**
	 * This method writes a string to the system clipboard.
	 */
	public static void setClipboard(String str) {
		StringSelection ss = new StringSelection(str);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
	}

	public static Image loadImage(String name) {
		return Toolkit.getDefaultToolkit().getImage(
				GuiHelper.class.getResource(name));
	}

	public static void addPopupMenu(JLabel component) {
		JPopupMenu contextMenu = new JPopupMenu();
		contextMenu.add(new CopyLabelAction(component));
		addPopupMenu(component, contextMenu);
	}

	public static void addPopupMenu(JTextComponent component) {
		JPopupMenu contextMenu = new JPopupMenu();
		contextMenu.add(new CutAction(component));
		contextMenu.add(new CopyAction(component));
		contextMenu.add(new PasteAction(component));
		addPopupMenu(component, contextMenu);
	}

	public static void addPopupMenu(JComponent component,
			final JPopupMenu contextMenu) {

		component.addMouseListener(new MouseAdapter() {

			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
			}

			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}

			private void maybeShowPopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					contextMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
	}

	private static class CopyLabelAction extends AbstractAction {

		private final JLabel target;

		public CopyLabelAction(JLabel target) {
			super("Copy");
			this.target = target;
		}

		public void actionPerformed(ActionEvent e) {
			setClipboard(target.getText());
		}
	}

	private static class CopyAction extends AbstractAction {

		private final JTextComponent target;

		public CopyAction(JTextComponent target) {
			super("Copy");
			this.target = target;
		}

		public void actionPerformed(ActionEvent e) {
			boolean deselectAfterCopy = false;
			if ((target.getSelectedText() == null) || !target.isEditable()) {
				target.selectAll();
				deselectAfterCopy = true;
			}
			target.copy();
			if (deselectAfterCopy) {
				target.select(0, 0);
			}
		}
	}

	private static class CutAction extends AbstractAction {

		private final JTextComponent target;

		public CutAction(JTextComponent target) {
			super("Cut");
			this.target = target;
		}

		public void actionPerformed(ActionEvent e) {
			boolean deselectAfterCopy = false;
			if (target.getSelectedText() == null) {
				target.selectAll();
				deselectAfterCopy = true;
			}
			if (target.isEditable()) {
				target.cut();
			} else {
				target.copy();
			}

			if (deselectAfterCopy) {
				target.select(0, 0);
			}
		}
	}

	private static class PasteAction extends AbstractAction {

		private final JTextComponent target;

		public PasteAction(JTextComponent target) {
			super("Paste");
			this.target = target;
		}

		public void actionPerformed(ActionEvent e) {
			target.paste();
		}
	}

	public static void centerScreen(Window window) {
		Dimension dim = window.getToolkit().getScreenSize();
		Rectangle bounds = window.getBounds();
		bounds.x = (dim.width - bounds.width) / 2;
		bounds.y = (dim.height - bounds.height) / 2;
		setBounds(window, bounds);
	}

	public static void centerOnParent(Window parent, Window child) {
		Rectangle dim;
		if (parent != null) {
			dim = new Rectangle(parent.getLocationOnScreen(), parent.getSize());
		} else {
			dim = new Rectangle(new Point(0, 0), Toolkit.getDefaultToolkit()
					.getScreenSize());
		}

		Rectangle bounds = child.getBounds();
		bounds.x = dim.x + (dim.width - bounds.width) / 2;
		bounds.y = dim.y + (dim.height - bounds.height) / 2;
		setBounds(child, bounds);
	}

	public static Frame createFrame(JComponent pnlMain, String applicationName,
			String iconName) {
		return createFrame(pnlMain, applicationName, iconName, true);
	}

	public static Frame createFrame(JComponent pnlMain, String applicationName,
			String iconName, boolean addScrollPane) {
		initGui();
		Frame frame = new Frame(applicationName, iconName);
		frame.getContentPane().add(addBorder(pnlMain, addScrollPane));
		frame.setTitle(applicationName + " [" + "RónaiSync" + "]");

		frame.addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent e) {
				e.getWindow().dispose();
			}
		});

		return frame;
	}

	public static Frame createAndShowFrame(JComponent pnlMain,
			String applicationName) {
		return createAndShowFrame(pnlMain, applicationName, null);
	}

	public static Frame createAndShowFrame(JComponent pnlMain,
			String applicationName, String iconName) {
		return createAndShowFrame(pnlMain, applicationName, iconName, true);
	}

	public static Frame createAndShowFrame(JComponent pnlMain,
			String applicationName, String iconName, boolean addScrollPane) {
		initGui();
		Frame frame = createFrame(pnlMain, applicationName, iconName,
				addScrollPane);
		frame.invalidate();
		frame.pack();
		Dimension prefSize = frame.getPreferredSize();
		frame.setMinimumSize(prefSize);

		Rectangle bounds = getSettings().getWindowBounds(applicationName);
		if (bounds != null) {
			setBounds(frame, bounds);
		} else {
			centerScreen(frame);
		}

		frame.addWindowListener(new WindowPreferencesSaverOnClose(null,
				applicationName));
		frame.setVisible(true);

		return frame;
	}

	public static JDialog createDialog(Window parent, JComponent main,
			String title) {
		initGui();
		final JDialog dialog = new JDialog(parent);
		dialog.getContentPane().add(addBorder(main, true));
		dialog.setTitle(title);
		dialog.setIconImages(lstApplicationIcons);
		dialog.pack();

		dialog.addWindowListener(new WindowPreferencesSaverOnClose(parent,
				title));

		dialog.getRootPane().registerKeyboardAction(new ActionListener() {

			public void actionPerformed(ActionEvent actionEvent) {
				dialog.setVisible(false);
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		Rectangle bounds = getSettings().getWindowBounds(parent, title);
		if (bounds != null) {
			setBounds(dialog, bounds);
		} else {
			centerOnParent(parent, dialog);
		}

		return dialog;
	}

	public static void setBounds(Window window, Rectangle bounds) {
		Dimension dim = window.getToolkit().getScreenSize();
		Dimension prefSize = window.getPreferredSize();

		if (bounds.width < prefSize.width) {
			bounds.width = prefSize.width;
		}
		if (bounds.height < prefSize.height) {
			bounds.height = prefSize.height;
		}

		if (bounds.x < 0) {
			bounds.x = 0;
		}
		if (bounds.y < 0) {
			bounds.y = 0;
		}
		if (bounds.x + bounds.width >= dim.width) {
			bounds.x = dim.width - bounds.width;
		}
		if (bounds.y + bounds.height >= dim.height) {
			bounds.y = dim.height - bounds.height;
		}
		if (bounds.x < 0) {
			bounds.x = 0;
		}
		if (bounds.y < 0) {
			bounds.y = 0;
		}
		if (bounds.x + bounds.width >= dim.width) {
			bounds.width = dim.width - bounds.x;
		}
		if (bounds.y + bounds.height >= dim.height) {
			bounds.height = dim.height - bounds.y;
		}

		window.setBounds(bounds);
	}

	public static void closeWindow(Window window) {
		AWTEvent windowEvenet = new WindowEvent(window,
				WindowEvent.WINDOW_CLOSING);
		window.dispatchEvent(windowEvenet);
		window.setVisible(false);
		window.dispose();
	}

	private static JComponent addBorder(JComponent pnlMain,
			boolean addScrollPane) {
		JPanel pnlBorder = new JPanel(new FormLayout("2dlu,fill:p:g,2dlu",
				"2dlu,fill:p:g,2dlu"));

		pnlBorder.add(pnlMain, new CellConstraints(2, 2));

		if (addScrollPane) {
			return new JScrollPane(pnlBorder);
		} else {
			return pnlBorder;
		}
	}

	public static List<? extends Image> getApplicationImages() {
		return lstApplicationIcons;
	}
}