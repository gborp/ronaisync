package com.braids.ronaisync.ux;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

public class PleaseWaitDialog extends JDialog implements PropertyChangeListener {

	private static final long serialVersionUID = 3793555575340797825L;

	private Window parentWindow;
	private int runningProcesses;

	public PleaseWaitDialog(Window parent) {
		super(parent, "Waiting...");
		parentWindow = parent;
		JLabel lblWait = new JLabel("Please wait");
		lblWait.setBorder(new EmptyBorder(new Insets(20, 20, 20, 20)));
		setLayout(new BorderLayout());
		add(lblWait, BorderLayout.CENTER);
		pack();
	}

	public void waitFor(SwingWorker<?, ?> worker) {
		worker.addPropertyChangeListener(this);
	}

	public synchronized void propertyChange(PropertyChangeEvent event) {
		if ("state".equals(event.getPropertyName())) {
			if (SwingWorker.StateValue.STARTED == event.getNewValue()) {
				runningProcesses++;
			} else if (SwingWorker.StateValue.DONE == event.getNewValue()) {
				runningProcesses--;
			}
			SwingUtilities.invokeLater(new Runnable() {

				public void run() {
					if (runningProcesses > 0) {
						setVisible(true);
						GuiHelper.centerOnParent(parentWindow,
								PleaseWaitDialog.this);
					} else {
						setVisible(false);
					}
				}

			});

		}

	}
}
