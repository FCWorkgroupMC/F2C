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

package net.fabricmc.loader.launch.knot;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.game.GameProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.SecureClassLoader;
import java.util.Enumeration;
import java.util.Objects;

class KnotClassLoader extends SecureClassLoader implements KnotClassLoaderInterface {
	private static class DynamicURLClassLoader extends URLClassLoader {
		private DynamicURLClassLoader(URL[] urls) {
			super(urls, new DummyClassLoader());
		}

		@Override
		public void addURL(URL url) {
			super.addURL(url);
		}

		static {
			registerAsParallelCapable();
		}
	}

	private final DynamicURLClassLoader urlLoader;
	private final ClassLoader originalLoader;
	private final KnotClassDelegate delegate;

	KnotClassLoader(boolean isDevelopment, EnvType envType, GameProvider provider) {
		super(new DynamicURLClassLoader(new URL[0]));
		this.originalLoader = getClass().getClassLoader();
		this.urlLoader = (DynamicURLClassLoader) getParent();
		this.delegate = new KnotClassDelegate(isDevelopment, envType, this, provider);
	}

	@Override
	public KnotClassDelegate getDelegate() {
		return delegate;
	}

	@Override
	public boolean isClassLoaded(String name) {
		synchronized (getClassLoadingLock(name)) {
			return findLoadedClass(name) != null;
		}
	}

	@Override
	public URL getResource(String name) {
		Objects.requireNonNull(name);

		URL url = urlLoader.getResource(name);
		if (url == null) {
			url = originalLoader.getResource(name);
		}
		return url;
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		Objects.requireNonNull(name);

		InputStream inputStream = urlLoader.getResourceAsStream(name);
		if (inputStream == null) {
			inputStream = originalLoader.getResourceAsStream(name);
		}
		return inputStream;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		Objects.requireNonNull(name);

		Enumeration<URL> first = urlLoader.getResources(name);
		Enumeration<URL> second = originalLoader.getResources(name);
		return new Enumeration<URL>() {
			Enumeration<URL> current = first;

			@Override
			public boolean hasMoreElements() {
				if (current == null) {
					return false;
				}

				if (current.hasMoreElements()) {
					return true;
				}

				if (current == first && second.hasMoreElements()) {
					return true;
				}

				return false;
			}

			@Override
			public URL nextElement() {
				if (current == null) {
					return null;
				}

				if (!current.hasMoreElements()) {
					if (current == first) {
						current = second;
					} else {
						current = null;
						return null;
					}
				}

				return current.nextElement();
			}
		};
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			Class<?> c = findLoadedClass(name);

			if (c == null && !name.startsWith("com.google.gson.") && !name.startsWith("java.")) { // FIXME: remove the GSON exclusion once loader stops using it (or repackages it)
				byte[] input = delegate.getPostMixinClassByteArray(name);
				if (input != null) {
					KnotClassDelegate.Metadata metadata = delegate.getMetadata(name, urlLoader.getResource(delegate.getClassFileName(name)));

					int pkgDelimiterPos = name.lastIndexOf('.');
					if (pkgDelimiterPos > 0) {
						// TODO: package definition stub
						String pkgString = name.substring(0, pkgDelimiterPos);
						if (getPackage(pkgString) == null) {
							definePackage(pkgString, null, null, null, null, null, null, null);
						}
					}

					c = defineClass(name, input, 0, input.length, metadata.codeSource);
				}
			}

			if (c == null) {
				c = originalLoader.loadClass(name);
			}

			if (resolve) {
				resolveClass(c);
			}

			return c;
		}
	}

	@Override
	public void addURL(URL url) {
		urlLoader.addURL(url);
	}

	static {
		registerAsParallelCapable();
	}

	@Override
	public InputStream getResourceAsStream(String classFile, boolean skipOriginalLoader) throws IOException {
		InputStream inputStream = urlLoader.getResourceAsStream(classFile);
		if (inputStream == null && !skipOriginalLoader) {
			inputStream = originalLoader.getResourceAsStream(classFile);
		}
		return inputStream;
	}
}
