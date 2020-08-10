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

package net.fabricmc.loader.util.version;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import net.fabricmc.loader.api.VersionParsingException;

public final class SemanticVersionPredicateParser {
	private static final Map<String, Function<SemanticVersionImpl, Predicate<SemanticVersionImpl>>> PREFIXES;

	public static Predicate<SemanticVersionImpl> create(String text) throws VersionParsingException {
		List<Predicate<SemanticVersionImpl>> predicateList = new ArrayList<>();
		List<SemanticVersionImpl> prereleaseVersions = new ArrayList<>();

		for (String s : text.split(" ")) {
			s = s.trim();
			if (s.isEmpty() || s.equals("*")) {
				continue;
			}

			Function<SemanticVersionImpl, Predicate<SemanticVersionImpl>> factory = null;
			for (String prefix : PREFIXES.keySet()) {
				if (s.startsWith(prefix)) {
					factory = PREFIXES.get(prefix);
					s = s.substring(prefix.length());
					break;
				}
			}

			SemanticVersionImpl version = new SemanticVersionImpl(s, true);
			if (version.isPrerelease()) {
				prereleaseVersions.add(version);
			}

			if (factory == null) {
				factory = PREFIXES.get("=");
			} else if (version.hasWildcard()) {
				throw new VersionParsingException("Prefixed ranges are not allowed to use X-ranges!");
			}

			predicateList.add(factory.apply(version));
		}

		if (predicateList.isEmpty()) {
			return (s) -> true;
		}

		return (s) -> {
			for (Predicate<SemanticVersionImpl> p : predicateList) {
				if (!p.test(s)) {
					return false;
				}
			}

			return true;
		};
	}

	static {
		// Make sure to keep this sorted in order of length!
		PREFIXES = new LinkedHashMap<>();
		PREFIXES.put(">=", (target) -> (source) -> source.compareTo(target) >= 0);
		PREFIXES.put("<=", (target) -> (source) -> source.compareTo(target) <= 0);
		PREFIXES.put(">", (target) -> (source) -> source.compareTo(target) > 0);
		PREFIXES.put("<", (target) -> (source) -> source.compareTo(target) < 0);
		PREFIXES.put("=", (target) -> (source) -> source.compareTo(target) == 0);
		PREFIXES.put("~", (target) -> (source) -> source.compareTo(target) >= 0
				&& source.getVersionComponent(0) == target.getVersionComponent(0)
				&& source.getVersionComponent(1) == target.getVersionComponent(1));
		PREFIXES.put("^", (target) -> (source) -> source.compareTo(target) >= 0
				&& source.getVersionComponent(0) == target.getVersionComponent(0));
	}
}
