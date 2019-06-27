package ninja.abap.odatamock.server;

import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.provider.EdmProvider;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ninja.abap.odatamock.server.ODataMockServer;
import ninja.abap.odatamock.server.ODataMockServerBuilder;


public class MockDataStoreTest {

	static Edm edm;
	static EdmProvider edmProvider;

	MockDataStore dataStore;

	@BeforeClass
	public static void beforeClass() throws Exception {
		ODataMockServer server = new ODataMockServerBuilder()
	    		.edmxFromFile("src/test/resources/Northwind.svc.edmx")
	    		.build();

		edm = server.getServiceFactory().getEdm();
		edmProvider = server.getServiceFactory().getEdmProvider();
	}
	
	@Before
	public void before() throws Exception {
		dataStore = new MockDataStore(edmProvider);
	}

	@Test
	public void testGetEmptyEntitySet() throws Exception {
		assertThat("Entity Set is empty", dataStore.getEntitySet("Customers").isEmpty(), is(true));
	}

	@Test(expected = ODataException.class)
	public void testGetUnexistingEntitySet() throws Exception {
		dataStore.getEntitySet("WhatIsLove?");
	}

	@Test
	public void testPutRecord() throws Exception {
		assertThat("Entity Set is empty", dataStore.getEntitySet("Customers").isEmpty(), is(true));

		Map<String, Object> fields = new HashMap<>();
		fields.put("CustomerID", "ANTON");
		fields.put("CompanyName", "Antonio Moreno Taquería");
		fields.put("ContactName", "Antonio Moreno");
		fields.put("ContactTitle", "Owner");
		fields.put("Address", "Mataderos  2312");
		fields.put("City", "México D.F.");
		fields.put("PostalCode", "05023");
		fields.put("Country", "Mexico");

		dataStore.put("Customers", fields);
		assertThat("Entity Set has 1 record", dataStore.getEntitySet("Customers").size(), is(1));
	}

	@Test(expected = ODataException.class)
	public void testPutDuplicateRecord() throws Exception {
		assertThat("Entity Set is empty", dataStore.getEntitySet("Customers").isEmpty(), is(true));

		Map<String, Object> fields = new HashMap<>();
		fields.put("CustomerID", "ANTON");
		fields.put("CompanyName", "Antonio Moreno Taquería");
		fields.put("ContactName", "Antonio Moreno");
		fields.put("ContactTitle", "Owner");
		fields.put("Address", "Mataderos  2312");
		fields.put("City", "México D.F.");
		fields.put("PostalCode", "05023");
		fields.put("Country", "Mexico");
		dataStore.put("Customers", fields);
		dataStore.put("Customers", fields);
	}

	@Test
	public void testPutMultipleRecords() throws Exception {
		assertThat("Entity Set is empty", dataStore.getEntitySet("Customers").isEmpty(), is(true));

		List<Map<String, Object>> records = new ArrayList<>();
		Map<String, Object> fields = new HashMap<>();
		fields.put("CustomerID", "ANTON");
		fields.put("CompanyName", "Antonio Moreno Taquería");
		fields.put("ContactName", "Antonio Moreno");
		fields.put("ContactTitle", "Owner");
		fields.put("Address", "Mataderos  2312");
		fields.put("City", "México D.F.");
		fields.put("PostalCode", "05023");
		fields.put("Country", "Mexico");
		records.add(fields);

		fields = new HashMap<>();
		fields.put("CustomerID", "CHOPS");
		fields.put("CompanyName", "Chop-suey Chinese");
		fields.put("ContactName", "Yang Wang");
		fields.put("ContactTitle", "Owner");
		fields.put("Address", "Hauptstr. 29");
		fields.put("City", "Bern");
		fields.put("PostalCode", "3012");
		fields.put("Country", "Switzerland");
		records.add(fields);

		dataStore.putAll("Customers", records);
		assertThat("Entity Set has 2 records", dataStore.getEntitySet("Customers").size(), is(2));
	}

	@Test
	public void testRemoveRecord() throws Exception {
		List<Map<String, Object>> records = new ArrayList<>();
		Map<String, Object> fields = new HashMap<>();
		fields.put("CustomerID", "ANTON");
		fields.put("CompanyName", "Antonio Moreno Taquería");
		records.add(fields);

		fields = new HashMap<>();
		fields.put("CustomerID", "CHOPS");
		fields.put("CompanyName", "Chop-suey Chinese");
		records.add(fields);

		dataStore.putAll("Customers", records);

		fields = new HashMap<>();
		fields.put("CustomerID", "ANTON");
		dataStore.remove("Customers", fields);

		assertThat("Entity Set has 1 record", dataStore.getEntitySet("Customers").size(), is(1));
		assertThat("Deleted record is gone", dataStore.getRecordByKey("Customers", fields), nullValue());
	}

	@Test
	public void testTruncate() throws Exception {
		List<Map<String, Object>> records = new ArrayList<>();
		Map<String, Object> fields = new HashMap<>();
		fields.put("CustomerID", "ANTON");
		fields.put("CompanyName", "Antonio Moreno Taquería");
		records.add(fields);

		fields = new HashMap<>();
		fields.put("CustomerID", "CHOPS");
		fields.put("CompanyName", "Chop-suey Chinese");
		records.add(fields);

		dataStore.putAll("Customers", records);

		dataStore.truncate("Customers");
		assertThat("Entity Set is empty", dataStore.getEntitySet("Customers").isEmpty(), is(true));
	}

	@Test
	public void testClear() throws Exception {
		List<Map<String, Object>> records = new ArrayList<>();
		Map<String, Object> fields = new HashMap<>();
		fields.put("CustomerID", "ANTON");
		fields.put("CompanyName", "Antonio Moreno Taquería");
		records.add(fields);

		fields = new HashMap<>();
		fields.put("CustomerID", "CHOPS");
		fields.put("CompanyName", "Chop-suey Chinese");
		records.add(fields);

		dataStore.putAll("Customers", records);

		dataStore.clear();
		assertThat("Entity Set is empty", dataStore.getEntitySet("Customers").isEmpty(), is(true));
	}

}
