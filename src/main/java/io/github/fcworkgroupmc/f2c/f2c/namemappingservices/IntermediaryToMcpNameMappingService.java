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
import io.github.fcworkgroupmc.f2c.f2c.util.NetworkUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;

public class IntermediaryToMcpNameMappingService implements INameMappingService {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Object2ObjectOpenHashMap<String, String> fields = new Object2ObjectOpenHashMap<>();
	private static final Object2ObjectOpenHashMap<String, String> methods = new Object2ObjectOpenHashMap<>();
	@Override
	public String mappingName() {
		return "intermediarytomcp";
	}

	@Override
	public String mappingVersion() {
		return "1";
	}

	@Override
	public Map.Entry<String, String> understanding() {
		return Pair.of("intermediary", "mcp");
	}

	@Override
	public BiFunction<Domain, String, String> namingFunction() {
		return (domain, original) -> {
			switch(domain) {
				case CLASS:
					return IntermediaryToSrgNameMappingService.classes.getOrDefault(original, original);
				case FIELD:
					String srg = IntermediaryToSrgNameMappingService.fields.getOrDefault(original, original);
					return fields.getOrDefault(srg, srg);
				case METHOD:
					srg = IntermediaryToSrgNameMappingService.methods.getOrDefault(original, original);
					return methods.getOrDefault(srg, srg);
			}
			return original;
		};
	}

	static void init() {
		if(IntermediaryToSrgNameMappingService.classes.isEmpty() || IntermediaryToSrgNameMappingService.fields.isEmpty()
				|| IntermediaryToSrgNameMappingService.methods.isEmpty())
			throw new RuntimeException("Mappings are empty, please check your Internet connection");
		try {
			NetworkUtil.newBuilder("http://export.mcpbot.bspk.rs/fields.csv")
					.connectAsync().thenApply(NetworkUtil.Net.Connection::asReaderBuffered)
					.thenAccept(reader -> reader.lines().skip(1L).map(s -> s.split(","))
					.forEach(names -> fields.put(names[0], names[1]))).get(); // [0] is srg name, [1] is mcp name
			NetworkUtil.newBuilder("http://export.mcpbot.bspk.rs/methods.csv")
					.connectAsync().thenApply(NetworkUtil.Net.Connection::asReaderBuffered)
					.thenAccept(reader -> reader.lines().skip(1L).map(s -> s.split(","))
					.forEach(names -> methods.put(names[0], names[1]))).get(); // [0] is srg name, [1] is mcp name
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.fatal("Error when executing task", e);
		}
	}
}