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
import org.apache.commons.lang3.StringUtils;
import org.apache.olingo.odata2.api.exception.ODataException;
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

	protected final ODataMockServerBuilder options;
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
		this.serviceFactory = new MockServiceFactory(options.edmx());
		this.servlet = new MockServlet(serviceFactory);

		this.dataStore = serviceFactory.getDataSource().getDataStore();

		// Load/generate mock data
		if (options.localDataPath() != null) {
			MockDataLoader loader = new MockDataLoader(serviceFactory.getEdm(), serviceFactory.getEdmProvider(),
					options.localDataPath(), serviceFactory.getDataSource().getDataStore());
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
    	this.uri = new URI(StringUtils.appendIfMissing(server.getURI().toString() + servletPath, "/"));
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
		serviceFactory.getDataSource().getFunctionImportHandlers().put(functionName, handler);
		return this;
	}

	/**
	 * Unregisters all currently registered event handlers.
	 * @return This same instance for fluent calls
	 */
	public ODataMockServer clearHandlers() {
		serviceFactory.getDataSource().getFunctionImportHandlers().clear();
		return this;
	}

}
