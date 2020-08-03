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
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.entrypoint.minecraft.hooks.EntrypointUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.nio.file.Path;
import java.util.EnumSet;

public class PreLaunchEntrypointLaunchPlugin implements ILaunchPluginService {
	@Override
	public String name() {
		return "prelaunchentrypointinvoker";
	}

	@Override
	public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) { return EnumSet.noneOf(Phase.class); }
	@Override
	public boolean processClass(Phase phase, ClassNode classNode, Type classType) { return false; }

	@Override
	public void initializeLaunch(ITransformerLoader transformerLoader, Path[] specialPaths) {
		EntrypointUtils.invoke("preLaunch", PreLaunchEntrypoint.class, PreLaunchEntrypoint::onPreLaunch);
	}
}