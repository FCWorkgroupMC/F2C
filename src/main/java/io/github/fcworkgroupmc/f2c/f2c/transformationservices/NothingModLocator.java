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

package io.github.fcworkgroupmc.f2c.f2c.transformationservices;

import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModLocator;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.jar.Manifest;

public class NothingModLocator implements IModLocator {
	@Override
	public List<IModFile> scanMods() { return Collections.emptyList(); }
	@Override
	public String name() { return "nothing"; }
	@Override
	public Path findPath(IModFile modFile, String... path) { return null; }
	@Override
	public void scanFile(IModFile modFile, Consumer<Path> pathConsumer) { }
	@Override
	public Optional<Manifest> findManifest(Path file) { return Optional.empty(); }
	@Override
	public void initArguments(Map<String, ?> arguments) { }
	@Override
	public boolean isValid(IModFile modFile) { return true; }
}