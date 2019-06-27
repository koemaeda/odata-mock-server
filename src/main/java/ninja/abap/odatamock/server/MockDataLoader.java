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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.provider.EdmProvider;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderException;
import org.apache.olingo.odata2.api.ep.EntityProviderReadProperties;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.exception.ODataException;

import lombok.NonNull;

/**
 * OData Mock Data Loader
 * Loads mock data from JSON files in a local directory.
 */
class MockDataLoader {

	protected final Edm edm;
	protected final EdmProvider edmProvider;
	protected final Path directory;
	protected final MockDataStore dataStore;

	MockDataLoader(final @NonNull Edm edm, final @NonNull EdmProvider edmProvider,
			final @NonNull String path, final @NonNull MockDataStore dataStore) throws ODataException {
		this.edm = edm;
		this.edmProvider = edmProvider;
		this.directory = Paths.get(path);
		this.dataStore = dataStore;
	}

	/**
	 * Load data for the Entity Sets from local JSON files
	 * @param generateMissing Generate random data for files not found
	 * @throws ODataException
	 */
	public void load(boolean generateMissing) throws ODataException {
		MockDataGenerator generator = new MockDataGenerator(edm, edmProvider);

		for (EdmEntitySet entitySet : edm.getEntitySets()) {
			try {
				dataStore.putAll(entitySet.getName(), loadDataFromFile(entitySet.getName()));
			}
			catch (FileNotFoundException e) {
				// TODO - log warning

				if (generateMissing) {
					dataStore.putAll(entitySet.getName(), generator.generate(entitySet.getName()));
				}
			}
			catch (Exception e) {
				String error = String.format("Error loading data for %s from %s: %s",
					entitySet.getName(), directory, e.getMessage());
				throw new ODataException(error, e);
			}
		}
	}

	/**
	 * Load Entity Set data from the corresponding local file
	 * @param name EntitySet name
	 * @return Raw data loaded from JSON
	 * @throws IOException 
	 * @throws EdmException 
	 * @throws EntityProviderException 
	 */
	protected List<Map<String, Object>> loadDataFromFile(String name) throws IOException, EntityProviderException, EdmException {
		// Check if file exists
		File file = directory.resolve(name + ".json").toFile();
		if (! file.exists())
			throw new FileNotFoundException(file.getPath());

		// Read JSON from file
		InputStream json = new ByteArrayInputStream(Files.readAllBytes(file.toPath()));

		// Parse the JSON file using Olingo client
		ODataFeed feed = EntityProvider.readFeed("application/json",
		        edm.getDefaultEntityContainer().getEntitySet(name), json,
		        EntityProviderReadProperties.init().build());

		return feed.getEntries().stream()
				.map(e -> e.getProperties())
				.collect(Collectors.toList());
	}

}
