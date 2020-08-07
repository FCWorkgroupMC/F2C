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

package io.github.fcworkgroupmc.f2c.f2c.transformationservices;

import cpw.mods.modlauncher.LaunchPluginHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.TransformingClassLoader;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import io.github.fcworkgroupmc.f2c.f2c.namemappingservices.IntermediaryToSrgNameMappingService;
import io.github.fcworkgroupmc.f2c.f2c.transformers.EntryPointBrandingTransformer;
import io.github.lxgaming.classloader.ClassLoaderUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.launch.knot.Knot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.Environment;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.platform.MixinPlatformManager;
import sun.misc.Unsafe;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class FabricModTransformationService implements ITransformationService {
	public static final String FABRIC_MOD_SUFFIX = ".fabricmod";
	public static final String JAR_SUFFIX = ".jar";
	/** fabric mod definition(fabric.mod.json) */
	public static final String FABRIC_MOD_DEF = "fabric.mod.json";
	private static final Logger LOGGER = LogManager.getLogger();

	private URL location;

	public static String mcVersion;
	public static FabricModTransformationService instance;
	public final List<Path> fabricMods = new ArrayList<>();
	@Nonnull
	@Override
	public String name() {
		return "f2c";
	}

	@Override
	public void initialize(IEnvironment environment) {
		try {
			Path modsDir = environment.getProperty(IEnvironment.Keys.GAMEDIR.get()).orElse(FMLPaths.GAMEDIR.get()).resolve(FMLPaths.MODSDIR.relative());
			if(Files.exists(modsDir)) {
				Files.walk(modsDir, 1).filter(path -> {
					String pathString = path.toAbsolutePath().normalize().toString();
					return (pathString.endsWith(JAR_SUFFIX) || pathString.endsWith(FABRIC_MOD_SUFFIX));
				}).forEach(modPath -> {
					ZipEntry entry = null;
					try(JarFile jarFile = new JarFile(modPath.toFile())) {
						entry = jarFile.getEntry(FABRIC_MOD_DEF);
					} catch (IOException e) {
						LOGGER.catching(Level.ERROR, e);
					}
					if(entry != null) {
						Path target = modPath.getParent().resolve(modPath.getFileName().toString().replace(JAR_SUFFIX, FABRIC_MOD_SUFFIX));
						try {
							Files.move(modPath, target);
						} catch (IOException e) {
							LOGGER.catching(Level.FATAL, e);
						}
						fabricMods.add(target);
						LOGGER.debug("Added mod: {}", target);
					}
				});
				if(location != null && location.getPath().endsWith(".jar"))
					fabricMods.add(Paths.get(location.toURI()));
				Runtime.getRuntime().addShutdownHook(new Thread(() -> fabricMods.forEach(p -> {
					try {
						Files.move(p, p.getParent().resolve(p.getFileName().toString().replace(FABRIC_MOD_SUFFIX, JAR_SUFFIX)));
					} catch (IOException e) {
						LOGGER.catching(Level.WARN, e);
					}
				})));
			} else LOGGER.warn("mods directory not present!");
		} catch (Exception e) {
			LOGGER.error("error occurred when initializing f2c service " + e);
		}
		try {
			Field mcVersionF = FMLLoader.class.getDeclaredField("mcVersion");
			mcVersionF.setAccessible(true);
			mcVersion = (String) mcVersionF.get(null);
		} catch (IllegalAccessException | NoSuchFieldException e) {
			LOGGER.fatal("Error when getting minecraft version", e);
		}
		Dist dist = environment.getProperty(Environment.Keys.DIST.get()).orElseThrow(IllegalArgumentException::new);
		new Knot(dist == Dist.DEDICATED_SERVER ? EnvType.SERVER : EnvType.CLIENT, FMLLoader.getMCPaths()[0].toFile()).init();
	}

	@Override
	public void beginScanning(IEnvironment environment) {
		if(location == null || !location.getPath().endsWith(".jar")) {
			try {
				Method addConnectorMethod = MixinPlatformManager.class.getDeclaredMethod("addConnector", String.class);
				addConnectorMethod.setAccessible(true);
				addConnectorMethod.invoke(MixinBootstrap.getPlatform(), "io.github.fcworkgroupmc.f2c.f2c.FabricMixinConnector");
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
				LOGGER.error("Couldn't add Mixin Connector to Mixin", e);
			}
		}

		IntermediaryToSrgNameMappingService.init(mcVersion);
	}

	@Override
	public List<Map.Entry<String, Path>> runScan(IEnvironment environment) {
		beginScanning(environment);
		if(!fabricMods.isEmpty()) {
			return fabricMods.stream().map(path -> new AbstractMap.SimpleImmutableEntry<>(path.getFileName().toString(), path)).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	@Override
	public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {
		instance = this;
		if(!otherServices.contains("fml") || (!otherServices.contains("mixin") && !otherServices.contains("mixinbootstrap")))
			throw new IncompatibleEnvironmentException(name() + " requires Forge and Mixin(or MixinBootstrap mod) to load");
		location = getClass().getProtectionDomain().getCodeSource().getLocation();
		if(location != null && location.getPath().endsWith(".jar")) {
			try {
				ClassLoaderUtils.appendToClassPath(location);

				Constructor<?> constructor = Class.forName("cpw.mods.modlauncher.NameMappingServiceHandler").getDeclaredConstructors()[0];
				constructor.setAccessible(true);

				Field nameMappingServiceHandlerField = Launcher.class.getDeclaredField("nameMappingServiceHandler");
				nameMappingServiceHandlerField.setAccessible(true);
				nameMappingServiceHandlerField.set(Launcher.INSTANCE, constructor.newInstance());

				Field launchPluginsField = Launcher.class.getDeclaredField("launchPlugins");
				launchPluginsField.setAccessible(true);
				launchPluginsField.set(Launcher.INSTANCE, new LaunchPluginHandler());
			} catch (Throwable throwable) {
				throwable.printStackTrace();
			}
		} else {
			try {
				Field unsafeF = Unsafe.class.getDeclaredField("theUnsafe");
				unsafeF.setAccessible(true);
				Unsafe unsafe = (Unsafe) unsafeF.get(null);

				unsafe.ensureClassInitialized(TransformingClassLoader.class);
				long skippedOff = unsafe.staticFieldOffset(TransformingClassLoader.class.getDeclaredField("SKIP_PACKAGE_PREFIXES"));
				Object skippedBase = unsafe.staticFieldBase(TransformingClassLoader.class.getDeclaredField("SKIP_PACKAGE_PREFIXES"));
				List<String> skip = (List<String>) unsafe.getObject(skippedBase, skippedOff);
				List<String> skipped = new ArrayList<>();
				skipped.add("net.fabricmc.loader.");
				skipped.add("net.fabricmc.api.Environment");
				skipped.add("net.fabricmc.api.EnvType");
				skipped.add("io.github.fcworkgroupmc.f2c.f2c.fabric.");
				skipped.addAll(skip);
				unsafe.putObject(skippedBase, skippedOff, skipped);
			} catch (NoSuchFieldException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	@Nonnull
	@Override
	public List<ITransformer> transformers() {
		return Collections.singletonList(new EntryPointBrandingTransformer());
	}
}