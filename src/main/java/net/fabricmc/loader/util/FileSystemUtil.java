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

package net.fabricmc.loader.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;

public final class FileSystemUtil {
    public static class FileSystemDelegate implements AutoCloseable {
        private final FileSystem fileSystem;
        private final boolean owner;

        public FileSystemDelegate(FileSystem fileSystem, boolean owner) {
            this.fileSystem = fileSystem;
            this.owner = owner;
        }

        public FileSystem get() {
            return fileSystem;
        }

        @Override
        public void close() throws IOException {
            if (owner) {
                fileSystem.close();
            }
        }
    }

    private FileSystemUtil() {

    }

    private static final Map<String, String> jfsArgsCreate = new HashMap<>();
    private static final Map<String, String> jfsArgsEmpty = new HashMap<>();

    static {
        jfsArgsCreate.put("create", "true");
    }

	public static FileSystemDelegate getJarFileSystem(File file, boolean create) throws IOException {
    	return getJarFileSystem(file.toURI(), create);
	}

	public static FileSystemDelegate getJarFileSystem(Path path, boolean create) throws IOException {
		return getJarFileSystem(path.toUri(), create);
	}

	public static FileSystemDelegate getJarFileSystem(URI uri, boolean create) throws IOException {
        URI jarUri;
        try {
            jarUri = new URI("jar:" + uri.getScheme(), uri.getHost(), uri.getPath(), uri.getFragment());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }

        try {
            return new FileSystemDelegate(FileSystems.newFileSystem(jarUri, create ? jfsArgsCreate : jfsArgsEmpty), true);
        } catch (FileSystemAlreadyExistsException e) {
            return new FileSystemDelegate(FileSystems.getFileSystem(jarUri), false);
        }
    }
}
