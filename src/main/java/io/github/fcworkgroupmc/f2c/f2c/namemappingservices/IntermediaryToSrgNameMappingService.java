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

package io.github.fcworkgroupmc.f2c.f2c.namemappingservices;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.INameMappingService;
import io.github.fcworkgroupmc.f2c.f2c.Metadata;
import io.github.fcworkgroupmc.f2c.f2c.util.NetworkUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.mapping.tree.*;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.progress.StartupMessageManager;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.IRenamer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.fcworkgroupmc.f2c.f2c.Metadata.F2C_DIR;
import static io.github.fcworkgroupmc.f2c.f2c.Metadata.isDevelopment;

public class IntermediaryToSrgNameMappingService implements INameMappingService {
	private static final Logger LOGGER = LogManager.getLogger();
	static final Object2ObjectOpenHashMap<String, String> classes = new Object2ObjectOpenHashMap<>();
	static final Object2ObjectOpenHashMap<String, String> fields = new Object2ObjectOpenHashMap<>();
	static final Object2ObjectOpenHashMap<String, String> methods = new Object2ObjectOpenHashMap<>();
	@Override
	public String mappingName() {
		return "intermediarytosrg";
	}

	@Override
	public String mappingVersion() {
		return "1";
	}

	@Override
	public Map.Entry<String, String> understanding() {
		return Pair.of("intermediary", "srg");
	}

	@Override
	public BiFunction<Domain, String, String> namingFunction() {
		return (domain, original) -> {
			switch(domain) {
				case CLASS:
					return classes.getOrDefault(original, original);
				case FIELD:
					return fields.getOrDefault(original, original);
				case METHOD:
					return methods.getOrDefault(original, original);
				default:
					throw new IllegalArgumentException("Unknown domain");
			}
		};
	}
	public static void init(String version, IEnvironment environment) {
		try {
			Path mappingsDir = environment.getProperty(IEnvironment.Keys.GAMEDIR.get()).orElse(FMLPaths.GAMEDIR.get()).resolve(F2C_DIR).resolve("mappings");
			Files.createDirectories(mappingsDir);
			Path srgFile = mappingsDir.resolve(version + "-joined.tsrg");
			Path srgFileCompleted = mappingsDir.resolve(version + "-joined.tsrg.complete");
			Path intermediaryFile = mappingsDir.resolve(version + ".tiny");
			Path intermediaryFileCompleted = mappingsDir.resolve(version + ".tiny.complete");
			if(Files.notExists(srgFile) || Files.notExists(srgFileCompleted)) {
				StartupMessageManager.addModMessage("F2C-Downloading srg obf mappings");
				LOGGER.debug("Downloading srg obf mappings");
				NetworkUtil.newBuilder("https://raw.githubusercontent.com/MinecraftForge/MCPConfig/master/versions/release/" + version + "/joined.tsrg")
						.timeout(Duration.ofSeconds(5L)).connectAsync()
						.thenAccept(connection -> {
							try {
								Files.copy(connection.asStream(), srgFile, StandardCopyOption.REPLACE_EXISTING);
								Files.deleteIfExists(srgFileCompleted);
								Files.createFile(srgFileCompleted);
							} catch (IOException e) {
								throw new RuntimeException(e);
							} finally {
								IOUtils.closeQuietly(connection);
							}
						}).get(5, TimeUnit.SECONDS);
			}
			if(Files.notExists(intermediaryFile) || Files.notExists(intermediaryFileCompleted)) {
				StartupMessageManager.addModMessage("F2C-Downloading intermediary obf mappings");
				LOGGER.debug("Downloading intermediary obf mappings");
				NetworkUtil.newBuilder("https://raw.githubusercontent.com/FabricMC/intermediary/master/mappings/" + version + ".tiny")
						.timeout(Duration.ofSeconds(5L)).connectAsync()
						.thenAccept(connection -> {
							try {
								Files.copy(connection.asStream(), intermediaryFile, StandardCopyOption.REPLACE_EXISTING);
								Files.deleteIfExists(intermediaryFileCompleted);
								Files.createFile(intermediaryFileCompleted);
							} catch (IOException e) {
								throw new RuntimeException(e);
							} finally {
								IOUtils.closeQuietly(connection);
							}
						}).get(5, TimeUnit.SECONDS);
			}
			CompletableFuture.supplyAsync(() -> {
				try {
					return Files.newInputStream(srgFile);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}).thenApplyAsync(in -> {
				try {
					return IMappingFile.load(in).reverse();
				} catch (IOException e) {
					LOGGER.fatal("Error loading srgnames mapping file", e);
					throw new RuntimeException("Error loading srgnames mapping file", e);
				} finally {
					IOUtils.closeQuietly(in);
				}
			}).thenAccept(mapping -> {
				try {
					CompletableFuture.supplyAsync(() -> {
						try {
							return IOUtils.buffer(new InputStreamReader(Files.newInputStream(intermediaryFile), StandardCharsets.UTF_8));
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}).thenApply(reader -> {
						try {
							TinyTree intermediaryNames = TinyMappingFactory.loadWithDetection(reader);
							Map<String, ClassDef> classMap = intermediaryNames.getClasses()
									.stream().collect(Collectors.toMap(def -> def.getName("official"), Function.identity())); // by obf name
							return mapping.rename(new IRenamer() { // (srg - obf)
								@Override
								public String rename(IMappingFile.IClass value) {
									return classMap.get(value.getMapped()).getName("intermediary");
								}
								@Override
								public String rename(IMappingFile.IField value) {
									return classMap.get(value.getParent().getMapped()).getFields().stream()
											.filter(field -> field.getName("official").equals(value.getMapped()))
											.findAny().orElse(new FieldDef() {
												@Override
												public String getDescriptor(String s) {return "inherit";}
												@Override
												public String getName(String s) {return "inherit";}
												@Override
												public String getRawName(String s) {return "inherit";}
												@Override
												public String getComment() {return "";}
											}).getName("intermediary");
								}
								@Override
								public String rename(IMappingFile.IMethod value) {
									return classMap.get(value.getParent().getMapped()).getMethods().stream()
											.filter(method -> method.getName("official").equals(value.getMapped()) &&
													method.getDescriptor("official").equals(value.getMappedDescriptor()))
											.findAny().orElse(new MethodDef() {
												@Override
												public Collection<ParameterDef> getParameters() { return Collections.emptySet(); }
												@Override
												public Collection<LocalVariableDef> getLocalVariables() { return Collections.emptySet(); }
												@Override
												public String getDescriptor(String s) { return "inherit"; }
												@Override
												public String getName(String s) { return "inherit"; }
												@Override
												public String getRawName(String s) { return "inherit"; }
												@Override
												public String getComment() { return ""; }
											}).getName("intermediary");
								}
							}).reverse(); // (intermediary - srg)
						} catch (IOException e) {
							LOGGER.fatal("Error loading intermediary mapping file", e);
							throw new RuntimeException("Error loading intermediary mapping file", e);
						} finally {
							IOUtils.closeQuietly(reader);
						}
					}).thenAccept(map -> map.getClasses().forEach(c -> {
						classes.put(c.getOriginal(), c.getMapped());
						c.getFields().forEach(f -> {if(!f.getOriginal().equals("inherit")) fields.put(f.getOriginal(), f.getMapped());});
						c.getMethods().forEach(m -> {if(!m.getOriginal().equals("inherit")) methods.put(m.getOriginal(), m.getMapped());});
					})).get();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}).thenAcceptAsync(v -> {
				if(isDevelopment() || FMLEnvironment.naming.equalsIgnoreCase("mcp"))
					IntermediaryToMcpNameMappingService.init();
			}).whenComplete((v, throwable) -> Metadata.funcReady()).get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.fatal("Error when executing task", e);
		} catch (TimeoutException e) {
			StartupMessageManager.addModMessage("F2C-Downloading obf mappings-Timed out");
			StartupMessageManager.addModMessage("F2C-Exit in 3 seconds");
			try {
				Thread.sleep(3000);
			}catch(InterruptedException ignored){}
			throw new RuntimeException("Connection timed out", e);
		} catch (IOException e) {
			LOGGER.fatal("IO error occurs", e);
		}
	}
}