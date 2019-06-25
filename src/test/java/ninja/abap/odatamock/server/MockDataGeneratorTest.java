package ninja.abap.odatamock.server;

import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.provider.EdmProvider;
import org.apache.olingo.odata2.core.edm.EdmBinary;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.*;

import java.math.BigDecimal;
import java.util.GregorianCalendar;
import java.util.UUID;

import org.apache.olingo.odata2.core.edm.*;


public class MockDataGeneratorTest {

	static Edm edm;
	static EdmProvider edmProvider;

	MockDataGenerator generator;

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
		generator = new MockDataGenerator(edm, new MockDataStore(edmProvider));
	}

	@Test
	public void testEdmBinary() throws Exception {
		Object value = generator.generateValue("Test", EdmBinary.getInstance(), 1);
		assertThat("Generated value is byte[]", value.getClass(), is(byte[].class));
	}

	@Test
	public void testEdmBoolean() throws Exception {
		Object value = generator.generateValue("Test", EdmBoolean.getInstance(), 1);
		assertThat("Generated value is Boolean", value.getClass(), is(Boolean.class));
	}

	@Test
	public void testEdmByte() throws Exception {
		Object value = generator.generateValue("Test", EdmByte.getInstance(), 1);
		assertThat("Generated value is Short", value.getClass(), is(Short.class));
	}

	@Test
	public void testEdmDateTime() throws Exception {
		Object value = generator.generateValue("Test", EdmDateTime.getInstance(), 1);
		assertThat("Generated value is GregorianCalendar", value.getClass(), is(GregorianCalendar.class));
	}

	@Test
	public void testEdmDateTimeOffset() throws Exception {
		Object value = generator.generateValue("Test", EdmDateTimeOffset.getInstance(), 1);
		assertThat("Generated value is GregorianCalendar", value.getClass(), is(GregorianCalendar.class));
	}

	@Test
	public void testEdmDecimal() throws Exception {
		Object value = generator.generateValue("Test", EdmDecimal.getInstance(), 1);
		assertThat("Generated value is BigDecimal", value.getClass(), is(BigDecimal.class));
	}

	@Test
	public void testEdmDouble() throws Exception {
		Object value = generator.generateValue("Test", EdmDouble.getInstance(), 1);
		assertThat("Generated value is Double", value.getClass(), is(Double.class));
	}

	@Test
	public void testEdmGuid() throws Exception {
		Object value = generator.generateValue("Test", EdmGuid.getInstance(), 1);
		assertThat("Generated value is UUID", value.getClass(), is(UUID.class));
	}

	@Test
	public void testEdmInt16() throws Exception {
		Object value = generator.generateValue("Test", EdmInt16.getInstance(), 1);
		assertThat("Generated value is Short", value.getClass(), is(Short.class));
	}

	@Test
	public void testEdmInt32() throws Exception {
		Object value = generator.generateValue("Test", EdmInt32.getInstance(), 1);
		assertThat("Generated value is Integer", value.getClass(), is(Integer.class));
	}

	@Test
	public void testEdmInt64() throws Exception {
		Object value = generator.generateValue("Test", EdmInt64.getInstance(), 1);
		assertThat("Generated value is Long", value.getClass(), is(Long.class));
	}

	@Test(expected = EdmException.class)
	public void testEdmNull() throws Exception {
		generator.generateValue("Test", EdmNull.getInstance(), 1);
	}

	@Test
	public void testEdmSByte() throws Exception {
		Object value = generator.generateValue("Test", EdmSByte.getInstance(), 1);
		assertThat("Generated value is Byte", value.getClass(), is(Byte.class));
	}

	@Test
	public void testEdmSingle() throws Exception {
		Object value = generator.generateValue("Test", EdmSingle.getInstance(), 1);
		assertThat("Generated value is Float", value.getClass(), is(Float.class));
	}

	@Test
	public void testEdmString() throws Exception {
		Object value = generator.generateValue("Test", EdmString.getInstance(), 1);
		assertThat("Generated value is String", value.getClass(), is(String.class));
		assertThat("Generated value is '1'", value.toString(), is("1"));
	}

	@Test
	public void testEdmTime() throws Exception {
		Object value = generator.generateValue("Test", EdmTime.getInstance(), 1);
		assertThat("Generated value is GregorianCalendar", value.getClass(), is(GregorianCalendar.class));
	}

}