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
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.olingo.odata2.api.ODataService;
import org.apache.olingo.odata2.api.ODataServiceFactory;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.core.edm.provider.EdmxProvider;
import org.apache.olingo.odata2.core.processor.ODataSingleProcessorService;

import lombok.Getter;
import lombok.NonNull;

class MockServiceFactory extends ODataServiceFactory {

	@Getter
	protected final String edmx;
	@Getter
	protected final Edm edm;
	@Getter
	protected final EdmxProvider edmProvider;
	@Getter
	protected final MockDataSource dataSource;

	protected final MapValueAccess valueAccess;
	protected final MockListsProcessor processor;

	MockServiceFactory(final @NonNull InputStream edmx) throws ODataException, IOException {
		// Consume and keep the Edmx stream to later be able to parse it
		this.edmx = IOUtils.toString(edmx, StandardCharsets.UTF_8);

		// Parse the provided metadata file using Olingo and initialize the
		//  in-memory Olingo data store and processor. 
		this.edm = EntityProvider.readMetadata(IOUtils.toInputStream(this.edmx, StandardCharsets.UTF_8), true);
		this.edmProvider = new EdmxProvider().parse(IOUtils.toInputStream(this.edmx, StandardCharsets.UTF_8), true);

		this.dataSource = new MockDataSource(edmProvider);
		this.valueAccess = new MapValueAccess();
		this.processor = new MockListsProcessor(dataSource, valueAccess);
	}

	@Override
	public ODataService createService(ODataContext ctx) throws ODataException {
		return new ODataSingleProcessorService(edmProvider, processor);
	}

}