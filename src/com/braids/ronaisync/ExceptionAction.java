package com.braids.ronaisync;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

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
public abstract class ExceptionAction extends AbstractAction {

	public ExceptionAction(String string) {
		super(string);
	}

	public void actionPerformed(final ActionEvent e) {
		try {
			actionProcess(e);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public void setLabel(String label) {
		putValue(Action.NAME, label);
	}

	public void setTooltip(String label) {
		putValue(Action.SHORT_DESCRIPTION, label);
	}

	public abstract void actionProcess(ActionEvent e) throws Exception;

}
