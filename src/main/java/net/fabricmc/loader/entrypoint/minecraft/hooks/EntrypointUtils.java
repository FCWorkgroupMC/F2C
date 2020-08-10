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

package net.fabricmc.loader.entrypoint.minecraft.hooks;

import io.github.fcworkgroupmc.f2c.f2c.fabric.FabricLoader; // F2C - reimplement net.fabricmc.loader.api.FabricLoader and delete net.fabricmc.loader.FabricLoader
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

import java.util.Collection;
import java.util.function.Consumer;

public final class EntrypointUtils {
	public static <T> void invoke(String name, Class<T> type, Consumer<? super T> invoker) {
		FabricLoader loader = FabricLoader.INSTANCE;

		if (!loader.hasEntrypoints(name)) {
			loader.getLogger().debug("No subscribers for entrypoint '" + name + "'");
		} else {
			invoke0(name, type, invoker);
		}
	}

	private static <T> void invoke0(String name, Class<T> type, Consumer<? super T> invoker) {
		FabricLoader loader = FabricLoader.INSTANCE;
		RuntimeException exception = null;
		Collection<EntrypointContainer<T>> entrypoints = loader.getEntrypointContainers(name, type);

		loader.getLogger().debug("Iterating over entrypoint '" + name + "'");

		for (EntrypointContainer<T> container : entrypoints) {
			try {
				invoker.accept(container.getEntrypoint());
			} catch (Throwable t) {
				if (exception == null) {
					exception = new RuntimeException("Could not execute entrypoint stage '" + name + "' due to errors, provided by '" + container.getProvider().getMetadata().getId() + "'!", t);
				} else {
					exception.addSuppressed(t);
				}
			}
		}

		if (exception != null) {
			throw exception;
		}
	}
}
