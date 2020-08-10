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

package net.fabricmc.loader.game;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.metadata.ModMetadata;
//import net.fabricmc.loader.entrypoint.EntrypointTransformer; // F2C - Remove EntrypointTransformer and use ModLauncher's ITransformer instead

import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public interface GameProvider {
	String getGameId();
	String getGameName();
	String getRawGameVersion();
	String getNormalizedGameVersion();
	Collection<BuiltinMod> getBuiltinMods();

	String getEntrypoint();
	Path getLaunchDirectory();
	boolean isObfuscated();
//	boolean requiresUrlClassLoader(); // F2C - remove redundant method
	List<Path> getGameContextJars();

	boolean locateGame(EnvType envType, ClassLoader loader);
//	void acceptArguments(String... arguments); // F2C - remove redundant method
//	EntrypointTransformer getEntrypointTransformer(); // F2C - Remove EntrypointTransformer and use ModLauncher's ITransformer instead
//	void launch(ClassLoader loader); // F2C - remove redundant method

	default boolean canOpenErrorGui() {
		return true;
	}

	public static class BuiltinMod {
		public BuiltinMod(URL url, ModMetadata metadata) {
			this.url = url;
			this.metadata = metadata;
		}

		public final URL url;
		public final ModMetadata metadata;
	}
}
