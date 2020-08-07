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

package net.fabricmc.loader.discovery;

import net.fabricmc.loader.metadata.LoaderModMetadata;

import java.net.URL;

public class ModCandidate {
	private final LoaderModMetadata info;
	private final URL originUrl;
	private final int depth;

	public ModCandidate(LoaderModMetadata info, URL originUrl, int depth) {
		this.info = info;
		this.originUrl = originUrl;
		this.depth = depth;
	}

	public URL getOriginUrl() {
		return originUrl;
	}

	public LoaderModMetadata getInfo() {
		return info;
	}

	public int getDepth() {
		return depth;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ModCandidate)) {
			return false;
		} else {
			ModCandidate other = (ModCandidate) obj;
			return other.info.getVersion().getFriendlyString().equals(info.getVersion().getFriendlyString()) && other.info.getId().equals(info.getId());
		}
	}

	@Override
	public int hashCode() {
		return info.getId().hashCode() * 17 + info.getVersion().hashCode();
	}

	@Override
	public String toString() {
		return "ModCandidate{" + info.getId() + "@" + info.getVersion().getFriendlyString() + "}";
	}
}
