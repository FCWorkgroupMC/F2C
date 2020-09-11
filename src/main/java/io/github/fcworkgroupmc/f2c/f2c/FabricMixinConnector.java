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

import cpw.mods.modlauncher.TransformingClassLoader;
import io.github.fcworkgroupmc.f2c.f2c.fabric.FabricLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.entrypoint.minecraft.hooks.EntrypointUtils;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.fabricmc.loader.metadata.LoaderModMetadata;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

import java.util.Arrays;
import java.util.List;

public class FabricMixinConnector implements IMixinConnector {
	public static final List<String> SKIPPED = Arrays.asList("com.google.common.jimfs.", "io.github.fcworkgroupmc.f2c.f2c.fabric.", "net.fabricmc.api.",
			"net.fabricmc.loader", "org.spongepowered.asm.mixin.transformer.FabricMixinTransformerProxy", "io.github.fcworkgroupmc.f2c.f2c.Metadata");
	@Override
	public void connect() {
		TransformingClassLoader classLoader = (TransformingClassLoader) Thread.currentThread().getContextClassLoader();
		classLoader.addTargetPackageFilter(s -> SKIPPED.stream().noneMatch(s::startsWith));

		if(Metadata.disableFabricLoader) return;
		// F2C - Remove net.fabricmc.loader.launch.common.FabricMixinBootstrap
		EnvType envType = FabricLauncherBase.getLauncher().getEnvironmentType();
		FabricLoader.INSTANCE.getAllMods().stream()
				.map(ModContainer::getMetadata)
				.filter((m) -> m instanceof LoaderModMetadata)
				.flatMap((m) -> ((LoaderModMetadata) m).getMixinConfigs(envType).stream())
				.filter(s -> s != null && !s.isEmpty())
				.forEach(Mixins::addConfiguration);

		EntrypointUtils.invoke("preLaunch", PreLaunchEntrypoint.class, PreLaunchEntrypoint::onPreLaunch);
	}
}