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
import io.github.fcworkgroupmc.f2c.f2c.fabric.FabricLoader;
import net.fabricmc.loader.transformer.accesswidener.AccessWidener;
import net.fabricmc.mappings.EntryTriple;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.nio.file.Path;
import java.util.*;

public class AccessWidenerLaunchPlugin implements ILaunchPluginService {
	@Override
	public String name() {
		return "accesswidener";
	}

	private static final EnumSet<Phase> Y = EnumSet.of(Phase.BEFORE);
	private static final EnumSet<Phase> N = EnumSet.noneOf(Phase.class);
	@Override
	public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
		return classType.getClassName().startsWith("net.minecraft.")
				&& !isEmpty
				&& FabricLoader.INSTANCE.getAccessWidener().getTargets().contains(classType.getClassName())
				? Y : N;
	}

	@Override
	public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
		return false;
	}

	@Override
	public void initializeLaunch(ITransformerLoader transformerLoader, Path[] specialPaths) {

	}
}