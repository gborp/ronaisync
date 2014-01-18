package com.braids.ronaisync;

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
public class RonaiSyncMain {

	public static void main(String[] args) throws Exception {

		String baseDirectory = args[0];
		String user = args[1];
		String password = args[2];
		String verbose = args[3];
		GnomeSyncNotification callback = new GnomeSyncNotification();
		new Synchronizer(baseDirectory, user, password,
				"verbose".equalsIgnoreCase(verbose), callback).sync();

		//
		// public boolean isGnome() {
		// String var = System.getenv(GNOME_ENV_VAR);
		//
		// return var != null && !var.trim().equals("");
		// }
		//
		// public boolean isXfce() {
		// String var = System.getenv(XFCE_ENV_VAR);
		//
		// return var != null && var.trim().equals("xfce-");
		// }
	}

}
