# OData Mock Server

Simple Java library for mocking [OData V2](https://odata.org/documentation/odata-version-2-0/) services.

It aims to provide similar functionality to the [SAPUI5 Mock Server](https://sapui5.hana.ondemand.com/#/topic/3a9728ec31f94ca18a7d543ce419d85d) (used in Web applications development).

The OData service is served by a Jetty HTTP server, making it useful for unit/integration tests as well as running as a standalone service to be consumed by other applications.

## Installation

*TODO - add Maven dependency here*

## Getting started

```java
import ninja.abap.odatamock.server.ODataMockServer;
import ninja.abap.odatamock.server.ODataMockServerBuilder;

public class FooTest {
    @Test
    public void testOData {
        ODataMockServer server = new ODataMockServerBuilder()
            .edmxFromFile("src/test/resources/Northwind.svc.edmx")
            .localDataPath("src/test/resources/mockdata")
            .build();

        server.getUri(); // returns URL to running server

        // tell your application to fire requests at the OData service

        server.stop();
    }
}
```

Check out this library's own JUnit tests for examples on how to operate the server.

## License

Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
