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

import com.mojang.bridge.game.GameVersion;
import cpw.mods.modlauncher.api.INameMappingService;
import io.github.fcworkgroupmc.f2c.f2c.util.NetworkUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.mapping.tree.ClassDef;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;
import net.minecraft.util.SharedConstants;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.IRenamer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IntermediaryToSrgNameMappingService implements INameMappingService {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final GameVersion VERSION = SharedConstants.getVersion();
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
					return fields.get(original);
				case METHOD:
					return methods.get(original);
			}
			return original;
		};
	}
	static {
		try {
			// Because Forge only supports release version of Minecraft, so we use GameVersion.getReleaseTarget()
			NetworkUtil.newBuilder("https://raw.githubusercontent.com/MinecraftForge/MCPConfig/master/versions/release/" + VERSION.getReleaseTarget() + "/joined.tsrg")
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
					}).thenAcceptAsync(mapping -> {
						try {
							NetworkUtil.newBuilder("https://raw.githubusercontent.com/FabricMC/intermediary/master/mappings/" + VERSION.getReleaseTarget() + ".tiny")
									.connectAsync().thenApply(NetworkUtil.Net.Connection::asReaderBuffered)
									.thenApply(reader -> {
										try {
											TinyTree intermediaryNames = TinyMappingFactory.loadWithDetection(reader);
											Map<String, ClassDef> classMap = intermediaryNames.getClasses()
													.stream().collect(Collectors.toMap(def -> def.getName("official"), Function.identity()));
											mapping.rename(new IRenamer() {
												@Override
												public String rename(IMappingFile.IClass value) {
													// Always directly throw NPE
													return classMap.get(value.getMapped()).getName("intermediary");
												}
												@Override
												public String rename(IMappingFile.IField value) {
													return classMap.get(value.getParent().getMapped()).getFields().stream()
															.filter(field -> field.getName("official").equals(value.getMapped()))
															.findAny().get().getName("intermediary");
												}
												@Override
												public String rename(IMappingFile.IMethod value) {
													return classMap.get(value.getParent().getMapped()).getMethods().stream()
															.filter(method -> method.getName("official").equals(value.getMapped()) &&
																	method.getDescriptor("official").equals(value.getMappedDescriptor()))
															.findAny().get().getName("intermediary");
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
					}).get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.fatal("Error when executing task", e);
		}
	}
	static <T> BinaryOperator<T> throwingMerger() {
		return (u,v) -> { throw new IllegalStateException(String.format("Duplicate key %s", u)); };
	}
}