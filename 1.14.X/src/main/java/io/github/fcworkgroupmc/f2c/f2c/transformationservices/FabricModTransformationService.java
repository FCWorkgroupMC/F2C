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

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import io.github.fcworkgroupmc.f2c.f2c.fabric.FabricLoader;
import io.github.fcworkgroupmc.f2c.f2c.transformers.EntryPointBrandingTransformer;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.fabricmc.loader.launch.knot.Knot;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class FabricModTransformationService implements ITransformationService {
	public static final String FABRIC_MOD_SUFFIX = ".fabricmod";
	public static final String JAR_SUFFIX = ".jar";
	/** fabric mod definition(fabric.mod.json) */
	public static final String FABRIC_MOD_DEF = "fabric.mod.json";
	private static final Logger LOGGER = LogManager.getLogger();

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
				Files.walk(modsDir, 1).filter(path -> path.endsWith(JAR_SUFFIX)).forEach(modPath -> {
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
					}
				});
				Runtime.getRuntime().addShutdownHook(new Thread(() -> fabricMods.forEach(p -> {
					try {
						Files.move(p, p.getParent().resolve(p.getFileName().toString().replace(FABRIC_MOD_SUFFIX, JAR_SUFFIX)));
					} catch (IOException e) {
						LOGGER.catching(Level.WARN, e);
					}
				})));
			}
		} catch (Exception e) {
			LOGGER.error("error occurred when initializing f2c service " + e);
		}
		String launchTarget = environment.getProperty(IEnvironment.Keys.LAUNCHTARGET.get()).orElse("client");
		new Knot(launchTarget.contains("server") ? EnvType.SERVER : EnvType.CLIENT, null).init();
	}

	@Override
	public void beginScanning(IEnvironment environment) {}

	@Override
	public List<Map.Entry<String, Path>> runScan(IEnvironment environment) {
		beginScanning(environment);
		if(!fabricMods.isEmpty()) {
			FabricLoader loader = FabricLoader.INSTANCE;
			loader.setGameProvider(FabricLauncherBase.getLauncher().getGameProvider());
			loader.loadMods();
			loader.endModLoading();
			return fabricMods.stream().map(path -> new AbstractMap.SimpleEntry<>(path.getFileName().toString(), path)).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	@Override
	public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {
		instance = this;
		if(!otherServices.contains("fml") || !otherServices.contains("mixin"))
			throw new IncompatibleEnvironmentException(name() + " requires Forge and Mixin(or MixinBootstrap mod) to load");
	}

	@Nonnull
	@Override
	public List<ITransformer> transformers() {return Collections.singletonList(new EntryPointBrandingTransformer());}
}