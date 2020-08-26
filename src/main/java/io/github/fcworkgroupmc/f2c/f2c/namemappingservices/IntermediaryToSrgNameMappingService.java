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

import cpw.mods.modlauncher.api.INameMappingService;
import io.github.fcworkgroupmc.f2c.f2c.fabric.FabricLoader;
import io.github.fcworkgroupmc.f2c.f2c.util.NetworkUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.mapping.tree.*;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.IRenamer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

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
			}
			return original;
		};
	}
	public static void init(String version) {
		try {
			NetworkUtil.newBuilder("https://raw.githubusercontent.com/MinecraftForge/MCPConfig/master/versions/release/" + version + "/joined.tsrg")
				.connectAsync().thenApplyAsync(NetworkUtil.Net.Connection::asStream)
				.thenApplyAsync(in -> {
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
						NetworkUtil.newBuilder("https://raw.githubusercontent.com/FabricMC/intermediary/master/mappings/" + version + ".tiny")
							.connectAsync().thenApply(NetworkUtil.Net.Connection::asReaderBuffered)
							.thenApply(reader -> {
								try {
									TinyTree intermediaryNames = TinyMappingFactory.loadWithDetection(reader);
									Map<String, ClassDef> classMap = intermediaryNames.getClasses()
											.stream().collect(Collectors.toMap(def -> def.getName("official"), Function.identity()));
									mapping.rename(new IRenamer() {
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
									});
									return mapping.reverse();
								} catch (IOException e) {
									LOGGER.fatal("Error loading intermediary mapping file", e);
									throw new RuntimeException("Error loading intermediary mapping file", e);
								} finally {
									IOUtils.closeQuietly(reader);
								}
							}).thenAccept(map -> map.getClasses().forEach(c -> {
								classes.put(c.getOriginal(), c.getMapped());
								c.getFields().forEach(f -> fields.put(f.getOriginal(), f.getMapped()));
								c.getMethods().forEach(m -> methods.put(m.getOriginal(), m.getMapped()));
							})).get();
					} catch (InterruptedException | ExecutionException e) {
						LOGGER.fatal("Error when executing task", e);
					}
				}).thenAcceptAsync(v -> IntermediaryToMcpNameMappingService.init())
					.whenComplete((v, throwable) -> FabricLoader.funcReady())
					.get(10, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.fatal("Error when executing task", e);
		} catch (TimeoutException e) {
			throw new RuntimeException("Connection timed out", e);
		}
	}
}