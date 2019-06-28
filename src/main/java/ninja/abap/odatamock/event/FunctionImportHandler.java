package ninja.abap.odatamock.event;

import java.util.Map;

import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmFunctionImport;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.exception.ODataNotFoundException;
import org.apache.olingo.odata2.api.exception.ODataNotImplementedException;

/**
 * Event handler function for responding to a Function Import that returns a single entry.
 * Returned data should be:
 *   A Map(String, Object) for single entries;
 *   A List of Map(String, Object) for feeds.
 *
 * @see org.apache.olingo.odata2.annotation.processor.core.datasource.DataSource
 */
@FunctionalInterface
public interface FunctionImportHandler {
	Object handle(EdmFunctionImport function, Map<String, Object> parameters, Map<String, Object> keys)
			throws ODataNotImplementedException, ODataNotFoundException, EdmException, ODataApplicationException;
}