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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmProperty;
import org.apache.olingo.odata2.api.edm.provider.EdmProvider;
import org.apache.olingo.odata2.api.edm.provider.EntityContainer;
import org.apache.olingo.odata2.api.edm.provider.EntitySet;
import org.apache.olingo.odata2.api.edm.provider.Schema;
import org.apache.olingo.odata2.api.exception.ODataException;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * 
 * Implementation based on org.apache.olingo.odata2.annotation.processor.core.datasource.DataStore
 *
 */
class MockDataStore {

	protected final EdmProvider edmProvider;

	@Getter
	protected final MapServiceData data = new MapServiceData();

	MockDataStore(final @NonNull EdmProvider edmProvider) throws ODataException {
		this.edmProvider = edmProvider;

		// Initialize the containers/collections
		for (Schema edmSchema : edmProvider.getSchemas()) {
			for (EntityContainer edmContainer : edmSchema.getEntityContainers()) {
				MapEntityContainer container = new MapEntityContainer();
				data.put(edmContainer.getName(), container);
				
				for (EntitySet edmES : edmContainer.getEntitySets()) {
					MapCollection col = new MapCollection();
					container.put(edmES.getName(), col);
				}
			}
		}
	}

	public MapEntityContainer getEntityContainer(String name) {
		return data.get(name);
	}

	public MapCollection getEntitySet(EdmEntitySet entitySet) throws EdmException {
		MapEntityContainer container = getEntityContainer(entitySet.getEntityContainer().getName());
		return container.get(entitySet.getName());
	}

	public void storeRecord(EdmEntitySet entitySet, Map<String, Object> fields) throws EdmException {
		MapCollection col = getEntitySet(entitySet);
		MapEntityRecord record = fields instanceof MapEntityRecord
			? (MapEntityRecord) fields : new MapEntityRecord(fields);
		MapEntityKey key = getRecordKey(entitySet, record);
		col.put(key, record);
	}

	public void storeRecords(EdmEntitySet entitySet, Iterable<Map<String, Object>> data) throws EdmException {
		MapCollection col = getEntitySet(entitySet);
		for (Map<String, Object> fields : data) {
			MapEntityRecord record = fields instanceof MapEntityRecord
					? (MapEntityRecord) fields : new MapEntityRecord(fields);
			MapEntityKey key = getRecordKey(entitySet, record);
			col.put(key, record);
		}
	}

	public void deleteRecord(EdmEntitySet entitySet, MapEntityRecord record) throws EdmException {
		MapCollection col = getEntitySet(entitySet);
		MapEntityKey key = getRecordKey(entitySet, record);
		col.remove(key);
	}

	public MapEntityRecord getRecordByKey(EdmEntitySet entitySet, Map<String, Object> key) throws EdmException {
		MapCollection col = getEntitySet(entitySet);
		return col.get(key);
	}


	protected MapEntityKey getRecordKey(EdmEntitySet entitySet, MapEntityRecord record) throws EdmException {
		MapEntityKey key = new MapEntityKey();
		for (EdmProperty keyProp : entitySet.getEntityType().getKeyProperties()) {
			String propName = keyProp.getName();
			key.put(propName, record.get(propName));
		}		
		return key;
	}

	//
	// Shorthand types for Lists/Maps data storage
	//

	@SuppressWarnings("serial")
	public static class MapEntityKey extends HashMap<String, Object> { }

	@SuppressWarnings("serial")
	@NoArgsConstructor
	public class MapEntityRecord extends HashMap<String, Object> {
		public MapEntityRecord(Map<String, Object> fields) {
			this.putAll(fields);
		}
	}

	@SuppressWarnings("serial")
	public static class MapCollection extends LinkedHashMap<MapEntityKey, MapEntityRecord> {
		public List<MapEntityRecord> asList() {
			return values().stream().collect(Collectors.toList());
		}
	}

	@SuppressWarnings("serial")
	public static class MapEntityContainer extends HashMap<String, MapCollection> { }

	@SuppressWarnings("serial")
	public static class MapServiceData extends HashMap<String, MapEntityContainer> { }

}
