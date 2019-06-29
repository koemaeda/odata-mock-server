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
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.olingo.odata2.api.edm.provider.EdmProvider;
import org.apache.olingo.odata2.api.edm.provider.EntityContainer;
import org.apache.olingo.odata2.api.edm.provider.EntitySet;
import org.apache.olingo.odata2.api.edm.provider.EntityType;
import org.apache.olingo.odata2.api.edm.provider.PropertyRef;
import org.apache.olingo.odata2.api.edm.provider.Schema;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;

import lombok.Getter;
import lombok.NonNull;

/**
 * Simple in-memory data store for mock data
 * Entity Sets and their data are stored in a tree of hash maps.
 *  
 * Implementation loosely based on org.apache.olingo.odata2.annotation.processor.core.datasource.DataStore
 */
public class MockDataStore {

	protected final EdmProvider edmProvider;

	/**
	 * Mock data is stored in a Map hierarchy: 
	 * Entity Set name / Record Key / Field values
	 */
	@Getter
	protected final Map<String,  // Entity Set name
		LinkedHashMap<           // (records are stored in the order they are inserted)
			Map<String, Object>, // Key fields
			Map<String, Object>  // All fields
		>> data = new HashMap<>();

	/**
	 * Entity Set name to EntityType definition
	 */
	protected Map<String, EntityType> entityTypes = new HashMap<>();

	MockDataStore(final @NonNull EdmProvider edmProvider) throws ODataException {
		this.edmProvider = edmProvider;

		// Initialize the containers/collections
		for (Schema edmSchema : edmProvider.getSchemas()) {
			for (EntityContainer edmContainer : edmSchema.getEntityContainers()) {
				for (EntitySet edmES : edmContainer.getEntitySets()) {
					data.put(edmES.getName(), new LinkedHashMap<>());

					EntityType edmET = edmProvider.getEntityType(edmES.getEntityType());
					entityTypes.put(edmES.getName(), edmET);
				}
			}
		}
	}

	/**
	 * Get stored records for an Entity Set
	 * @param entitySet Entity Set name
	 * @return The stored records. Returned List is a copy of the actual stored data.
	 * There are no guarantees on the type, mutability, serializability, or thread-safety of the List returned. 
	 * @throws ODataApplicationException If the Entity Set does not exist in the mocked OData service.
	 */
	public List<Map<String, Object>> getEntitySet(String entitySet) throws ODataApplicationException {
		if (! data.containsKey(entitySet))
			throw new ODataApplicationException(String.format("Entity Set %s not found", entitySet),
					Locale.getDefault());

		return data.get(entitySet).values().stream().collect(Collectors.toList());
	}

	/**
	 * Inserts a record into an Entity Set
	 * @param entitySet Entity Set name
	 * @param record New record to be added to the stored data (map of fields) 
	 * @throws ODataApplicationException If the Entity Set does not exist in the mocked OData service
	 *   or there's already an existing record with the same key.
	 */
	public void insert(String entitySet, Map<String, Object> record) throws ODataApplicationException {
		LinkedHashMap<Map<String, Object>, Map<String, Object>>	esData = data.get(entitySet);
		if (esData == null)
			throw new ODataApplicationException(String.format("Entity Set %s not found", entitySet),
					Locale.getDefault());

		Map<String, Object> key = getRecordKey(entitySet, record);
		if (esData.containsKey(key))
			throw new ODataApplicationException(String.format("Cannot insert duplicate record key in %s", entitySet),
					Locale.getDefault());

		esData.put(key, record);
	}

	/**
	 * Inserts/updates a record into an Entity Set
	 * @param entitySet Entity Set name
	 * @param record New record to be added/updated to the stored data (map of fields) 
	 * @throws ODataApplicationException If the Entity Set does not exist in the mocked OData service
	 */
	public void put(String entitySet, Map<String, Object> record) throws ODataApplicationException {
		LinkedHashMap<Map<String, Object>, Map<String, Object>>	esData = data.get(entitySet);
		if (esData == null)
			throw new ODataApplicationException(String.format("Entity Set %s not found", entitySet),
					Locale.getDefault());

		Map<String, Object> key = getRecordKey(entitySet, record);
		esData.put(key, record);
	}

	/**
	 * Inserts/updates multiple records into an Entity Set
	 * @param entitySet Entity Set name
	 * @param records New records to be added/updated to the stored data (maps of fields)
	 * @throws ODataApplicationException If the Entity Set does not exist in the mocked OData service
	 */
	public void putAll(String entitySet, Iterable<Map<String, Object>> records) throws ODataApplicationException {
		LinkedHashMap<Map<String, Object>, Map<String, Object>>	esData = data.get(entitySet);
		if (esData == null)
			throw new ODataApplicationException(String.format("Entity Set %s not found", entitySet),
					Locale.getDefault());

		for (Map<String, Object> record : records) {
			Map<String, Object> key = getRecordKey(entitySet, record);
			esData.put(key, record);
		}
	}

	/**
	 * Removes a record from an Entity Set
	 * @param entitySet Entity Set name
	 * @param key Record key fields
	 * @return The previous record associated with key, or null if there was no mapping for key.
	 * @throws ODataApplicationException If the Entity Set does not exist in the mocked OData service.
	 */
	public Map<String, Object> remove(String entitySet, Map<String, Object> key) throws ODataApplicationException {
		LinkedHashMap<Map<String, Object>, Map<String, Object>>	esData = data.get(entitySet);
		if (esData == null)
			throw new ODataApplicationException(String.format("Entity Set %s not found", entitySet), Locale.getDefault());

		return esData.remove(key);
	}

	/**
	 * Read an Entity Set record by its key fields
	 * @param entitySet Entity Set name
	 * @param key Record key fields
	 * @return The record associated with key.
	 * @throws ODataApplicationException If the Entity Set does not exist in the mocked OData service.
	 */
	public Map<String, Object> getRecordByKey(String entitySet, Map<String, Object> key) throws ODataApplicationException {
		LinkedHashMap<Map<String, Object>, Map<String, Object>>	esData = data.get(entitySet);
		if (esData == null)
			throw new ODataApplicationException(String.format("Entity Set %s not found", entitySet), Locale.getDefault());

		return esData.get(key);
	}

	/**
	 * Removes all stored records for an Entity Set
	 * @param entitySet Entity Set name
	 * @throws ODataApplicationException If the Entity Set does not exist in the mocked OData service.
	 */
	public void truncate(String entitySet) throws ODataApplicationException {
		LinkedHashMap<Map<String, Object>, Map<String, Object>>	esData = data.get(entitySet);
		if (esData == null)
			throw new ODataApplicationException(String.format("Entity Set %s not found", entitySet), Locale.getDefault());

		esData.clear();
	}

	/**
	 * Removes all stored data for ALL Entity Sets
	 */
	public void clear() {
		data.keySet().parallelStream().forEach(name -> data.get(name).clear());
	}


	protected Map<String, Object> getRecordKey(String entitySet, Map<String, Object> record)
			throws ODataApplicationException {
		Map<String, Object> keyFields = new HashMap<>();
		EntityType entityType = entityTypes.get(entitySet);
		for (PropertyRef keyProp : entityType.getKey().getKeys()) {
			String propName = keyProp.getName();
			keyFields.put(propName, record.get(propName));
		}
		return keyFields;
	}

}
