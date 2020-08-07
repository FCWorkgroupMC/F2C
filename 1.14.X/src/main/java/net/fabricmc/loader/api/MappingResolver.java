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

package net.fabricmc.loader.api;

import java.util.Collection;

/**
 * Helper class for performing mapping resolution.
 *
 * <p><strong>Note</strong>: The target namespace (the one being mapped to) for mapping (or the
 * source one for unmapping) is always implied to be the one Loader is
 * currently operating in.</p>
 *
 * <p>All the {@code className} used in this resolver are in <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.1">binary names</a>,
 * such as {@code "mypackage.MyClass$Inner"}.</p>
 *
 * @since 0.4.1
 */
public interface MappingResolver {
	/**
	 * Get the list of all available mapping namespaces in the loaded instance.
	 *
	 * @return The list of all available namespaces.
	 */
	Collection<String> getNamespaces();

	/**
	 * Get the current namespace being used at runtime.
	 *
	 * @return the runtime namespace
	 */
	String getCurrentRuntimeNamespace();

	/**
	 * Map a class name to the mapping currently used at runtime.
	 *
	 * @param namespace the namespace of the provided class name
	 * @param className the provided binary class name
	 * @return the mapped class name, or {@code className} if no such mapping is present
	 */
	String mapClassName(String namespace, String className);

	/**
	 * Unmap a class name to the mapping currently used at runtime.
	 *
	 * @param targetNamespace The target namespace for unmapping.
	 * @param className the provided binary class name of the mapping form currently used at runtime
	 * @return the mapped class name, or {@code className} if no such mapping is present
	 */
	String unmapClassName(String targetNamespace, String className);

	/**
	 * Map a field name to the mapping currently used at runtime.
	 *
	 * @param namespace the namespace of the provided field name and descriptor
	 * @param owner the binary name of the owner class of the field
	 * @param name the name of the field
	 * @param descriptor the descriptor of the field
	 * @return the mapped field name, or {@code name} if no such mapping is present
	 */
	String mapFieldName(String namespace, String owner, String name, String descriptor);

	/**
	 * Map a method name to the mapping currently used at runtime.
	 *
	 * @param namespace the namespace of the provided method name and descriptor
	 * @param owner the binary name of the owner class of the method
	 * @param name the name of the method
	 * @param descriptor the descriptor of the method
	 * @return the mapped method name, or {@code name} if no such mapping is present
	 */
	String mapMethodName(String namespace, String owner, String name, String descriptor);
}
