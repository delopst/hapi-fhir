package ca.uhn.fhir.jpa.provider.r4;

import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.parser.StrictErrorHandler;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.util.TestUtil;
import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@SuppressWarnings("Duplicates")
public class PatientSearchR4Test extends BaseResourceProviderR4Test {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(PatientSearchR4Test.class);
	private String orgId;
	private String patId;
	private String encId1;
	private String encId2;
	private ArrayList<String> myObsIds;
	private String myWrongPatId;
	private String myWrongEnc1;

	@Before
	public void beforeDisableResultReuse() {
		myDaoConfig.setReuseCachedSearchResultsForMillis(null);
	}

	@Override
	@After
	public void after() throws Exception {
		super.after();

		myDaoConfig.setReuseCachedSearchResultsForMillis(new DaoConfig().getReuseCachedSearchResultsForMillis());
		myDaoConfig.setEverythingIncludesFetchPageSize(new DaoConfig().getEverythingIncludesFetchPageSize());
		myDaoConfig.setSearchPreFetchThresholds(new DaoConfig().getSearchPreFetchThresholds());
	}

	@Override
	public void before() throws Exception {
		super.before();
		myFhirCtx.setParserErrorHandler(new StrictErrorHandler());

		myDaoConfig.setAllowMultipleDelete(true);
	}

	/**
	 * See #674
	 */
	@Test
	public void testSearchPatientNameText() throws Exception {

		Patient patient = new Patient();
		String nameTextString = "Test123";
		patient.getNameFirstRep().setText(nameTextString);
		String patId = ourClient.create().resource(patient).execute().getId().toUnqualifiedVersionless().getValue();

		Bundle bundle = fetchBundle(ourServerBase + "/Patient?name=" + nameTextString, EncodingEnum.JSON);
		
		assertNull(bundle.getLink("next"));
		
		Set<String> actual = new TreeSet<String>();
		for (BundleEntryComponent nextEntry : bundle.getEntry()) {
			actual.add(nextEntry.getResource().getIdElement().toUnqualifiedVersionless().getValue());
		}
		
		ourLog.info("Found IDs: {}", actual);
		
		assertThat(actual, hasItem(patId));
		assertThat(actual, hasItem(encId1));
		assertThat(actual, hasItem(encId2));
		assertThat(actual, hasItem(orgId));
		assertThat(actual, hasItems(myObsIds.toArray(new String[0])));
		assertThat(actual, not(hasItem(myWrongPatId)));
		assertThat(actual, not(hasItem(myWrongEnc1)));
	}

	private Bundle fetchBundle(String theUrl, EncodingEnum theEncoding) throws IOException, ClientProtocolException {
		Bundle bundle;
		HttpGet get = new HttpGet(theUrl);
//		CloseableHttpResponse resp = ourHttpClient.execute(get);
		try (CloseableHttpResponse resp = ourHttpClient.execute(get)){
			assertEquals(theEncoding.getResourceContentTypeNonLegacy(), resp.getFirstHeader(ca.uhn.fhir.rest.api.Constants.HEADER_CONTENT_TYPE).getValue().replaceAll(";.*", ""));
			bundle = theEncoding.newParser(myFhirCtx).parseResource(Bundle.class, IOUtils.toString(resp.getEntity().getContent(), Charsets.UTF_8));
		}

		return bundle;
	}

	@AfterClass
	public static void afterClassClearContext() {
		TestUtil.clearAllStaticFieldsForUnitTest();
	}

}
