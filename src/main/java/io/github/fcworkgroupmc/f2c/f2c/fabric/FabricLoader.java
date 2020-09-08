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

package io.github.fcworkgroupmc.f2c.f2c.fabric;

import io.github.fcworkgroupmc.f2c.f2c.Metadata;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.EntrypointStorage;
import net.fabricmc.loader.FabricMappingResolver;
import net.fabricmc.loader.ModContainer;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.MappingResolver;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.discovery.ClasspathModCandidateFinder;
import net.fabricmc.loader.discovery.ModCandidate;
import net.fabricmc.loader.discovery.ModResolutionException;
import net.fabricmc.loader.discovery.ModResolver;
import net.fabricmc.loader.game.GameProvider;
import net.fabricmc.loader.gui.FabricGuiEntry;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.fabricmc.loader.metadata.EntrypointMetadata;
import net.fabricmc.loader.metadata.LoaderModMetadata;
import net.fabricmc.loader.transformer.accesswidener.AccessWidener;
import net.fabricmc.loader.util.DefaultLanguageAdapter;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

// F2C - reimplement net.fabricmc.loader.api.FabricLoader and delete net.fabricmc.loader.FabricLoader
public class FabricLoader implements net.fabricmc.loader.api.FabricLoader {
	public static final FabricLoader INSTANCE = new FabricLoader();
	private static final Logger LOGGER = LogManager.getFormatterLogger("Fabric|Loader");
	private FabricLoader() { }

	private GameProvider provider;
	private Path gameDir;
	public boolean lockLoading;
	private MappingResolver mappingResolver;
	private Object gameInstance;
	private AccessWidener accessWidener = new AccessWidener(this);

	private final Map<String, ModContainer> modMap = new HashMap<>();
	private List<ModContainer> mods = new ArrayList<>();

	private List<Path> fabricMods;

	private final Map<String, LanguageAdapter> adapterMap = new HashMap<>();
	private final EntrypointStorage entrypointStorage = new EntrypointStorage();

	@Override
	public <T> List<T> getEntrypoints(String key, Class<T> type) {
		return entrypointStorage.getEntrypoints(key, type);
	}

	@Override
	public <T> List<EntrypointContainer<T>> getEntrypointContainers(String key, Class<T> type) {
		return entrypointStorage.getEntrypointContainers(key, type);
	}

	@Override
	public MappingResolver getMappingResolver() {
		if (mappingResolver == null) {
			mappingResolver = new FabricMappingResolver(
					FabricLauncherBase.getLauncher().getMappingConfiguration()::getMappings,
					FabricLauncherBase.getLauncher().getTargetNamespace()
			);
		}

		return mappingResolver;
	}

	@Override
	public Optional<net.fabricmc.loader.api.ModContainer> getModContainer(String id) {
		return Optional.ofNullable(modMap.get(id));
	}

	@Override
	public Collection<net.fabricmc.loader.api.ModContainer> getAllMods() {
		return Collections.unmodifiableList(mods);
	}

	@Override
	public boolean isModLoaded(String id) {
		return modMap.containsKey(id);
	}

	@Override
	public boolean isDevelopmentEnvironment() {
		return FabricLauncherBase.getLauncher().isDevelopment();
	}

	@Override
	public EnvType getEnvironmentType() {
		return FabricLauncherBase.getLauncher().getEnvironmentType();
	}

	@Override
	public Object getGameInstance() {
		return gameInstance;
	}

	@Override
	public Path getGameDir() {
		return this.gameDir;
	}

	@Override
	public File getGameDirectory() {
		return getGameDir().toFile();
	}

	@Override
	public Path getConfigDir() {
		return getGameDir().resolve(FMLPaths.CONFIGDIR.relative());
	}

	@Override
	public File getConfigDirectory() {
		return getConfigDir().toFile();
	}

	public void setGameProvider(GameProvider provider) {
		this.provider = provider;
		this.gameDir = this.provider.getLaunchDirectory();
	}
	public GameProvider getGameProvider() {
		return this.provider;
	}
	public Logger getLogger() {
		return LOGGER;
	}
	public AccessWidener getAccessWidener() {
		return accessWidener;
	}
	public boolean hasEntrypoints(String key) {
		return entrypointStorage.hasEntrypoints(key);
	}
	public void setMods(List<Path> fabricMods) {
		this.fabricMods = fabricMods;
	}

	public void loadMods() {
		if(provider == null) throw new RuntimeException("You must to set game provider!");
		if(lockLoading) throw new RuntimeException("mod loading is ended");

		try {
			ModResolver resolver = new ModResolver();
			// F2C - Remove DirectoryModCandidateFinder, use ListModCandidateFinder instead
			resolver.addCandidateFinder(new ClasspathModCandidateFinder());
//			resolver.addCandidateFinder(new DirectoryModCandidateFinder(getGameDir().resolve(FMLPaths.MODSDIR.relative())));
			resolver.addCandidateFinder(new ListModCandidateFinder(fabricMods));
			Map<String, ModCandidate> candidateMap = resolver.resolve(this);

			String modText;
			switch (candidateMap.values().size()) {
				case 0:
					modText = "Loading %d mods";
					break;
				case 1:
					modText = "Loading %d mod: %s";
					break;
				default:
					modText = "Loading %d mods: %s";
					break;
			}
			LOGGER.info("[" + getClass().getSimpleName() + "] " + modText, candidateMap.values().size(), candidateMap.values().stream()
					.map(info -> String.format("%s@%s", info.getInfo().getId(), info.getInfo().getVersion().getFriendlyString()))
					.collect(Collectors.joining(", ")));

			for(ModCandidate candidate : candidateMap.values()) {
				LoaderModMetadata info = candidate.getInfo();
				URL originUrl = candidate.getOriginUrl();

				if (modMap.containsKey(info.getId())) {
					throw new ModResolutionException("Duplicate mod ID: " + info.getId() + "! (" + modMap.get(info.getId()).getOriginUrl().getFile() + ", " + originUrl.getFile() + ")");
				}

				if (!info.loadsInEnvironment(getEnvironmentType())) {
					return;
				}

				ModContainer container = new ModContainer(info, originUrl);
				mods.add(container);
				modMap.put(info.getId(), container);
			}
		} catch (ModResolutionException e) {
			FabricGuiEntry.displayCriticalError(e, true);
		}
		Metadata.addLibraries(mods.stream().filter(mod -> !mod.getInfo().getId().equals("fabricloader")).map(mod-> {
			try {
				return new ModFile(Paths.get(mod.getOriginUrl().toURI()), Metadata.nothingLocator);
			} catch (URISyntaxException e) { throw new RuntimeException(e); }
		}).collect(Collectors.toList()));
	}
	public void endModLoading() {
		if(lockLoading) throw new RuntimeException("Mod loading already ended");
		lockLoading = true;

		adapterMap.put("default", DefaultLanguageAdapter.INSTANCE);

		for (ModContainer mod : mods) {
			if (!(mod.getInfo().getVersion() instanceof SemanticVersion)) { // postprocessModMetadata()
				LOGGER.warn("Mod `" + mod.getInfo().getId() + "` (" + mod.getInfo().getVersion().getFriendlyString() + ") does not respect SemVer - comparison support is limited.");
			} else if (((SemanticVersion) mod.getInfo().getVersion()).getVersionComponentCount() >= 4) {
				LOGGER.warn("Mod `" + mod.getInfo().getId() + "` (" + mod.getInfo().getVersion().getFriendlyString() + ") uses more dot-separated version components than SemVer allows; support for this is currently not guaranteed.");
			}

			for (Map.Entry<String, String> laEntry : mod.getInfo().getLanguageAdapterDefinitions().entrySet()) { // setupLanguageAdapters()
				if (adapterMap.containsKey(laEntry.getKey())) {
					throw new RuntimeException("Duplicate language adapter key: " + laEntry.getKey() + "! (" + laEntry.getValue() + ", " + adapterMap.get(laEntry.getKey()).getClass().getName() + ")");
				}

				try {
					adapterMap.put(laEntry.getKey(), (LanguageAdapter) Class.forName(laEntry.getValue(), true, FabricLauncherBase.getLauncher().getTargetClassLoader()).getDeclaredConstructor().newInstance());
				} catch (Exception e) {
					throw new RuntimeException("Failed to instantiate language adapter: " + laEntry.getKey(), e);
				}
			}

			try { // setupMods()
				mod.setupRootPath();

				for (String in : mod.getInfo().getOldInitializers()) {
					String adapter = mod.getInfo().getOldStyleLanguageAdapter();
					entrypointStorage.addDeprecated(mod, adapter, in);
				}

				for (String key : mod.getInfo().getEntrypointKeys()) {
					for (EntrypointMetadata in : mod.getInfo().getEntrypoints(key)) {
						entrypointStorage.add(mod, key, in, adapterMap);
					}
				}
			} catch (Exception e) {
				throw new RuntimeException(String.format("Failed to setup mod %s (%s)", mod.getInfo().getName(), mod.getOriginUrl().getFile()), e);
			}
		}
	}

	public void prepareModInit(Path newRunDir, Object gameInstance) {
		if (!lockLoading) {
			throw new RuntimeException("Cannot instantiate mods when loading is unlocked!");
		}

		this.gameInstance = gameInstance;

		if (gameDir != null) {
			try {
				if (!gameDir.toRealPath().equals(newRunDir.toRealPath())) {
					getLogger().warn("Inconsistent game execution directories: engine says " + newRunDir.toRealPath() + ", while initializer says " + gameDir.toRealPath() + "...");
					this.gameDir = newRunDir;
				}
			} catch (IOException e) {
				getLogger().warn("Exception while checking game execution directory consistency!", e);
			}
		} else {
			this.gameDir = newRunDir;
		}
	}

	// Deprecated methods
	/**
	 * @return A list of all loaded mods, as ModContainers.
	 * @deprecated Use {@link net.fabricmc.loader.api.FabricLoader#getAllMods()}
	 */
	@Deprecated
	public Collection<ModContainer> getModContainers() {
		return Collections.unmodifiableList(mods);
	}

	@Deprecated
	public List<ModContainer> getMods() {
		return Collections.unmodifiableList(mods);
	}
}
