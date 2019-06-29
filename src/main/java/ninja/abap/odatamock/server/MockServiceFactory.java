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

import org.apache.olingo.odata2.annotation.processor.core.datasource.DataSource;
import org.apache.olingo.odata2.api.ODataService;
import org.apache.olingo.odata2.api.ODataServiceFactory;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.core.edm.provider.EdmxProvider;
import org.apache.olingo.odata2.core.processor.ODataSingleProcessorService;

import lombok.NonNull;

class MockServiceFactory extends ODataServiceFactory {

	protected final EdmxProvider edmProvider;
	protected final DataSource dataSource;

	protected final MapValueAccess valueAccess;
	protected final MockListsProcessor processor;

	MockServiceFactory(final @NonNull EdmxProvider edmProvider, DataSource dataSource)
			throws ODataException, IOException {
		this.edmProvider = edmProvider;
		this.dataSource = dataSource;

		this.valueAccess = new MapValueAccess();
		this.processor = new MockListsProcessor(dataSource, valueAccess);
	}

	@Override
	public ODataService createService(ODataContext ctx) throws ODataException {
		return new ODataSingleProcessorService(edmProvider, processor);
	}

}