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

package io.github.fcworkgroupmc.f2c.f2c.launchplugins.fabric;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.fabricmc.loader.transformer.ClassStripper;
import net.fabricmc.loader.transformer.EnvironmentStrippingData;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.EnumSet;

import static io.github.fcworkgroupmc.f2c.f2c.launchplugins.fabric.AccessWidenerLaunchPlugin.N;
import static io.github.fcworkgroupmc.f2c.f2c.launchplugins.fabric.AccessWidenerLaunchPlugin.Y;

public class F2CLaunchPlugin implements ILaunchPluginService {
	@Override
	public String name() {
		return "f2c";
	}

	@Override
	public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
		boolean isMinecraftClass = classType.getClassName().startsWith("net.minecraft.") || classType.getClassName().indexOf('.') < 0;
		return isMinecraftClass ? N : Y;
	}
	@Override
	public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
		EnvironmentStrippingData stripData = new EnvironmentStrippingData(Opcodes.ASM6, FabricLauncherBase.getLauncher().getEnvironmentType().toString());
		classNode.accept(stripData);
		if (stripData.stripEntireClass()) {
			throw new RuntimeException("Cannot load class " + classType.getClassName() + " in environment type " + FabricLauncherBase.getLauncher().getEnvironmentType());
		}
		if (!stripData.isEmpty()) {
			classNode.accept(new ClassStripper(Opcodes.ASM6, classNode, stripData.getStripInterfaces(), stripData.getStripFields(), stripData.getStripMethods()));
			return true;
		}
		return false;
	}
}