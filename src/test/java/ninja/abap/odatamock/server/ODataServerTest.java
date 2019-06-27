package ninja.abap.odatamock.server;

import org.apache.http.client.fluent.Request;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ninja.abap.odatamock.server.ODataMockServer;
import ninja.abap.odatamock.server.ODataMockServerBuilder;


public class ODataServerTest {

	@Test
	public void testServerBuilderWithoutRootPath() throws Exception {
		ODataMockServer server = new ODataMockServerBuilder()
    		.edmxFromFile("src/test/resources/Northwind.svc.edmx")
    		.build();

		assertThat("Server URI is not null", server.getUri(), notNullValue());

		String resp = Request.Get(server.getUri()).execute().returnContent().asString();
		assertThat("OData service was served", resp, containsString("<atom:title>CustomerDemographics</atom:title>"));
	}

	@Test
	public void testServerBuilderWithRootPath() throws Exception {
		ODataMockServer server = new ODataMockServerBuilder()
    		.edmxFromFile("src/test/resources/Northwind.svc.edmx")
    		.rootPath("/my-odata")
    		.build();

		assertThat("Server URI is not null", server.getUri(), notNullValue());
		assertThat("Server URI is valid", server.getUri().toString(), containsString("/my-odata"));

		String resp = Request.Get(server.getUri()).execute().returnContent().asString();
		assertThat("OData service was served", resp, containsString("<atom:title>CustomerDemographics</atom:title>"));
	}

	@Test
	public void testEntitySetAutomaticData() throws Exception {
		ODataMockServer server = new ODataMockServerBuilder()
    		.edmxFromFile("src/test/resources/Northwind.svc.edmx")
    		.localDataPath("src/test/resources/mockdata")
    		.generateMissing(true)
    		.build();

		String count = Request.Get(server.getUri() + "Regions/$count")
				.execute().returnContent().asString();
			assertThat("Orders has 50 records", count, is("50"));

		String resp = Request.Get(server.getUri() + "Regions")
			.addHeader("Accept", "application/json")
			.execute().returnContent().asString();
		assertThat("Automatic data was served", resp, containsString("\"RegionDescription\":"));
	}

	@Test
	public void testEntitySetFromFile() throws Exception {
		ODataMockServer server = new ODataMockServerBuilder()
    		.edmxFromFile("src/test/resources/Northwind.svc.edmx")
    		.localDataPath("src/test/resources/mockdata")
    		.build();

		String count = Request.Get(server.getUri() + "Orders/$count")
				.execute().returnContent().asString();
			assertThat("Orders has 50 records", count, is("50"));

		String json = Request.Get(server.getUri() + "Orders")
			.addHeader("Accept", "application/json")
			.execute().returnContent().asString();
		assertThat("File data was served", json, containsString("\"ShipName\":\"Vins et alcools Chevalier\""));
	}

	@Test
	public void testManuallyLoadedEntitySet() throws Exception {
		ODataMockServer server = new ODataMockServerBuilder()
			.edmxFromFile("src/test/resources/Northwind.svc.edmx")
			.build();
		assertThat("Server URI is not null", server.getUri(), notNullValue());

		List<Map<String, Object>> records = new ArrayList<>();
		Map<String, Object> fields = new HashMap<>();
		fields.put("CustomerID", "ANTON");
		fields.put("CompanyName", "Antonio Moreno Taquería");
		records.add(fields);

		fields = new HashMap<>();
		fields.put("CustomerID", "CHOPS");
		fields.put("CompanyName", "Chop-suey Chinese");
		records.add(fields);

		server.getDataStore().putAll("Customers", records);

		String json = Request.Get(server.getUri() + "Customers")
				.addHeader("Accept", "application/json; charset=utf-8")
				.execute().returnContent().asString();
			assertThat("Stored data was served", json, containsString("\"CompanyName\":\"Antonio Moreno Taquería\""));
	}

}
