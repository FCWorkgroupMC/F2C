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

import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileLocator;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.forgespi.locating.IModFile;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SelfModLocator extends AbstractJarFileLocator {
	@Override
	public List<IModFile> scanMods() {
		if(Metadata.isDevelopment()) return Collections.emptyList();
		try {
			return Stream.of(Paths.get(Metadata.location.toURI()))
					.map(p->ModFile.newFMLInstance(p, this))
					.peek(f->modJars.compute(f, (mf, fs)->createFileSystem(mf)))
					.collect(Collectors.toList());
		} catch (URISyntaxException e) {
			throw new RuntimeException("Add \"this\" mod to forge failed", e);
		}
	}
	@Override
	public String name() {
		return "add self";
	}
	@Override
	public void initArguments(Map<String, ?> arguments) {}
}