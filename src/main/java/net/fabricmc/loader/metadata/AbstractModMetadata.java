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

package net.fabricmc.loader.metadata;

import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;

public abstract class AbstractModMetadata implements ModMetadata {
	@Override
	public boolean containsCustomElement(String key) {
		return containsCustomValue(key);
	}

	@Override
	public JsonElement getCustomElement(String key) {
		CustomValue value = getCustomValue(key);

		return value != null ? convert(value) : null;
	}

	@Override
	public boolean containsCustomValue(String key) {
		return getCustomValues().containsKey(key);
	}

	@Override
	public CustomValue getCustomValue(String key) {
		return getCustomValues().get(key);
	}

	private static JsonElement convert(CustomValue value) {
		switch (value.getType()) {
		case ARRAY: {
			JsonArray ret = new JsonArray();

			for (CustomValue v : value.getAsArray()) {
				ret.add(convert(v));
			}

			return ret;
		}
		case BOOLEAN:
			return new JsonPrimitive(value.getAsBoolean());
		case NULL:
			return JsonNull.INSTANCE;
		case NUMBER:
			return new JsonPrimitive(value.getAsNumber());
		case OBJECT: {
			JsonObject ret = new JsonObject();

			for (Map.Entry<String, CustomValue> entry : value.getAsObject()) {
				ret.add(entry.getKey(), convert(entry.getValue()));
			}

			return ret;
		}
		case STRING:
			return new JsonPrimitive(value.getAsString());
		}

		throw new IllegalStateException();
	}
}
