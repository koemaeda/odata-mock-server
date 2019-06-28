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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.olingo.odata2.annotation.processor.core.datasource.DataSource;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmFunctionImport;
import org.apache.olingo.odata2.api.edm.provider.EdmProvider;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.exception.ODataNotFoundException;
import org.apache.olingo.odata2.api.exception.ODataNotImplementedException;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ninja.abap.odatamock.event.FunctionImportHandler;

/**
 * Implementation based on org.apache.olingo.odata2.annotation.processor.core.datasource.AnnotationInMemoryDs
 */
@RequiredArgsConstructor
class MockDataSource implements DataSource {

	protected final EdmProvider edmProvider;
	@Getter
	protected final MockDataStore dataStore;

	@Getter
	protected final Map<String, FunctionImportHandler> functionImportHandlers = new HashMap<>();

	MockDataSource(final @NonNull EdmProvider edmProvider) throws ODataException {
		this.edmProvider = edmProvider;
		this.dataStore = new MockDataStore(edmProvider);
	}

	@Override
	public List<?> readData(EdmEntitySet entitySet)
			throws ODataNotImplementedException, ODataNotFoundException, EdmException, ODataApplicationException {
		return dataStore.getEntitySet(entitySet.getName());
	}

	@Override
	public Object readData(EdmEntitySet entitySet, Map<String, Object> keys)
			throws ODataNotImplementedException, ODataNotFoundException, EdmException, ODataApplicationException {
		return dataStore.getRecordByKey(entitySet.getName(), keys);
	}

	@Override
	public Object readData(EdmFunctionImport function, Map<String, Object> parameters, Map<String, Object> keys)
			throws ODataNotImplementedException, ODataNotFoundException, EdmException, ODataApplicationException {
		FunctionImportHandler handler = functionImportHandlers.get(function.getName());
		if (handler != null)
			return handler.handle(function, parameters, keys);
		else
			throw new ODataNotImplementedException();
	}

	@Override
	public Object readRelatedData(EdmEntitySet sourceEntitySet, Object sourceData, EdmEntitySet targetEntitySet,
			Map<String, Object> targetKeys)
			throws ODataNotImplementedException, ODataNotFoundException, EdmException, ODataApplicationException {
		throw new ODataNotImplementedException();
	}

	@Override
	public BinaryData readBinaryData(EdmEntitySet entitySet, Object mediaLinkEntryData)
			throws ODataNotImplementedException, ODataNotFoundException, EdmException, ODataApplicationException {
		throw new ODataNotImplementedException();
	}

	@Override
	public Object newDataObject(EdmEntitySet entitySet)
			throws ODataNotImplementedException, EdmException, ODataApplicationException {
		return new HashMap<String, Object>();
	}

	@Override
	public void writeBinaryData(EdmEntitySet entitySet, Object mediaLinkEntryData, BinaryData binaryData)
			throws ODataNotImplementedException, ODataNotFoundException, EdmException, ODataApplicationException {
		throw new ODataNotImplementedException();
	}

	@Override
	public void deleteData(EdmEntitySet entitySet, Map<String, Object> keys)
			throws ODataNotImplementedException, ODataNotFoundException, EdmException, ODataApplicationException {
		Map<String, Object> record = dataStore.getRecordByKey(entitySet.getName(), keys);
		if (record == null)
			throw new ODataNotFoundException(ODataNotFoundException.ENTITY);

		dataStore.remove(entitySet.getName(), record);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void createData(EdmEntitySet entitySet, Object data)
			throws ODataNotImplementedException, EdmException, ODataApplicationException {
		if (! (data instanceof Map))
			throw new ODataApplicationException("Inserted record is of invalid type " +
				data.getClass().getName(), Locale.getDefault());

		dataStore.put(entitySet.getName(), (Map<String, Object>) data);
	}

	@Override
	public void deleteRelation(EdmEntitySet sourceEntitySet, Object sourceData, EdmEntitySet targetEntitySet,
			Map<String, Object> targetKeys)
			throws ODataNotImplementedException, ODataNotFoundException, EdmException, ODataApplicationException {
		throw new ODataNotImplementedException();
	}

	@Override
	public void writeRelation(EdmEntitySet sourceEntitySet, Object sourceData, EdmEntitySet targetEntitySet,
			Map<String, Object> targetKeys)
			throws ODataNotImplementedException, ODataNotFoundException, EdmException, ODataApplicationException {
		throw new ODataNotImplementedException();
	}

}
