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
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.olingo.odata2.annotation.processor.core.datasource.DataSource;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.core.edm.provider.EdmxProvider;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import lombok.Getter;
import lombok.NonNull;
import ninja.abap.odatamock.event.FunctionImportHandler;

@Getter
public class ODataMockServer {

	protected Server server;
	protected URI uri;

	/**
	 * Mock Data Store - Use this property to manipulate the served data.
	 */
	@Getter
	protected MockDataStore dataStore;

	@Getter
	protected final String edmx;
	@Getter
	protected final Edm edm;
	@Getter
	protected final EdmxProvider edmProvider;

	protected final ODataMockServerBuilder options;
	protected DataSource dataSource;
	protected MockServiceFactory serviceFactory;
	protected MockServlet servlet;

	/**
	 * Constructor - Initializes and starts the OData server.
	 * @param options OData server options
	 * @throws ODataException If the OData server fails to load
	 * @throws IOException If the Edmx file cannot be read
	 * @throws Exception If the Edmx parsing the HTTP server fails to start
	 */
	ODataMockServer(final @NonNull ODataMockServerBuilder options)
			throws ODataException, IOException, Exception {
		this.options = options;

		// Parse the provided metadata file using Olingo and initialize the
		//  in-memory Olingo data store and processor.
		this.edmx = IOUtils.toString(options.edmx(), StandardCharsets.UTF_8);
		this.edm = EntityProvider.readMetadata(IOUtils.toInputStream(this.edmx, StandardCharsets.UTF_8), true);
		this.edmProvider = new EdmxProvider().parse(IOUtils.toInputStream(this.edmx, StandardCharsets.UTF_8), true);

		this.dataStore = new MockDataStore(edmProvider);
		this.dataSource = createDataSource();

		this.serviceFactory = new MockServiceFactory(edmProvider, dataSource);
		this.servlet = new MockServlet(serviceFactory);

		// Load/generate mock data
		if (options.localDataPath() != null) {
			MockDataLoader loader = new MockDataLoader(edm, edmProvider, options.localDataPath(), dataStore);
			loader.load(options.generateMissing());
		}

		start();
	}

	/**
	 * Start the Jetty HTTP server and registers the OData servlet
	 * @throws Exception If Jetty fails to start
	 */
	public void start() throws Exception {
		if (server != null && server.isRunning())
			return; // Already started

		server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(options.portNumber());
        server.setConnectors(new Connector[] {connector});

    	ServletHolder servletHolder = new ServletHolder(servlet);

        ServletContextHandler newHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        newHandler.setContextPath("/");

    	String servletPath = StringUtils.removeEnd(options.rootPath(), "/");
    	newHandler.addServlet(servletHolder, servletPath + "/*");
		server.setHandler(newHandler);

    	server.start();

    	URI rootUri = server.getURI();
    	this.uri = new URI(rootUri.getScheme(), rootUri.getUserInfo(), rootUri.getHost(),
    			rootUri.getPort(), servletPath, null, null);
	}

	/**
	 * Stop the Jetty HTTP server
	 * @throws Exception If Jetty fails to stop
	 */
	public void stop() throws Exception {
		if (server != null && server.isRunning())
			server.stop();
	}

	/**
	 * Registers a Function Import handler for a specific function import.
	 * @param functionName The Function Import name
	 * @param handler A handler function to respond to the requests
	 * @return This same instance for fluent calls
	 */
	public ODataMockServer onFunctionImport(String functionName, FunctionImportHandler handler) {
		if (dataSource instanceof MockDataSource) {
			MockDataSource mds = (MockDataSource) dataSource;
			mds.getFunctionImportHandlers().put(functionName, handler);
		}
		return this;
	}

	/**
	 * Unregisters all currently registered event handlers.
	 * @return This same instance for fluent calls
	 */
	public ODataMockServer clearHandlers() {
		if (dataSource instanceof MockDataSource) {
			MockDataSource mds = (MockDataSource) dataSource;
			mds.getFunctionImportHandlers().clear();
		}
		return this;
	}

	/**
	 * Creates a new DataSource instance for handling the OData requests.
	 * Override this method if you need more advanced customization.
	 * @return New DataSource instance
	 * @throws ODataException If the DataSource initialization fails
	 */
	protected DataSource createDataSource() throws ODataException {
		return new MockDataSource(edmProvider, dataStore);
	}

}
