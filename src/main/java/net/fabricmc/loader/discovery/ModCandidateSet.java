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

import net.fabricmc.loader.api.Version;
import java.util.*;
import java.util.stream.Collectors;

public class ModCandidateSet {
	private final String modId;
	private final Set<ModCandidate> depthZeroCandidates = new HashSet<>();
	private final Map<String, ModCandidate> candidates = new HashMap<>();

	private static int compare(ModCandidate a, ModCandidate b) {
		Version av = a.getInfo().getVersion();
		Version bv = b.getInfo().getVersion();

		if (av instanceof Comparable && bv instanceof Comparable) {
			//noinspection unchecked
			return ((Comparable) bv).compareTo(av);
		} else {
			return 0;
		}
	}

	public ModCandidateSet(String modId) {
		this.modId = modId;
	}

	public String getModId() {
		return modId;
	}

	public boolean add(ModCandidate candidate) {
		String version = candidate.getInfo().getVersion().getFriendlyString();
		ModCandidate oldCandidate = candidates.get(version);
		if (oldCandidate != null) {
			int oldDepth = oldCandidate.getDepth();
			int newDepth = candidate.getDepth();

			if (oldDepth <= newDepth) {
				return false;
			} else {
				candidates.remove(version);
				if (oldDepth > 0) {
					depthZeroCandidates.remove(oldCandidate);
				}
			}
		}

		candidates.put(version, candidate);
		if (candidate.getDepth() == 0) {
			depthZeroCandidates.add(candidate);
		}

		return true;
	}

	public boolean isUserProvided() {
		return !depthZeroCandidates.isEmpty();
	}

	public Collection<ModCandidate> toSortedSet() throws ModResolutionException {
		if (depthZeroCandidates.size() > 1) {
			String modVersions = depthZeroCandidates.stream()
				.map((c) -> "[" + c.getInfo().getVersion() + " at " + c.getOriginUrl().getFile() + "]")
				.collect(Collectors.joining(", "));

			throw new ModResolutionException("Duplicate versions for mod ID '" + modId + "': " + modVersions);
		} else if (depthZeroCandidates.size() == 1) {
			return depthZeroCandidates;
		} else if (candidates.size() > 1) {
			List<ModCandidate> out = new ArrayList<>(candidates.values());
			out.sort(ModCandidateSet::compare);
			return out;
		} else {
			return Collections.singleton(candidates.values().iterator().next());
		}
	}
}
