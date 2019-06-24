/*
 * Copyright (C) 2019 Guilherme Maeda
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ninja.abap.odatamock.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * Fluent builder / Options for ODataServer 
 */
@Data
@Accessors(fluent = true)
public class ODataMockServerBuilder {

	/**
	 * Edmx (metadata) file contents as Stream.
	 */
	protected InputStream edmx;

	/**
	 * Port the HTTP server will listen to.
	 * If left empty, a random free port will be used.
	 */
	protected int portNumber = 0;

	/**
	 * Root path for the OData service (eg "/my-odata-service").
	 * Defaults is / (server root).
	 */
	protected String rootPath = "/";

	/**
	 * Local directory for serving Entity Set data from.
	 * Files in this directory must have the same name as the Entity Sets and ".json" extension.
	 */
	protected String localDataPath = null;

	/**
	 * Generate mock data for missing .json files that are not found in localDataPath.
	 * Default value is false.
	 */
	protected boolean generateMissing = false;

	/**
	 * Read the Edmx metadata from a local file.
	 */
	public ODataMockServerBuilder edmxFromFile(@NonNull String edmxPath) throws IOException {
		this.edmx = Files.newInputStream(Paths.get(edmxPath), StandardOpenOption.READ);
		return this;
	}

	/**
	 * Create the OData server from the defined options.
	 * The server is automatically started upon creation.
	 * @return ODataServer
	 */
	public ODataMockServer build() throws Exception {
		return new ODataMockServer(this);
	}

}
