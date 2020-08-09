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

package io.github.fcworkgroupmc.f2c.f2c.fabric;

import io.github.fcworkgroupmc.f2c.f2c.transformationservices.FabricModTransformationService;
import net.fabricmc.loader.discovery.ModCandidateFinder;
import net.fabricmc.loader.util.UrlConversionException;
import net.fabricmc.loader.util.UrlUtil;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class ListModCandidateFinder implements ModCandidateFinder {
	private List<Path> mods;
	public ListModCandidateFinder(List<Path> mods) {
		this.mods = mods;
	}
	@Override
	public void findCandidates(FabricLoader loader, Consumer<URL> urlProposer) {
		mods.forEach((modPath) -> {
			if (!Files.isDirectory(modPath) && modPath.toString().endsWith(/*".jar"*/
					FabricModTransformationService.FABRIC_MOD_SUFFIX)) { // F2C - change suffix
				try {
					urlProposer.accept(UrlUtil.asUrl(modPath));
				} catch (UrlConversionException e) {
					throw new RuntimeException("Failed to convert URL for mod '" + modPath + "'!", e);
				}
			}
		});
	}
}