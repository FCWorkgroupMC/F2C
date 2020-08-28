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

import io.github.fcworkgroupmc.f2c.f2c.Metadata;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.game.GameProvider;
import net.fabricmc.loader.game.GameProviders;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.fabricmc.loader.transformer.FabricTransformer;
import net.fabricmc.loader.util.UrlConversionException;
import net.fabricmc.loader.util.UrlUtil;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public final class Knot extends FabricLauncherBase {
	protected Map<String, Object> properties = new HashMap<>();

//	private KnotClassLoaderInterface classLoader; // F2C - don't need fabric's ClassLoader
	private boolean isDevelopment;
	private EnvType envType;
	private final File gameJarFile;
	private GameProvider provider;

	public Knot(EnvType type, File gameJarFile) { // F2C - change access to public
		this.envType = type;
		this.gameJarFile = gameJarFile;
	}

	public void init() { // F2C - change access to public
		setProperties(properties);

		// configure fabric vars
		if (envType == null) {
			/*String side = System.getProperty("fabric.side");
			if (side == null) {
				throw new RuntimeException("Please specify side or use a dedicated Knot!");
			}

			switch (side.toLowerCase(Locale.ROOT)) {
				case "client":
					envType = EnvType.CLIENT;
					break;
				case "server":
					envType = EnvType.SERVER;
					break;
				default:
					throw new RuntimeException("Invalid side provided: must be \"client\" or \"server\"!");
			}*/ // F2C - throw an exception instead of getting side from system property when envType is null
			throw new RuntimeException("envType cannot be null!");
		}

		// TODO: Restore these undocumented features
		// String proposedEntrypoint = System.getProperty("fabric.loader.entrypoint");

		List<GameProvider> providers = GameProviders.create();
		provider = null;

		for (GameProvider p : providers) {
			if (p.locateGame(envType, this.getClass().getClassLoader())) {
				provider = p;
				break;
			}
		}

		if (provider != null) {
			LOGGER.info("Loading for game " + provider.getGameName() + " " + provider.getRawGameVersion());
		} else {
			LOGGER.error("Could not find valid game provider!");
			for (GameProvider p : providers) {
				LOGGER.error("- " + p.getGameName()+ " " + p.getRawGameVersion());
			}
			throw new RuntimeException("Could not find valid game provider!");
		}

//		provider.acceptArguments(args); // F2C - remove redundant method

//		isDevelopment = Boolean.parseBoolean(System.getProperty("fabric.development", "false"));
		// F2C - use a custom value
		isDevelopment = Metadata.DEV;

		// Setup classloader
		// TODO: Provide KnotCompatibilityClassLoader in non-exclusive-Fabric pre-1.13 environments?
//		boolean useCompatibility = provider.requiresUrlClassLoader() || Boolean.parseBoolean(System.getProperty("fabric.loader.useCompatibilityClassLoader", "false"));
//		classLoader = new KnotClassLoader(isDevelopment(), envType, provider); // F2C - Don't need KnotCompatibilityClassLoader and KnotClassLoader
//		ClassLoader cl = (ClassLoader) classLoader; // F2C - Remove classloader setup

		/*if (provider.isObfuscated()) {
			for (Path path : provider.getGameContextJars()) {
				FabricLauncherBase.deobfuscate(
					provider.getGameId(), provider.getNormalizedGameVersion(),
					provider.getLaunchDirectory(),
					path,
					this
				);
			}
		}*/ // F2C - Remove this

		// Locate entrypoints before switching class loaders
//		provider.getEntrypointTransformer().locateEntrypoints(this); // F2C - Remove EntrypointTransformer and use ModLauncher's ITransformer instead

//		Thread.currentThread().setContextClassLoader(cl); // F2C - Remove classloader setup

		// F2C - reimplement net.fabricmc.loader.api.FabricLoader and delete net.fabricmc.loader.FabricLoader
		/*FabricLoader loader = FabricLoader.INSTANCE;
		loader.setGameProvider(provider);
		loader.loadMods();
		loader.endModLoading();

		FabricLoader.INSTANCE.getAccessWidener().loadFromMods();

		/*MixinBootstrap.init();
		FabricMixinBootstrap.init(getEnvironmentType(), loader);
		FabricLauncherBase.finishMixinBootstrapping();

		classLoader.getDelegate().initializeTransformers();

		EntrypointUtils.invoke("preLaunch", PreLaunchEntrypoint.class, PreLaunchEntrypoint::onPreLaunch);*/ // F2C - move to another place

//		provider.launch(cl); // F2C - remove redundant method
	}

	@Override
	public String getTargetNamespace() {
		// TODO: Won't work outside of Yarn
//		return isDevelopment ? "named" : "intermediary"; // F2C - remove redundant field
		return "intermediary"; // F2C - always intermediary
	}

	@Override
	public Collection<URL> getLoadTimeDependencies() {
		String cmdLineClasspath = System.getProperty("java.class.path");

		return Arrays.stream(cmdLineClasspath.split(File.pathSeparator)).filter((s) -> {
			if (s.equals("*") || s.endsWith(File.separator + "*")) {
				System.err.println("WARNING: Knot does not support wildcard classpath entries: " + s + " - the game may not load properly!");
				return false;
			} else {
				return true;
			}
		}).map((s) -> {
			File file = new File(s);
			if (!file.equals(gameJarFile)) {
				try {
					return (UrlUtil.asUrl(file));
				} catch (UrlConversionException e) {
					LOGGER.debug(e);
					return null;
				}
			} else {
				return null;
			}
		}).filter(Objects::nonNull).collect(Collectors.toSet());
	}

	@Override
	public void propose(URL url) {
		FabricLauncherBase.LOGGER.debug("[Knot/F2C] Proposed " + url + " to classpath.");
//		classLoader.addURL(url);
		// F2C - do nothing
	}

	@Override
	public EnvType getEnvironmentType() {
		return envType;
	}

	@Override
	public boolean isClassLoaded(String name) {
//		return classLoader.isClassLoaded(name);
		return FMLLoader.getLaunchClassLoader().getLoadedClass(name) != null; // F2C - Use modlauncher's TransformingClassLoader
	}

	@Override
	public InputStream getResourceAsStream(String name) {
//		try {
//			return classLoader.getResourceAsStream(name, false);
			return FMLLoader.getLaunchClassLoader().getResourceAsStream(name); // F2C - Use modlauncher's TransformingClassLoader
//		} catch (IOException e) {
//			throw new RuntimeException("Failed to read file '" + name + "'!", e);
//		} // F2C - Remove unused try-catch block
	}

	@Override
	public ClassLoader getTargetClassLoader() {
//		return (ClassLoader) classLoader;
		// F2C - Use modlauncher's TransformingClassLoader
		if(FMLLoader.getLaunchClassLoader() != null)
			return FMLLoader.getLaunchClassLoader().getInstance();
		return Thread.currentThread().getContextClassLoader();
	}

	@Override
	public byte[] getClassByteArray(String name, boolean runTransformers) throws IOException {
		// F2C - Use modlauncher's TransformingClassLoader
		byte[] bytes = new byte[0];
		try(InputStream stream = FMLLoader.getLaunchClassLoader().getResourceAsStream(name.replace('.', '/').concat(".class"))) {
			if (stream != null) {
				bytes = IOUtils.toByteArray(stream);
			}
		}
		if (runTransformers) {
			if(bytes != null) return FabricTransformer.transform(isDevelopment(), envType, name.replace('/', '.'), bytes);
//			return classLoader.getDelegate().getPreMixinClassByteArray(name, false);
		} else {
			return bytes;
//			return classLoader.getDelegate().getRawClassByteArray(name, false);
		}
		return null;
	}

	@Override
	public boolean isDevelopment() {
		return isDevelopment;
	}

	@Override
	public String getEntrypoint() {
		return provider.getEntrypoint();
	}

	@Override
	public GameProvider getGameProvider() { // F2C - add method
		return provider;
	}

	/*public static void main(String[] args) {
		new Knot(null, null).init(args);
	}*/ // F2C - remove redundant method
}
