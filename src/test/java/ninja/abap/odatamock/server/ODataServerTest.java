package ninja.abap.odatamock.server;

import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.olingo.odata2.api.client.batch.BatchPart;
import org.apache.olingo.odata2.api.client.batch.BatchQueryPart;
import org.apache.olingo.odata2.api.client.batch.BatchSingleResponse;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ninja.abap.odatamock.server.ODataMockServer;
import ninja.abap.odatamock.server.ODataMockServerBuilder;


public class ODataServerTest {

	private ODataMockServer server;

	@After
	public void after() throws Exception {
		server.stop();
	}

	@Test
	public void testServerBuilderWithoutRootPath() throws Exception {
		server = new ODataMockServerBuilder()
    		.edmxFromFile("src/test/resources/Northwind.svc.edmx")
    		.build();

		assertThat("Server URI is not null", server.getUri(), notNullValue());
		assertThat("Server URI path is ''", server.getUri().getPath(), is(""));

		String resp = Request.Get(server.getUri()).execute().returnContent().asString();
		assertThat("OData service was served", resp, containsString("<atom:title>CustomerDemographics</atom:title>"));
	}

	@Test
	public void testServerBuilderWithRootPath() throws Exception {
		server = new ODataMockServerBuilder()
    		.edmxFromFile("src/test/resources/Northwind.svc.edmx")
    		.rootPath("/my-odata")
    		.build();

		assertThat("Server URI is not null", server.getUri(), notNullValue());
		assertThat("Server URI is valid", server.getUri().toString(), containsString("/my-odata"));
		assertThat("Server URI path is '/my-odata'", server.getUri().getPath(), is("/my-odata"));

		String resp = Request.Get(server.getUri()).execute().returnContent().asString();
		assertThat("OData service was served", resp, containsString("<atom:title>CustomerDemographics</atom:title>"));
	}

	@Test
	public void testEntitySetAutomaticData() throws Exception {
		server = new ODataMockServerBuilder()
    		.edmxFromFile("src/test/resources/Northwind.svc.edmx")
    		.localDataPath("src/test/resources/mockdata")
    		.generateMissing(true)
    		.build();

		String count = Request.Get(server.getUri() + "/Regions/$count")
				.execute().returnContent().asString();
			assertThat("Orders has 50 records", count, is("50"));

		String resp = Request.Get(server.getUri() + "/Regions")
			.addHeader("Accept", "application/json")
			.execute().returnContent().asString();
		assertThat("Automatic data was served", resp, containsString("\"RegionDescription\":"));
	}

	@Test
	public void testEntitySetFromFile() throws Exception {
		server = new ODataMockServerBuilder()
    		.edmxFromFile("src/test/resources/Northwind.svc.edmx")
    		.localDataPath("src/test/resources/mockdata")
    		.build();

		String count = Request.Get(server.getUri() + "/Orders/$count")
				.execute().returnContent().asString();
			assertThat("Orders has 10 records", count, is("10"));

		String json = Request.Get(server.getUri() + "/Orders")
			.addHeader("Accept", "application/json")
			.execute().returnContent().asString();
		assertThat("File data was served", json, containsString("\"ShipName\":\"Vins et alcools Chevalier\""));

		// Get association via $expand
		json = Request.Get(server.getUri() + "/Orders?$expand=Order_Details")
				.addHeader("Accept", "application/json")
				.execute().returnContent().asString();
		assertThat("Association data was served", json, containsString("\"UnitPrice\":\"14.4000\""));
	}

	@Test
	public void testManuallyLoadedEntitySet() throws Exception {
		server = new ODataMockServerBuilder()
			.edmxFromFile("src/test/resources/Northwind.svc.edmx")
			.build();

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

		String json = Request.Get(server.getUri() + "/Customers")
				.addHeader("Accept", "application/json; charset=utf-8")
				.execute().returnContent().asString();
		assertThat("Stored data was served", json, containsString("\"CompanyName\":\"Antonio Moreno Taquería\""));
	}

	@Test
	public void testBatchRequest() throws Exception {
		server = new ODataMockServerBuilder()
			.edmxFromFile("src/test/resources/Northwind.svc.edmx")
			.localDataPath("src/test/resources/mockdata")
			.build();

		Map<String, String> reqHeaders = new HashMap<>();
		reqHeaders.put("Accept", "application/json; charset=utf-8");
		List<BatchPart> batchParts = new ArrayList<>();
		batchParts.add(BatchQueryPart.method("GET").uri("/Orders").headers(reqHeaders).build());
		batchParts.add(BatchQueryPart.method("GET").uri("/HeyIDontExist").headers(reqHeaders).build());
		batchParts.add(BatchQueryPart.method("GET").uri("/Invoices").headers(reqHeaders).build());

		InputStream request = EntityProvider.writeBatchRequest(batchParts, "dummy_boundary");
		Content response = Request.Post(server.getUri() + "/$batch")
			.addHeader("Accept", "application/json; charset=utf-8")
			.addHeader("Content-Type", "multipart/mixed; boundary=dummy_boundary")
			.bodyStream(request)
			.execute().returnContent();
		String contentType = response.getType().toString();

		List<BatchSingleResponse> responses = EntityProvider.parseBatchResponse(response.asStream(), contentType);
		assertThat("3 responses are present", responses.size(), is(3));

		assertThat("1st response contains rows", responses.get(0).getBody(),
				containsString("\"ShipName\":\"Vins et alcools Chevalier\""));
		assertThat("2nd response contains ERROR", responses.get(1).getBody(),
				containsString("error"));
		assertThat("3rd response contains rows", responses.get(2).getBody(),
				containsString("\"CustomerName\":\"Alfreds Futterkiste\""));
	}

	@Test
	public void testFunctionImportHandler() throws Exception {
		server = new ODataMockServerBuilder()
			.edmxFromFile("src/test/resources/OData.svc.edmx")
			.build();

		server.onFunctionImport("GetProductsByRating", (function, parameters, keys) -> {
			Map<String, Object> entry = new HashMap<>();
			entry.put("ID", 1);
			entry.put("Name", "Dummy Product");
			entry.put("ReleaseDate", Calendar.getInstance());
			entry.put("Rating", 5);
			entry.put("Price", new BigDecimal(100));
			return Arrays.asList(entry);
		});

		String json = Request.Get(server.getUri() + "/GetProductsByRating?rating=123")
				.addHeader("Accept", "application/json; charset=utf-8")
				.execute().returnContent().asString();
		assertThat("Dummy data was served", json, containsString("\"Name\":\"Dummy Product\""));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testCreateEntry() throws Exception {
		server = new ODataMockServerBuilder()
			.edmxFromFile("src/test/resources/Northwind.svc.edmx")
			.build();

		File jsonFile = Paths.get("src/test/resources/mockdata/Customer-create.json").toFile();
		Request.Post(server.getUri() + "/Customers")
				.addHeader("Accept", "application/json; charset=utf-8")
				.bodyFile(jsonFile, ContentType.APPLICATION_JSON.withCharset("utf-8"))
				.execute().returnContent().asString();

		List<Map<String, Object>> data = server.getDataStore().getEntitySet("Customers");
		assertThat("1 record was created", data.size(), is(1));
		Map<String, Object> entry = data.get(0);
		assertThat("CompanyName is 'Antonio Moreno Taquería'",
				entry.get("CompanyName"), is("Antonio Moreno Taquería"));
		List<Map<String, Object>> orders = (List<Map<String, Object>>) entry.get("Orders");
		assertThat("Orders association contain 2 records", orders.size(), is(2));

		// Get association
		String json = Request.Get(server.getUri() + "/Customers('ANTON')/Orders")
				.addHeader("Accept", "application/json; charset=utf-8")
				.execute().returnContent().asString();
		assertThat("Association data was served", json, containsString("\"ShipCity\":\"México D.F.\""));

		// Get association via $expand
		json = Request.Get(server.getUri() + "/Customers('ANTON')?$expand=Orders")
				.addHeader("Accept", "application/json; charset=utf-8")
				.execute().returnContent().asString();
		assertThat("Association data was served", json, containsString("\"ShipCity\":\"México D.F.\""));

	}

	@Test
	public void testUpdateEntry() throws Exception {
		server = new ODataMockServerBuilder()
			.edmxFromFile("src/test/resources/Northwind.svc.edmx")
			.build();

		File jsonFile = Paths.get("src/test/resources/mockdata/Customer-create.json").toFile();
		Request.Post(server.getUri() + "/Customers")
				.addHeader("Accept", "application/json; charset=utf-8")
				.bodyFile(jsonFile, ContentType.APPLICATION_JSON.withCharset("utf-8"))
				.execute();

		Request.Patch(server.getUri() + "/Customers('ANTON')")
				.addHeader("Accept", "application/json; charset=utf-8")
				.bodyString("{\"CompanyName\": \"New Name\"}", ContentType.APPLICATION_JSON.withCharset("utf-8"))
				.execute();

		Map<String, Object> key = new HashMap<>(1);
		key.put("CustomerID", "ANTON");
		Map<String, Object> entry = server.getDataStore().getRecordByKey("Customers", key);
		assertThat("CompanyName is now 'New Name'", entry.get("CompanyName"), is("New Name"));
	}

}
