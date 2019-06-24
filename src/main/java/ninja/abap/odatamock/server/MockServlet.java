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

import javax.servlet.http.HttpServletRequest;
import org.apache.olingo.odata2.api.ODataServiceFactory;
import org.apache.olingo.odata2.core.servlet.ODataServlet;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@SuppressWarnings("serial")
class MockServlet extends ODataServlet {

	@Getter
	protected final ODataServiceFactory serviceFactory;

	@Override
	protected ODataServiceFactory getServiceFactory(HttpServletRequest request) {
		return serviceFactory;
	}

}
