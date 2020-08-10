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

import com.google.gson.Gson;
import cpw.mods.modlauncher.ArgumentHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import joptsimple.OptionSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.metadata.BuiltinModMetadata;
import net.fabricmc.loader.minecraft.McVersionLookup;
import net.fabricmc.loader.minecraft.McVersionLookup.McVersion;
import net.minecraftforge.fml.loading.FMLPaths;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

//import net.fabricmc.loader.entrypoint.EntrypointTransformer; // F2C - Remove EntrypointTransformer and use ModLauncher's ITransformer instead
//import net.fabricmc.loader.entrypoint.minecraft.EntrypointPatchBranding; // F2C - Remove EntrypointTransformer and use ModLauncher's ITransformer instead
//import net.fabricmc.loader.entrypoint.minecraft.EntrypointPatchHook; // F2C - Remove EntrypointTransformer and use ModLauncher's ITransformer instead
//import net.fabricmc.loader.entrypoint.minecraft.EntrypointPatchFML125; // F2C - remove outdated support
//import net.fabricmc.loader.util.Arguments; // F2C - remove redundant class

public class MinecraftGameProvider implements GameProvider {
	private static final Gson GSON = new Gson();

	private EnvType envType;
	private String entrypoint;
//	private Arguments arguments; // F2C - remove redundant field
	private Path gameJar, realmsJar;
	private McVersion versionData;
//	private boolean hasModLoader = false; // F2C - remove redundant field

	/*public static final EntrypointTransformer TRANSFORMER = new EntrypointTransformer(it -> Arrays.asList(
		new EntrypointPatchHook(it),
		new EntrypointPatchBranding(it)
//			, new EntrypointPatchFML125(it) // F2C - remove outdated support
	));*/ // F2C - Remove EntrypointTransformer and use ModLauncher's ITransformer instead

	@Override
	public String getGameId() {
		return "minecraft";
	}

	@Override
	public String getGameName() {
		return "Minecraft";
	}

	@Override
	public String getRawGameVersion() {
		return versionData.raw;
	}

	@Override
	public String getNormalizedGameVersion() {
		return versionData.normalized;
	}

	@Override
	public Collection<BuiltinMod> getBuiltinMods() {
		URL url;

		try {
			url = gameJar.toUri().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

		return Arrays.asList(
			new BuiltinMod(url, new BuiltinModMetadata.Builder(getGameId(), getNormalizedGameVersion())
				.setName(getGameName())
				.build())
		);
	}

	@Override
	public String getEntrypoint() {
		return entrypoint;
	}

	@Override
	public Path getLaunchDirectory() {
		/*if (arguments == null) {
			return new File(".").toPath();
		}

		return FabricLauncherBase.getLaunchDirectory(arguments).toPath();*/ // F2C - rewrite method
		return Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.GAMEDIR.get()).orElse(FMLPaths.GAMEDIR.get() == null ?
				Paths.get(".").toAbsolutePath().normalize() : FMLPaths.GAMEDIR.get());
	}

	@Override
	public boolean isObfuscated() {
//		return true; // generally yes...
		return false; // F2C - always false
	}

	/*@Override
	public boolean requiresUrlClassLoader() {
		return hasModLoader; // F2C - remove redundant field
	}*/ // F2C - remove redundant method

	@Override
	public List<Path> getGameContextJars() {
		List<Path> list = new ArrayList<>();
		list.add(gameJar);
		if (realmsJar != null) {
			list.add(realmsJar);
		}
		return list;
	}

	@Override
	public boolean locateGame(EnvType envType, ClassLoader loader) {
		this.envType = envType;
		List<String> entrypointClasses;

		if (envType == EnvType.CLIENT) {
			entrypointClasses = Collections.singletonList("net.minecraft.client.main.Main"); // F2C - Replace with Collections.singletonList()
//	            Arrays.asList("net.minecraft.client.main.Main", "net.minecraft.client.MinecraftApplet", "com.mojang.minecraft.MinecraftApplet"); // F2C - Remove outdated entrypoint classes
		} else {
			entrypointClasses = Arrays.asList("net.minecraft.server.Main", "net.minecraft.server.MinecraftServer"
//					, "com.mojang.minecraft.server.MinecraftServer" // F2C - Remove outdated entrypoint class
			);
		}

		Optional<GameProviderHelper.EntrypointResult> entrypointResult = GameProviderHelper.findFirstClass(loader, entrypointClasses);
		if (!entrypointResult.isPresent()) {
			return false;
		}

		entrypoint = entrypointResult.get().entrypointName;
		gameJar = entrypointResult.get().entrypointPath;
		realmsJar = GameProviderHelper.getSource(loader, "realmsVersion").orElse(null);
//		hasModLoader = GameProviderHelper.getSource(loader, "ModLoader.class").isPresent(); // F2C - remove redundant field
		versionData = McVersionLookup.getVersion(gameJar);

		return true;
	}

	/*@Override
	public void acceptArguments(String... argStrs) {
		this.arguments = new Arguments();
		arguments.parse(argStrs);

		FabricLauncherBase.processArgumentMap(arguments, envType);
	}*/ // F2C - remove redundant method

	/*@Override
	public EntrypointTransformer getEntrypointTransformer() {
		return TRANSFORMER;
	}*/ // F2C - Remove EntrypointTransformer and use ModLauncher's ITransformer instead

	@Override
	public boolean canOpenErrorGui() {
		// Disabled on macs due to -XstartOnFirstThread being incompatible with awt but required for lwjgl
		if (System.getProperty("os.name").equals("Mac OS X")) {
			return false;
		}

		if (/*arguments == null ||*/ envType == EnvType.CLIENT) {
			return true;
		}

		/*List<String> extras = arguments.getExtraArgs();
		return !extras.contains("nogui") && !extras.contains("--nogui");*/ // F2C - rewrite
		OptionSet set = null;
		try {
			Field setF = ArgumentHandler.class.getDeclaredField("nonOption");
			setF.setAccessible(true);
			Field handler = Launcher.class.getDeclaredField("argumentHandler");
			handler.setAccessible(true);
			set = (OptionSet) setF.get(handler.get(Launcher.INSTANCE));
		} catch (NoSuchFieldException | IllegalAccessException e) {}
		return set.has("nogui");
	}

	/*@Override
	public void launch(ClassLoader loader) {
		String targetClass = entrypoint;

		if (envType == EnvType.CLIENT && targetClass.contains("Applet")) {
			targetClass = "net.fabricmc.loader.entrypoint.applet.AppletMain";
		}

		try {
			Class<?> c = loader.loadClass(targetClass);
			Method m = c.getMethod("main", String[].class);
			m.invoke(null, (Object) arguments.toArray());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}*/ // F2C - remove redundant method
}
