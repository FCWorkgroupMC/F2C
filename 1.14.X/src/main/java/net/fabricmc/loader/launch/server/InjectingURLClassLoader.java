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

package net.fabricmc.loader.launch.server;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

class InjectingURLClassLoader extends URLClassLoader {
	private final List<String> exclusions;

	InjectingURLClassLoader(URL[] urls, ClassLoader classLoader, String... exclusions) {
		super(urls, classLoader);
		this.exclusions  = Arrays.asList(exclusions);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			Class c = findLoadedClass(name);

			if (c == null) {
				boolean excluded = false;
				for (String s : exclusions) {
					if (name.startsWith(s)) {
						excluded = true;
						break;
					}
				}

				if (!excluded) {
					try {
						c = findClass(name);
					} catch (ClassNotFoundException e) {
						// pass
					}
				}
			}

			if (c == null) {
				c = getParent().loadClass(name);
			}

			if (c == null) {
				throw new ClassNotFoundException(name);
			}

			if (resolve) {
				resolveClass(c);
			}

			return c;
		}
	}
}
