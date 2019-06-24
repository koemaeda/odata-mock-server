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

import java.util.Map;
import org.apache.olingo.odata2.annotation.processor.core.datasource.ValueAccess;
import org.apache.olingo.odata2.api.edm.EdmMapping;
import org.apache.olingo.odata2.api.edm.EdmProperty;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.exception.ODataHttpException;
import org.apache.olingo.odata2.api.exception.ODataNotFoundException;

/**
 * OData entity value access implementation for simple Maps.
 * 
 * Implementation based on org.apache.olingo.odata2.annotation.processor.core.datasource.BeanPropertyAccess
 */
class MapValueAccess implements ValueAccess {

	@Override
	public <T> Object getPropertyValue(T data, EdmProperty property) throws ODataException {
		return getValue(data, property.getName());
	}
	@Override
	public <T, V> void setPropertyValue(T data, EdmProperty property, V value) throws ODataException {
		setValue(data, property.getName(), value);
	}

	@Override
	public <T> Class<?> getPropertyType(T data, EdmProperty property) throws ODataException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> Object getMappingValue(T data, EdmMapping mapping) throws ODataException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T, V> void setMappingValue(T data, EdmMapping mapping, V value) throws ODataException {
		// TODO Auto-generated method stub

	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getMap(Object data) {
		return (Map<String, Object>) data;
	}

	private <T> Object getValue(final T data, final String propertyName) throws ODataNotFoundException {
		try {
			return getMap(data).get(propertyName);
		}
		catch (Exception e) {
			throw new ODataNotFoundException(ODataHttpException.COMMON, e);
		}
	}

	private <T, V> void setValue(final T data, final String propertyName, final V value)
			throws ODataNotFoundException {
		try {
			getMap(data).put(propertyName, value);
		}
		catch (Exception e) {
			throw new ODataNotFoundException(ODataHttpException.COMMON, e);
		}
	}

}
