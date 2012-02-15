package com.braids.ronaisync.ux;

import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import com.braids.ronaisync.LogHelper;
import com.braids.ronaisync.ThreadAction;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

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
public class ExceptionHandler implements java.lang.Thread.UncaughtExceptionHandler {

	public void uncaughtException(Thread t, Throwable ex) {
		handle(ex);
	}

	public void handle(Throwable ex) {

//		if (Manager.isShutdown()) {
//			return;
//		}

		



		LogHelper.GENERAL.error(ex, ex);

		EditField sfMessage = new EditField();
		sfMessage.setEditable(false);
		TextArea taStacktrace = new TextArea();
		taStacktrace.setEditable(false);

		sfMessage.setText(ex.getMessage());

		StringWriter writer = new StringWriter();
		ex.printStackTrace(new PrintWriter(writer));
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		taStacktrace.setText(writer.getBuffer().toString());

		JButton btnRestart = new JButton(new RestartPagaVcsAction());

		CellConstraints cc = new CellConstraints();
		FormLayout lyTop = new FormLayout("f:1dlu:g,2dlu,p", "p");
		JPanel pnlTop = new JPanel(lyTop);
		pnlTop.add(sfMessage, cc.xy(1, 1));
		pnlTop.add(btnRestart, cc.xy(3, 1));

		FormLayout lyMain = new FormLayout("f:20dlu:g", "p,2dlu,p");
		JPanel pnlMain = new JPanel(lyMain);
		pnlMain.add(pnlTop, cc.xy(1, 1));
		pnlMain.add(new JScrollPane(taStacktrace), cc.xy(1, 3));

		GuiHelper.createAndShowFrame(pnlMain, "Exception occured");
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
				} catch (CannotUndoException e) {}
			}
		});
		inputMap.put(KeyStroke.getKeyStroke("control Z"), "Undo");
		actionMap.put("Redo", new AbstractAction("Redo") {

			public void actionPerformed(ActionEvent evt) {
				try {
					if (undo.canRedo()) {
						undo.redo();
					}
				} catch (CannotRedoException e) {}
			}
		});
		inputMap.put(KeyStroke.getKeyStroke("control Y"), "Redo");
	}
	private class RestartPagaVcsAction extends ThreadAction {

		public RestartPagaVcsAction() {
			super("Restart PagaVCS");
			setTooltip("Press refresh in nautilus after pressing this button");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			System.exit(0);
		}
	}
}
