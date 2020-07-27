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
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class FabricModTransformationService implements ITransformationService {
	public static final String FABRIC_MOD_SUFFIX = ".fabricmod";
	private static final Logger LOGGER = LogManager.getLogger();
	@Nonnull
	@Override
	public String name() {
		return "f2c";
	}

	@Override
	public void initialize(IEnvironment environment) {
		try {
			Path modsDir = environment.getProperty(IEnvironment.Keys.GAMEDIR.get()).orElse(Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParent()).
					resolve(FMLPaths.MODSDIR.relative());
			Files.walk(modsDir, 1).filter(path -> path.endsWith(".jar")).forEach(modPath -> {
				try {
					JarFile jarFile = new JarFile(modPath.toFile());
					ZipEntry entry = jarFile.getEntry("fabric.mod.json");
					jarFile.close();
					if(entry != null) modPath.toFile().renameTo(new File(modPath.toString().replace(".jar", FABRIC_MOD_SUFFIX)));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			Runtime.getRuntime().addShutdownHook(new ShutdownRenameThread(modsDir));
		} catch (Exception e) {
			LOGGER.error("An error occurred when loading FabricModExtensionTransformationService", e);
		}
	}

	private static class ShutdownRenameThread extends Thread {
		private Path modsDir;
		public ShutdownRenameThread(Path modsDir) {
			this.modsDir = modsDir;
		}
		@Override
		public void run() {
			try {
				Files.walk(modsDir, 1).filter(path -> path.endsWith(FABRIC_MOD_SUFFIX)).
						forEach(path -> path.toFile().renameTo(new File(path.toString().replace(FABRIC_MOD_SUFFIX, ".jar"))));
			} catch (IOException e) {
				LOGGER.catching(Level.WARN, e);
			}
		}
	}

	@Override
	public void beginScanning(IEnvironment environment) {
		try {
			Path gameDir = environment.getProperty(IEnvironment.Keys.GAMEDIR.get()).orElse(Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParent());
			Path modsDir = gameDir.resolve(FMLPaths.MODSDIR.relative());
			Files.walk(modsDir, 1).filter(path -> path.endsWith(FABRIC_MOD_SUFFIX)).forEach(path -> {
				try(JarFile jarFile = new JarFile(path.toFile())) {
					ZipEntry entry = jarFile.getEntry("fabric.mod.json");
					if(entry != null) {

					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {}

	@Nonnull
	@Override
	public List<ITransformer> transformers() {return Collections.emptyList();}
}