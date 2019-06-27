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

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmEntityType;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmFacets;
import org.apache.olingo.odata2.api.edm.EdmLiteralKind;
import org.apache.olingo.odata2.api.edm.EdmSimpleType;
import org.apache.olingo.odata2.api.edm.EdmType;
import org.apache.olingo.odata2.api.edm.provider.EdmProvider;
import org.apache.olingo.odata2.api.edm.provider.EntityContainer;
import org.apache.olingo.odata2.api.edm.provider.EntitySet;
import org.apache.olingo.odata2.api.edm.provider.EntityType;
import org.apache.olingo.odata2.api.edm.provider.Property;
import org.apache.olingo.odata2.api.edm.provider.Schema;
import org.apache.olingo.odata2.api.exception.ODataException;

import lombok.NonNull;

/**
 * OData Mock Data Generator
 * Generates missing data for empty Entity Sets  
 */
class MockDataGenerator {

	protected final static int RECORD_COUNT = 50;

	protected final Edm edm;
	protected final EdmProvider edmProvider;

	protected final Map<String, EntitySet> entitySets = new HashMap<>();
	protected final Map<EdmEntityType, EntityType> etClient2Prov = new HashMap<>();

	MockDataGenerator(final @NonNull Edm edm, final @NonNull EdmProvider edmProvider)
			throws EdmException, ODataException {
		this.edm = edm;
		this.edmProvider = edmProvider;

		// Index Entity Sets
		for (Schema edmSchema : edmProvider.getSchemas()) {
			for (EntityContainer edmContainer : edmSchema.getEntityContainers()) {
				for (EntitySet provES : edmContainer.getEntitySets()) {
					entitySets.put(provES.getName(), provES);

					// Map Edm types from Client to Provider
					EdmEntitySet clientES = edm.getDefaultEntityContainer().getEntitySet(provES.getName());
					EntityType provET = edmProvider.getEntityType(provES.getEntityType());
					etClient2Prov.put(clientES.getEntityType(), provET);
				}
			}
		}
	}

	/**
	 * Generate automatic data for an Entity Set
	 * @param entitySet Entity Set name
	 * @return Generated data
	 * @throws EdmException 
	 */
	public List<Map<String, Object>> generate(String entitySet) throws EdmException {
		List<Map<String, Object>> result = new ArrayList<>(RECORD_COUNT);

		EdmEntityType entityType = edm.getDefaultEntityContainer().getEntitySet(entitySet).getEntityType();
		for (int i=1; i<=RECORD_COUNT; i++) {
			result.add(generateRecord(entityType, i));
		}

		return result;
	}

	/**
	 * Generate automatic Entity record for a record index
	 * @param entityType Edm Entity Type
	 * @param index Record index
	 * @return Map of record fields
	 * @throws EdmException
	 */
	public Map<String, Object> generateRecord(EdmEntityType entityType, int index)
			throws EdmException {
		int fieldCount = entityType.getPropertyNames().size();
		Map<String, Object> fields = new HashMap<>(fieldCount);

		// Read Provider EntityType
		EntityType provET = etClient2Prov.get(entityType);
		Map<String, Property> provProps = provET.getProperties().parallelStream()
				.collect(Collectors.toMap(e -> e.getName(), e -> e));

		for (String name : entityType.getPropertyNames()) {
			EdmType edmType = entityType.getProperty(name).getType();
			if (edmType instanceof EdmSimpleType) {
				EdmFacets facets = provProps.get(name).getFacets();
				fields.put(name, generateValue(name, (EdmSimpleType) edmType, facets, index));
			}
		}

		return fields;
	}

	/**
	 * Generate the Java value for an Edm type and a record index.
	 * @see https://www.odata.org/documentation/odata-version-2-0/overview/#AbstractTypeSystem
	 * 
	 * @param fieldName Field name
	 * @param edmType Field Edm type
	 * @param facets Edm Facets (optional)
	 * @param index Record index
	 * @return Appropriate Java object for field
	 * @throws EdmException
	 */
	public Object generateValue(String fieldName, EdmSimpleType edmType, EdmFacets facets, int index) throws EdmException {
		String strVal;

		// For DateTime fields
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_YEAR, index - (RECORD_COUNT / 2));

		switch (edmType.toString()) {
		case "Edm.Binary":
			strVal = Base64.encodeBase64String(String.format("%s %d", fieldName, index).getBytes());
			break;
		case "Edm.Boolean":
			strVal = Boolean.valueOf(Math.random() > 0.5).toString();
			break;
		case "Edm.Byte":
			strVal = Integer.toString(index % 255);
			break;
		case "Edm.DateTime":
			strVal = String.format("/Date(%d)/", calendar.getTimeInMillis());
			break;
		case "Edm.DateTimeOffset": // ISO 8601-ish
			strVal = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(calendar.getTime());
			break;
		case "Edm.Decimal":
		case "Edm.Double":
		case "Edm.Single":
			BigDecimal val = new BigDecimal(index);
			if (facets != null && facets.getScale() != null)
				val.setScale(facets.getScale());
			strVal = val.toPlainString();
			break;
		case "Edm.Guid":
			strVal = String.format("%8d-%4d-%4d-%4d-%12d", index, index, index, index, index).replace(' ', '0');
			break;
		case "Edm.Int16":
		case "Edm.Int32":
		case "Edm.Int64":
			strVal = String.format("%d", index);
			break;
		case "Edm.SByte":
			strVal = Integer.toString(index % 128);
			break;
		case "Edm.String":
			String indexStr = String.format(" %d", index);
			String namePrefix = fieldName;
			if (facets != null && facets.getMaxLength() != null
					&& namePrefix.length() + indexStr.length() > facets.getMaxLength())
				namePrefix = fieldName.substring(0, facets.getMaxLength() - indexStr.length());
			strVal = namePrefix + indexStr;
			break;
		case "Edm.Time":
			strVal = String.format("PT%dH00M", index);
			break;
		default:
			throw new EdmException(EdmException.TYPEPROBLEM.addContent(edmType.toString()));
		}

		return edmType.valueOfString(strVal, EdmLiteralKind.JSON, facets, edmType.getDefaultType());
	}

}
