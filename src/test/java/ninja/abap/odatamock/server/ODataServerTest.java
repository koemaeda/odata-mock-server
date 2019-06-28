package ninja.abap.odatamock.server;

import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.olingo.odata2.api.client.batch.BatchPart;
import org.apache.olingo.odata2.api.client.batch.BatchQueryPart;
import org.apache.olingo.odata2.api.client.batch.BatchSingleResponse;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.*;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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

	@Test
	public void testBatchRequest() throws Exception {
		ODataMockServer server = new ODataMockServerBuilder()
			.edmxFromFile("src/test/resources/Northwind.svc.edmx")
			.localDataPath("src/test/resources/mockdata")
			.build();
		assertThat("Server URI is not null", server.getUri(), notNullValue());

		Map<String, String> reqHeaders = new HashMap<>();
		reqHeaders.put("Accept", "application/json; charset=utf-8");
		List<BatchPart> batchParts = new ArrayList<>();
		batchParts.add(BatchQueryPart.method("GET").uri("/Orders").headers(reqHeaders).build());
		batchParts.add(BatchQueryPart.method("GET").uri("/HeyIDontExist").headers(reqHeaders).build());
		batchParts.add(BatchQueryPart.method("GET").uri("/Invoices").headers(reqHeaders).build());

		InputStream request = EntityProvider.writeBatchRequest(batchParts, "dummy_boundary");
		Content response = Request.Post(server.getUri() + "$batch")
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
		ODataMockServer server = new ODataMockServerBuilder()
			.edmxFromFile("src/test/resources/OData.svc.edmx")
			.build();
		assertThat("Server URI is not null", server.getUri(), notNullValue());

		server.onFunctionImport("GetProductsByRating", (function, parameters, keys) -> {
			Map<String, Object> entry = new HashMap<>();
			entry.put("ID", 1);
			entry.put("Name", "Dummy Product");
			entry.put("ReleaseDate", Calendar.getInstance());
			entry.put("Rating", 5);
			entry.put("Price", new BigDecimal(100));
			return Arrays.asList(entry);
		});

		String json = Request.Get(server.getUri() + "GetProductsByRating?rating=123")
				.addHeader("Accept", "application/json; charset=utf-8")
				.execute().returnContent().asString();
		assertThat("Dummy data was served", json, containsString("\"Name\":\"Dummy Product\""));
	}

}
