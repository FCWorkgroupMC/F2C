/*
 * Copyright 2016 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

package net.fabricmc.loader.transformer;

import net.fabricmc.api.EnvType;
import io.github.fcworkgroupmc.f2c.f2c.fabric.FabricLoader; // F2C - reimplement net.fabricmc.loader.api.FabricLoader and delete net.fabricmc.loader.FabricLoader
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.fabricmc.loader.transformer.accesswidener.AccessWidenerVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public final class FabricTransformer {
	/*public static byte[] lwTransformerHook(String name, String transformedName, byte[] bytes) {
		boolean isDevelopment = FabricLauncherBase.getLauncher().isDevelopment();
		EnvType envType = FabricLauncherBase.getLauncher().getEnvironmentType();

		byte[] input = MinecraftGameProvider.TRANSFORMER.transform(name);
		if (input != null) {
			return FabricTransformer.transform(isDevelopment, envType, name, input);
		} else {
			if (bytes != null) {
				return FabricTransformer.transform(isDevelopment, envType, name, bytes);
			} else {
				return null;
			}
		}

	}*/ // F2C - Remove method

	public static byte[] transform(boolean isDevelopment, EnvType envType, String name, byte[] bytes) {
		boolean isMinecraftClass = name.startsWith("net.minecraft.") || name.indexOf('.') < 0;
		boolean transformAccess = isMinecraftClass && FabricLauncherBase.getLauncher().getMappingConfiguration().requiresPackageAccessHack();
		boolean environmentStrip = !isMinecraftClass || isDevelopment;
		boolean applyAccessWidener = isMinecraftClass && FabricLoader.INSTANCE.getAccessWidener().getTargets().contains(name);

		if (!transformAccess && !environmentStrip && !applyAccessWidener) {
			return bytes;
		}

		ClassReader classReader = new ClassReader(bytes);
		ClassWriter classWriter = new ClassWriter(0);
		ClassVisitor visitor = classWriter;
		int visitorCount = 0;

		if (applyAccessWidener) {
			visitor = new AccessWidenerVisitor(Opcodes.ASM8, visitor, FabricLoader.INSTANCE.getAccessWidener());
			visitorCount++;
		}

		if (transformAccess) {
			visitor = new PackageAccessFixer(Opcodes.ASM8, visitor);
			visitorCount++;
		}

		if (environmentStrip) {
			EnvironmentStrippingData stripData = new EnvironmentStrippingData(Opcodes.ASM8, envType.toString());
			classReader.accept(stripData, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
			if (stripData.stripEntireClass()) {
				throw new RuntimeException("Cannot load class " + name + " in environment type " + envType);
			}
			if (!stripData.isEmpty()) {
				visitor = new ClassStripper(Opcodes.ASM8, visitor, stripData.getStripInterfaces(), stripData.getStripFields(), stripData.getStripMethods());
				visitorCount++;
			}
		}

		if (visitorCount <= 0) {
			return bytes;
		}

		classReader.accept(visitor, 0);
		return classWriter.toByteArray();
	}
}
