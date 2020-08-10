/*
 * Copyright 2016 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 *  Copyright (C) 2020  FCWorkgroupMC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.fabricmc.loader.gui;

import java.awt.GraphicsEnvironment;
import java.util.HashSet;
import java.util.Set;

import io.github.fcworkgroupmc.f2c.f2c.fabric.FabricLoader; // F2C - reimplement net.fabricmc.loader.api.FabricLoader and delete net.fabricmc.loader.FabricLoader
import net.fabricmc.loader.game.GameProvider;
import net.fabricmc.loader.gui.FabricStatusTree.FabricStatusNode;
import net.fabricmc.loader.gui.FabricStatusTree.FabricStatusTab;

/** The main entry point for all fabric-based stuff. */
public final class FabricGuiEntry {
	/** Opens the given {@link FabricStatusTree} in a new swing window.
	 * 
	 * @throws Exception if something went wrong while opening the window. */
	public static void open(FabricStatusTree tree) throws Exception {
		openWindow(tree, true);
	}

	private static void openWindow(FabricStatusTree tree, boolean shouldWait) throws Exception {
		FabricMainWindow.open(tree, shouldWait);
	}

	/** @param exitAfter If true then this will call {@link System#exit(int)} after showing the gui, otherwise this will
	 *            return normally. */
	public static void displayCriticalError(Throwable exception, boolean exitAfter) {
		FabricLoader.INSTANCE.getLogger().fatal("A critical error occurred", exception);

		GameProvider provider = FabricLoader.INSTANCE.getGameProvider();

		if ((provider == null || provider.canOpenErrorGui()) && !GraphicsEnvironment.isHeadless()) {
			FabricStatusTree tree = new FabricStatusTree();
			FabricStatusTab crashTab = tree.addTab("Crash");

			tree.mainText = "Failed to launch!";
			addThrowable(crashTab.node, exception, new HashSet<>());

			// Maybe add an "open mods folder" button?
			// or should that be part of the main tree's right-click menu?
			tree.addButton("Exit").makeClose();

			try {
				open(tree);
			} catch (Exception e) {
				if (exitAfter) {
					FabricLoader.INSTANCE.getLogger().warn("Failed to open the error gui!", e);
				} else {
					throw new RuntimeException("Failed to open the error gui!", e);
				}
			}
		}

		if (exitAfter) {
			System.exit(1);
		}
	}

	private static void addThrowable(FabricStatusNode node, Throwable e, Set<Throwable> seen) {
		if (!seen.add(e)) {
			return;
		}

		// Remove some self-repeating exception traces from the tree
		// (for example the RuntimeException that is is created unnecessarily by ForkJoinTask)
		Throwable cause;

		while ((cause = e.getCause()) != null) {
			if (e.getSuppressed().length > 0) {
				break;
			}

			String msg = e.getMessage();

			if (msg == null) {
				msg = e.getClass().getName();
			}

			if (!msg.equals(cause.getMessage()) && !msg.equals(cause.toString())) {
				break;
			}

			e = cause;
		}

		FabricStatusNode sub = node.addException(e);

		if (e.getCause() != null) {
			addThrowable(sub, e.getCause(), seen);
		}

		for (Throwable t : e.getSuppressed()) {
			addThrowable(sub, t, seen);
		}
	}
}
