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

package io.github.fcworkgroupmc.f2c.f2c;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.INameMappingService;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.progress.StartupMessageManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;

import static io.github.fcworkgroupmc.f2c.f2c.Metadata.F2C_DIR;

public class FabricObfProcessor {
	private static final Logger LOGGER = LogManager.getLogger();
	public static final FabricModRemapper REMAPPER = new FabricModRemapper();
	private static void processJar(JarFile input, JarOutputStream output) throws IOException {
		JsonObject fabricJson = new JsonParser().parse(new InputStreamReader(input.getInputStream(input.getEntry(Metadata.FABRIC_MOD_DEF)), StandardCharsets.UTF_8)).getAsJsonObject();
		List<String> refMapPaths = Collections.emptyList();
		if(fabricJson.has("mixins"))
			refMapPaths = StreamSupport.stream(fabricJson.getAsJsonArray("mixins").spliterator(), false)
					.map(element -> { try {
						return new JsonParser().parse(new InputStreamReader(input.getInputStream(input.getEntry(element.getAsString())), StandardCharsets.UTF_8)).getAsJsonObject();
					} catch(IOException e){throw new IllegalStateException(e);}}).filter(obj -> obj.has("refmap"))
					.map(obj -> obj.get("refmap").getAsString()).collect(Collectors.toList());
		Enumeration<JarEntry> entries = input.entries();
		while(entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			output.putNextEntry(new JarEntry(entry.getName()));
			if(!entry.isDirectory()) {
				if(refMapPaths.contains(entry.getName())) {
					JsonObject object = REMAPPER.mapRefMap(new JsonParser().parse(new InputStreamReader(input.getInputStream(entry), StandardCharsets.UTF_8)).getAsJsonObject());
					output.write(object.toString().getBytes());
				} else if(entry.getName().endsWith(".class")) {
					ClassReader reader = new ClassReader(input.getInputStream(entry));
					ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
					reader.accept(new ClassRemapper(writer, REMAPPER), 0);
					output.write(writer.toByteArray());
				} else if(entry.getName().endsWith(Metadata.JAR_SUFFIX)) {
					try (JarInputStream innerStream = new JarInputStream(input.getInputStream(entry));
					     ByteArrayOutputStream baos = new ByteArrayOutputStream();
					     JarOutputStream out = new JarOutputStream(baos)) {
						LOGGER.debug("Processing inner jar {}", entry.getName());
						processInnerJar(innerStream, out);
						out.finish();
						output.write(baos.toByteArray());
					}
				} else {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					IOUtils.copyLarge(input.getInputStream(entry), baos);
					output.write(baos.toByteArray());
				}
			}
			output.closeEntry();
			output.flush();
		}
	}
	public static void processJar(Path input, Path output) {
		LOGGER.debug("Processing {}", input.getFileName());
		StartupMessageManager.addModMessage("Processing Fabric mod obf: " + input.getFileName());
		try(JarFile jarFile = new JarFile(input.toFile())) {
			if(Files.notExists(output)) Files.createFile(output);
			try(JarOutputStream outputJar = new JarOutputStream(Files.newOutputStream(output))) {
				processJar(jarFile, outputJar);
			}
		} catch (IOException e) {
			throw new RuntimeException("Error early processing the Fabric mod file", e);
		}
	}
	public static void processInnerJar(JarInputStream in, JarOutputStream out) throws IOException {
		Path temp = Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.GAMEDIR.get()).orElse(FMLPaths.GAMEDIR.get()).resolve(F2C_DIR).resolve("tempInner");
		if(Files.notExists(temp)) Files.createDirectories(temp);
		Path tempJar = temp.resolve(UUID.randomUUID().toString().replace("-", "") + ".temp");
		Files.deleteIfExists(tempJar);
		Files.createFile(tempJar);

		try(OutputStream os = Files.newOutputStream(tempJar);
			JarOutputStream tempOut = new JarOutputStream(os)) {
			for(JarEntry entry = in.getNextJarEntry(); entry != null; entry = in.getNextJarEntry()) {
				tempOut.putNextEntry(new ZipEntry(entry.getName()));

				if(!entry.isDirectory()) IOUtils.copyLarge(in, tempOut);

				tempOut.closeEntry();
				tempOut.flush();
			}
		}
		try(JarFile input = new JarFile(tempJar.toFile())) {
			processJar(input, out);
		}

		Files.deleteIfExists(tempJar);
	}
	public static class FabricModRemapper extends Remapper {
		private static final BiFunction<INameMappingService.Domain, String, String> remapFunc = Metadata.remapFunc;
		@Override
		public String mapMethodName(String owner, String name, String descriptor) {
			return remapFunc.apply(INameMappingService.Domain.METHOD, name);
		}
		@Override
		public String mapFieldName(String owner, String name, String descriptor) {
			return remapFunc.apply(INameMappingService.Domain.FIELD, name);
		}
		@Override
		public String map(String internalName) {
			return remapFunc.apply(INameMappingService.Domain.CLASS, internalName);
		}
		private JsonObject mapRefMap(JsonObject object) {
			try {
				Method deepCopy = JsonObject.class.getDeclaredMethod("deepCopy");
				deepCopy.setAccessible(true);

				JsonObject mapped = new JsonObject();

				JsonObject mappings = (JsonObject) deepCopy.invoke(object.get("mappings").getAsJsonObject());
				JsonObject mappingsMapped = new JsonObject();
				mappings.entrySet().forEach(entry -> {
					JsonObject value = new JsonObject();
					entry.getValue().getAsJsonObject().entrySet().forEach(e -> value.addProperty(e.getKey(), remapValue(e.getValue().getAsString())));
					mappingsMapped.add(entry.getKey(), value);
				});
				mapped.add("mappings", mappingsMapped);

				JsonObject data = (JsonObject) deepCopy.invoke(object.get("data").getAsJsonObject());
				String naming = FMLEnvironment.naming;
				if(!naming.equalsIgnoreCase("srg") && !naming.equalsIgnoreCase("mcp")) throw new RuntimeException("Invalid naming!");

				JsonObject dataMappingMapped = new JsonObject();
				if(data.has("named:intermediary")) {
					data.get("named:intermediary").getAsJsonObject().entrySet().forEach(entry -> {
						String key = entry.getKey();
						JsonObject value = new JsonObject();
						entry.getValue().getAsJsonObject().entrySet()
								.forEach(e -> value.addProperty(e.getKey(), remapValue(e.getValue().getAsString())));
						dataMappingMapped.add(key, value);
					});
					data.add("named:" + naming, dataMappingMapped);
				}
				mapped.add("data", data);
				return mapped;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return object;
		}
		private String remapValue(String s) {
			int i = s.indexOf(';') + 1;
			boolean hasNoClsDescBeforeFieldName = s.contains(":") && s.indexOf(':') < i;
			boolean hasNoClsDescBeforeMethodName = s.contains("(") && s.contains(")") && s.indexOf('(') < i;
			String clsDesc;
			if(hasNoClsDescBeforeFieldName || hasNoClsDescBeforeMethodName) clsDesc = "";
			else clsDesc = s.substring(0, i);
			String sMapped = clsDesc.isEmpty() ? "" : mapDesc(clsDesc);
			if(s.contains("(") && s.contains(")")) { // method
				int j = s.indexOf('(');
				String methodName = hasNoClsDescBeforeMethodName ? s.substring(0, j) : s.substring(i, j);
				if(methodName.equals("<init>") || methodName.equals("<clinit>")) sMapped += methodName;
				else sMapped += remapFunc.apply(INameMappingService.Domain.METHOD, methodName);
				sMapped += mapMethodDesc(s.substring(j));
			} else if(s.contains(":")) { // field
				int j = s.indexOf(':');
				String fieldName = hasNoClsDescBeforeFieldName ? s.substring(0, j) : s.substring(i, j);
				sMapped += remapFunc.apply(INameMappingService.Domain.FIELD, fieldName);
				sMapped += ':';
				sMapped += mapDesc(s.substring(j + 1));
			} else { // class
				sMapped = map(s);
			}
			return sMapped;
		}
	}
}