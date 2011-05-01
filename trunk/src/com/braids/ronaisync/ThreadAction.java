package com.braids.ronaisync;

import java.awt.event.ActionEvent;

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
public abstract class ThreadAction extends ExceptionAction {

	public ThreadAction(String string) {
		super(string);
	}

	public void actionPerformed(final ActionEvent e) {
		new Thread(new Runnable() {

			public void run() {
				ThreadAction.super.actionPerformed(e);
			}

		}).start();
	}
}
