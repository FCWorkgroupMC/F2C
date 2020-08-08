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

package net.fabricmc.loader.language;

import java.io.IOException;

@Deprecated
public interface LanguageAdapter {
	enum MissingSuperclassBehavior {
		RETURN_NULL,
		CRASH
	}

	default Object createInstance(String classString, Options options) throws ClassNotFoundException, LanguageAdapterException {
		try {
			Class c = JavaLanguageAdapter.getClass(classString, options);
			if (c != null) {
				return createInstance(c, options);
			} else {
				return null;
			}
		} catch (IOException e) {
			throw new LanguageAdapterException("I/O error!", e);
		}
	}

	Object createInstance(Class<?> baseClass, Options options) throws LanguageAdapterException;

	public static class Options {
		private MissingSuperclassBehavior missingSuperclassBehavior;

		public MissingSuperclassBehavior getMissingSuperclassBehavior() {
			return missingSuperclassBehavior;
		}

		public static class Builder {
			private final Options options;

			private Builder() {
				options = new Options();
			}

			public static Builder create() {
				return new Builder();
			}

			public Builder missingSuperclassBehaviour(MissingSuperclassBehavior value) {
				options.missingSuperclassBehavior = value;
				return this;
			}

			public Options build() {
				return options;
			}
		}
	}
}