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

package net.fabricmc.loader.entrypoint.minecraft.hooks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class EntrypointBranding {
	public static final String FABRIC = "fabric-with-f2c"; // F2C - add "with F2C"
	public static final String VANILLA = "vanilla";

	private static final Logger LOGGER = LogManager.getLogger("Fabric|Branding");

	private EntrypointBranding() {
	}

	public static String brand(final String brand) {
		if (brand == null || brand.isEmpty()) {
			LOGGER.warn("Null or empty branding found!", new IllegalStateException());
			return FABRIC;
		}
		return VANILLA.equals(brand) ? FABRIC : brand + ',' + FABRIC;
	}
}
