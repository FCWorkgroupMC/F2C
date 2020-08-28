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
import cpw.mods.modlauncher.api.*;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import io.github.fcworkgroupmc.f2c.f2c.FabricObfProcessor;
import io.github.fcworkgroupmc.f2c.f2c.fabric.FabricLoader;
import io.github.fcworkgroupmc.f2c.f2c.namemappingservices.IntermediaryToSrgNameMappingService;
import io.github.fcworkgroupmc.f2c.f2c.transformers.EntryPointBrandingTransformer;
import io.github.lxgaming.classloader.ClassLoaderUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.launch.knot.Knot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.ModDirTransformerDiscoverer;
import net.minecraftforge.fml.loading.progress.StartupMessageManager;
import net.minecraftforge.forgespi.Environment;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.platform.MixinPlatformManager;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import static io.github.fcworkgroupmc.f2c.f2c.Metadata.*;

@SuppressWarnings("rawtypes")
public class FabricModTransformationService implements ITransformationService {
	private static final Logger LOGGER = LogManager.getLogger();

	private URL location;

	public final List<Path> fabricMods = new ArrayList<>();

	private final Map<String, ILaunchPluginService> launchPluginServices;
	private final Map namingTable;
	public FabricModTransformationService() {
		if (Launcher.INSTANCE == null) {
			throw new IllegalStateException("Launcher has not been initialized!");
		}
		this.launchPluginServices = getLaunchPluginServices();
		this.namingTable = getNamingTable();
	}
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
					return (pathString.endsWith(JAR_SUFFIX) || pathString.endsWith(FABRIC_MOD_SUFFIX)) && !pathString.contains("f2c-");
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
				if(isNotDev()) {
					fabricMods.add(Paths.get(location.toURI()));
					LOGGER.debug("Added mod: {}", location);
				}
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
		initMcVersion();
		Dist dist = environment.getProperty(Environment.Keys.DIST.get()).orElseThrow(IllegalArgumentException::new);
		new Knot(dist == Dist.DEDICATED_SERVER ? EnvType.SERVER : EnvType.CLIENT, FMLLoader.getMCPaths()[0].toFile()).init();
	}

	@Override
	public void beginScanning(IEnvironment environment) {
		if(isDevelopment()) {
			try {
				Method addConnectorMethod = MixinPlatformManager.class.getDeclaredMethod("addConnector", String.class);
				addConnectorMethod.setAccessible(true);
				addConnectorMethod.invoke(MixinBootstrap.getPlatform(), "io.github.fcworkgroupmc.f2c.f2c.FabricMixinConnector");
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
				LOGGER.error("Couldn't add Mixin Connector to Mixin", e);
			}
		}

		StartupMessageManager.addModMessage("F2C-Downloading obf mappings");
		IntermediaryToSrgNameMappingService.init(mcVersion);
	}

	@Override
	public List<Map.Entry<String, Path>> runScan(IEnvironment environment) {
		beginScanning(environment);
		if(!fabricMods.isEmpty()) {
			List<Path> processedMods = new ArrayList<>();
			Path processedDir = environment.getProperty(IEnvironment.Keys.GAMEDIR.get()).orElse(FMLPaths.GAMEDIR.get()).resolve(".f2c").resolve("processed");
			try {
				if(Files.exists(processedDir) && !Files.isDirectory(processedDir))
					Files.delete(processedDir);
				if(Files.notExists(processedDir))
					Files.createDirectories(processedDir);
			} catch (IOException e) { e.printStackTrace(); }
			while(!FabricLoader.funcReady); // wait for the remap function ready
			StartupMessageManager.addModMessage("Processing Fabric mod obf");
			fabricMods.forEach(path -> {
				Path processedJar = processedDir.resolve(path.getFileName());
				FabricObfProcessor.processJar(path, processedJar);
				processedMods.add(processedJar);
			});
			FabricLoader.INSTANCE.setMods(processedMods);
			return processedMods.stream().map(path->new AbstractMap.SimpleImmutableEntry<>(path.getFileName().toString(), path)).collect(Collectors.toList());
		}
		FabricLoader.INSTANCE.setMods(Collections.emptyList());
		return Collections.emptyList();
	}

	@Override
	public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {
		location = getClass().getProtectionDomain().getCodeSource().getLocation();
		if(!otherServices.contains("fml") || (!otherServices.contains("mixin") && !otherServices.contains("mixinbootstrap")))
			throw new IncompatibleEnvironmentException(name() + " requires Forge and Mixin(or MixinBootstrap mod) to load");
		if(isNotDev()) {
			try {
				ClassLoaderUtils.appendToClassPath(Launcher.class.getClassLoader(), location);

				ModDirTransformerDiscoverer.getExtraLocators().add(Paths.get(location.toURI()));

				registerNameMappingService("io.github.fcworkgroupmc.f2c.f2c.namemappingservices.IntermediaryToSrgNameMappingService");
				registerNameMappingService("io.github.fcworkgroupmc.f2c.f2c.namemappingservices.IntermediaryToMcpNameMappingService");

				registerLaunchPluginService("io.github.fcworkgroupmc.f2c.f2c.launchplugins.fabric.AccessWidenerLaunchPlugin");
				registerLaunchPluginService("io.github.fcworkgroupmc.f2c.f2c.launchplugins.fabric.F2CLaunchPlugin");
			} catch (Throwable throwable) {
				throwable.printStackTrace();
			}
		}
		try {
			Files.probeContentType(Paths.get(location.toURI())); // Prevent java.lang.ExceptionInInitializerError in Jimfs
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
	}

	@Nonnull
	@Override
	public List<ITransformer> transformers() {
		return Collections.singletonList(new EntryPointBrandingTransformer());
	}

	public boolean isDevelopment() {
		return location == null || !location.getPath().endsWith(".jar");
	}
	public boolean isNotDev() {
		return location != null && location.getPath().endsWith(".jar");
	}

	private void registerNameMappingService(String className) throws IncompatibleEnvironmentException {
		try {
			Class<? extends INameMappingService> nameMappingServiceClass = (Class<? extends INameMappingService>) Class.forName(className, true, Launcher.class.getClassLoader());
			if(isNameMappingServicePresent(nameMappingServiceClass)) {
				LOGGER.warn("{} is already registered", nameMappingServiceClass.getSimpleName());
				return;
			}
			INameMappingService service = nameMappingServiceClass.newInstance();
			String name = service.mappingName();

			Constructor<?> c = Class.forName("cpw.mods.modlauncher.NameMappingServiceDecorator").getConstructor(INameMappingService.class);
			c.setAccessible(true);
			namingTable.put(name, c.newInstance(service));

			LOGGER.debug("Registered {} ({})", nameMappingServiceClass.getSimpleName(), name);
		} catch (Throwable e) {
			LOGGER.error("Encountered an error while registering {}", className, e);
			throw new IncompatibleEnvironmentException(String.format("Failed to register %s", className));
		}
	}
	private void registerLaunchPluginService(String className) throws IncompatibleEnvironmentException {
		try {
			Class<? extends ILaunchPluginService> launchPluginServiceClass = (Class<? extends ILaunchPluginService>) Class.forName(className, true, Launcher.class.getClassLoader());
			if (isLaunchPluginServicePresent(launchPluginServiceClass)) {
				LOGGER.warn("{} is already registered", launchPluginServiceClass.getSimpleName());
				return;
			}

			ILaunchPluginService launchPluginService = launchPluginServiceClass.newInstance();
			String pluginName = launchPluginService.name();
			this.launchPluginServices.put(pluginName, launchPluginService);

			List<Map<String, String>> mods = Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.MODLIST.get()).orElse(null);
			if (mods != null) {
				Map<String, String> mod = new HashMap<>();
				mod.put("name", pluginName);
				mod.put("type", "PLUGINSERVICE");
				String fileName = launchPluginServiceClass.getProtectionDomain().getCodeSource().getLocation().getFile();
				mod.put("file", fileName.substring(fileName.lastIndexOf('/')));
				mods.add(mod);
			}

			LOGGER.debug("Registered {} ({})", launchPluginServiceClass.getSimpleName(), pluginName);
		} catch (Throwable ex) {
			LOGGER.error("Encountered an error while registering {}", className, ex);
			throw new IncompatibleEnvironmentException(String.format("Failed to register %s", className));
		}
	}
	private boolean isLaunchPluginServicePresent(Class<? extends ILaunchPluginService> launchPluginServiceClass) {
		for (ILaunchPluginService launchPluginService : this.launchPluginServices.values()) {
			if (launchPluginServiceClass.isInstance(launchPluginService)) {
				return true;
			}
		}
		return false;
	}
	private boolean isNameMappingServicePresent(Class<? extends INameMappingService> launchPluginServiceClass) throws NoSuchFieldException, IllegalAccessException {
		for (Object nameMappingServiceDecorator : this.namingTable.values()) {
			Field f = nameMappingServiceDecorator.getClass().getDeclaredField("service");
			f.setAccessible(true);
			if (launchPluginServiceClass.isInstance(f.get(nameMappingServiceDecorator))) {
				return true;
			}
		}
		return false;
	}
	private Map<String, ILaunchPluginService> getLaunchPluginServices() {
		try {
			// cpw.mods.modlauncher.Launcher.launchPlugins
			Field launchPluginsField = Launcher.class.getDeclaredField("launchPlugins");
			launchPluginsField.setAccessible(true);
			LaunchPluginHandler launchPluginHandler = (LaunchPluginHandler) launchPluginsField.get(Launcher.INSTANCE);

			// cpw.mods.modlauncher.LaunchPluginHandler.plugins
			Field pluginsField = LaunchPluginHandler.class.getDeclaredField("plugins");
			pluginsField.setAccessible(true);
			return (Map<String, ILaunchPluginService>) pluginsField.get(launchPluginHandler);
		} catch (Exception ex) {
			LOGGER.error("Encountered an error while getting LaunchPluginServices", ex);
			return null;
		}
	}
	private Map<String, ?> getNamingTable() {
		try {
			Field namingTableField = Class.forName("cpw.mods.modlauncher.NameMappingServiceHandler").getDeclaredField("namingTable");
			namingTableField.setAccessible(true);

			Field nameMappingServiceHandlerField = Launcher.class.getDeclaredField("nameMappingServiceHandler");
			nameMappingServiceHandlerField.setAccessible(true);

			return (Map) namingTableField.get(nameMappingServiceHandlerField.get(Launcher.INSTANCE));
		} catch (Exception ex) {
			LOGGER.error("Encountered an error while getting NameMappingServiceHandler", ex);
			return null;
		}
	}
}