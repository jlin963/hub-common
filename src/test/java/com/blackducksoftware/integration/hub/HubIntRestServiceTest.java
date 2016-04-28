/*******************************************************************************
 * Black Duck Software Suite SDK
 * Copyright (C) 2016 Black Duck Software, Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *******************************************************************************/
package com.blackducksoftware.integration.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restlet.Response;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;

import com.blackducksoftware.integration.hub.api.VersionComparison;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.exception.ProjectDoesNotExistException;
import com.blackducksoftware.integration.hub.exception.VersionDoesNotExistException;
import com.blackducksoftware.integration.hub.meta.MetaLink;
import com.blackducksoftware.integration.hub.policy.api.PolicyStatus;
import com.blackducksoftware.integration.hub.policy.api.PolicyStatusEnum;
import com.blackducksoftware.integration.hub.project.api.ProjectItem;
import com.blackducksoftware.integration.hub.report.api.ReportFormatEnum;
import com.blackducksoftware.integration.hub.report.api.ReportInformationItem;
import com.blackducksoftware.integration.hub.report.api.VersionReport;
import com.blackducksoftware.integration.hub.scan.api.ScanLocationItem;
import com.blackducksoftware.integration.hub.scan.api.ScanLocationResults;
import com.blackducksoftware.integration.hub.util.HubIntTestHelper;
import com.blackducksoftware.integration.hub.util.TestLogger;
import com.blackducksoftware.integration.hub.version.api.DistributionEnum;
import com.blackducksoftware.integration.hub.version.api.PhaseEnum;
import com.blackducksoftware.integration.hub.version.api.ReleaseItem;
import com.google.gson.Gson;

public class HubIntRestServiceTest {

	private static Properties testProperties;

	private static HubIntTestHelper helper;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@BeforeClass
	public static void testInit() throws Exception {
		testProperties = new Properties();
		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		final InputStream is = classLoader.getResourceAsStream("test.properties");
		try {
			testProperties.load(is);
		} catch (final IOException e) {
			System.err.println("reading test.properties failed!");
		}
		// p.load(new FileReader(new File("test.properties")));
		System.out.println(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		System.out.println(testProperties.getProperty("TEST_USERNAME"));
		System.out.println(testProperties.getProperty("TEST_PASSWORD"));

		helper = new HubIntTestHelper(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		helper.setLogger(new TestLogger());
		helper.setCookies(testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));

		try {
			final ProjectItem project = helper.getProjectByName(testProperties.getProperty("TEST_PROJECT"));

			try {
				helper.getVersion(project, testProperties.getProperty("TEST_VERSION"));
			} catch (final VersionDoesNotExistException e) {
				helper.createHubVersion(project, testProperties.getProperty("TEST_VERSION"),
						testProperties.getProperty("TEST_PHASE"), testProperties.getProperty("TEST_DISTRIBUTION"));
			}
		} catch (final ProjectDoesNotExistException e) {
			final String projectUrl = helper.createHubProject(testProperties.getProperty("TEST_PROJECT"));
			final ProjectItem project = helper.getProject(projectUrl);
			helper.createHubVersion(project, testProperties.getProperty("TEST_VERSION"),
					testProperties.getProperty("TEST_PHASE"), testProperties.getProperty("TEST_DISTRIBUTION"));
		}
	}

	@AfterClass
	public static void testTeardown() {
		try {
			final ProjectItem project = helper.getProjectByName(testProperties.getProperty("TEST_PROJECT"));
			helper.deleteHubProject(project);
		} catch (final Exception e) {

		}
	}

	@Test
	public void testSetCookies() throws Exception {
		final TestLogger logger = new TestLogger();

		final HubIntRestService restService = new HubIntRestService(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		restService.setLogger(logger);
		restService.setCookies(testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));

		assertNotNull(restService.getCookies());
		assertTrue(!restService.getCookies().isEmpty());
		assertTrue(logger.getErrorList().isEmpty());
	}

	@Test
	public void testSetTimeoutZero() throws Exception {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Can not set the timeout to zero.");
		final HubIntRestService restService = new HubIntRestService(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		restService.setTimeout(0);
	}

	@Test
	public void testSetTimeout() throws Exception {
		final TestLogger logger = new TestLogger();
		final HubIntRestService restService = new HubIntRestService(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		restService.setTimeout(120);
		restService.setLogger(logger);
		restService.setCookies(testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));

		assertNotNull(restService.getCookies());
		assertTrue(!restService.getCookies().isEmpty());
		assertTrue(logger.getErrorList().isEmpty());
	}

	@Test
	public void testGetProjectMatches() throws Exception {
		final TestLogger logger = new TestLogger();

		final HubIntRestService restService = new HubIntRestService(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		restService.setLogger(logger);
		restService.setCookies(testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		final String testProjectName = "TESTNAME";
		String projectUrl = null;
		try {

			projectUrl = restService.createHubProject(testProjectName);

			// Sleep for 3 second, server takes a second before you can start using projects
			Thread.sleep(3000);

			final List<ProjectItem> matches = restService.getProjectMatches(testProjectName);

			assertNotNull("matches must be not null", matches);
			assertTrue(!matches.isEmpty());
			assertTrue("error log expected to be empty", logger.getErrorList().isEmpty());
		} finally {
			if (StringUtils.isNotBlank(projectUrl)) {
				helper.deleteHubProject(projectUrl);
			}
		}
	}

	@Test
	public void testGetProjectByName() throws Exception {
		final TestLogger logger = new TestLogger();

		final HubIntRestService restService = new HubIntRestService(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		restService.setLogger(logger);
		restService.setCookies(testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));

		final ProjectItem project = restService.getProjectByName(testProperties.getProperty("TEST_PROJECT"));

		assertNotNull(project);
		assertEquals(testProperties.getProperty("TEST_PROJECT"), project.getName());
		assertTrue(logger.getErrorList().isEmpty());
	}

	@Test
	public void testGetProjectByNameSpecialCharacters() throws Exception {
		final TestLogger logger = new TestLogger();

		final String projectName = "CItest!@#$^&";

		final HubIntRestService restService = new HubIntRestService(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		restService.setLogger(logger);
		restService.setCookies(testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		String projectURL = null;
		try {

			projectURL = restService.createHubProject(projectName);

			assertTrue(StringUtils.isNotBlank(projectURL));

			final ProjectItem project = restService.getProjectByName(projectName);

			assertNotNull(project);
			assertEquals(projectName, project.getName());
			assertTrue(logger.getErrorList().isEmpty());

		} finally {
			helper.deleteHubProject(projectURL);
		}

		assertTrue(logger.getErrorList().isEmpty());
	}


	@Test
	public void testGetVersionForProject() throws Exception {
		final TestLogger logger = new TestLogger();

		final HubIntRestService restService = new HubIntRestService(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		restService.setLogger(logger);
		restService.setCookies(testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));

		final ProjectItem project = restService.getProjectByName(testProperties.getProperty("TEST_PROJECT"));

		assertNotNull(project);
		assertEquals(testProperties.getProperty("TEST_PROJECT"), project.getName());
		assertTrue(logger.getErrorList().isEmpty());

		final ReleaseItem release = restService.getVersion(project, testProperties.getProperty("TEST_VERSION"));

		assertNotNull(release);

		assertTrue(logger.getErrorList().isEmpty());
	}

	@Test
	public void testGetVersionsForProject() throws Exception {
		final TestLogger logger = new TestLogger();

		final HubIntRestService restService = new HubIntRestService(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		restService.setLogger(logger);
		restService.setCookies(testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));

		final ProjectItem project = restService.getProjectByName(testProperties.getProperty("TEST_PROJECT"));

		assertNotNull(project);
		assertEquals(testProperties.getProperty("TEST_PROJECT"), project.getName());
		assertTrue(logger.getErrorList().isEmpty());

		final List<ReleaseItem> releaseList = restService.getVersionsForProject(project);

		assertNotNull(releaseList);
		assertTrue(releaseList.size() > 0);

		assertTrue(logger.getErrorList().isEmpty());
	}


	@Test
	public void testCreateHubProject() throws Exception {
		final TestLogger logger = new TestLogger();

		final HubIntRestService restService = new HubIntRestService(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		restService.setLogger(logger);
		restService.setCookies(testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		// TEST_CREATE_PROJECT
		String projectURL = null;
		try {

			projectURL = restService.createHubProject(testProperties.getProperty("TEST_CREATE_PROJECT"));

			assertTrue(StringUtils.isNotBlank(projectURL));
		} finally {
			helper.deleteHubProject(projectURL);
		}

		assertTrue(logger.getErrorList().isEmpty());
	}

	@Test
	public void testCreateHubProjectSpecialCharacters() throws Exception {
		final TestLogger logger = new TestLogger();

		final String projectName = "CItest!@#$^&";

		final HubIntRestService restService = new HubIntRestService(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		restService.setLogger(logger);
		restService.setCookies(testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		String projectURL = null;
		try {

			projectURL = restService.createHubProject(projectName);

			assertTrue(StringUtils.isNotBlank(projectURL));
		} finally {
			helper.deleteHubProject(projectURL);
		}

		assertTrue(logger.getErrorList().isEmpty());
	}


	@Test
	public void testCreateHubVersion() throws Exception {
		final TestLogger logger = new TestLogger();

		final HubIntRestService restService = new HubIntRestService(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		restService.setLogger(logger);
		restService.setCookies(testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		// TEST_CREATE_PROJECT
		String projectURL = null;
		try {

			projectURL = restService.createHubProject(testProperties.getProperty("TEST_CREATE_PROJECT"));
			assertTrue(StringUtils.isNotBlank(projectURL));

			final ProjectItem project = restService.getProject(projectURL);

			final String versionURL = restService.createHubVersion(project,
					testProperties.getProperty("TEST_CREATE_VERSION"), PhaseEnum.DEVELOPMENT.name(),
					DistributionEnum.INTERNAL.name());

			assertTrue(StringUtils.isNotBlank(versionURL));
		} finally {
			helper.deleteHubProject(projectURL);
		}

		assertTrue(logger.getErrorList().isEmpty());
	}

	@Test
	public void testGetHubVersion() throws Exception {
		final TestLogger logger = new TestLogger();

		final HubIntRestService restService = new HubIntRestService(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		restService.setLogger(logger);
		restService.setCookies(testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));

		final String version = restService.getHubVersion();

		assertTrue(StringUtils.isNotBlank(version));
		assertTrue(logger.getErrorList().isEmpty());
	}

	@Test
	public void testCompareWithHubVersion() throws Exception {
		final TestLogger logger = new TestLogger();

		final HubIntRestService restService = new HubIntRestService(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		restService.setLogger(logger);
		restService.setCookies(testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));

		VersionComparison comparison = restService.compareWithHubVersion("1");

		assertNotNull(comparison);
		assertEquals("1", comparison.getConsumerVersion());
		assertEquals(Integer.valueOf(-1), comparison.getNumericResult());
		assertEquals("<", comparison.getOperatorResult());

		comparison = restService.compareWithHubVersion("9999999");

		assertNotNull(comparison);
		assertEquals("9999999", comparison.getConsumerVersion());
		assertEquals(Integer.valueOf(1), comparison.getNumericResult());
		assertEquals(">", comparison.getOperatorResult());

		assertTrue(logger.getErrorList().isEmpty());
	}

	@Test
	public void testGenerateHubReport() throws Exception {
		final TestLogger logger = new TestLogger();

		final HubIntRestService restService = new HubIntRestService(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		restService.setLogger(logger);
		restService.setCookies(testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));

		final ProjectItem project = restService.getProjectByName(testProperties.getProperty("TEST_PROJECT"));
		final ReleaseItem release = restService.getVersion(project, testProperties.getProperty("TEST_VERSION"));

		assertNotNull(
				"In project : " + testProperties.getProperty("TEST_PROJECT") + " , could not find the version : " + testProperties.getProperty("TEST_VERSION"),
				release);
		String reportUrl = null;
		reportUrl = restService.generateHubReport(release,
				ReportFormatEnum.JSON);

		assertNotNull(reportUrl, reportUrl);
		// The project specified in the test properties file will be deleted at the end of the tests
		// So we dont need to worry about cleaning up the reports
	}

	@Test
	public void testGenerateHubReportFormatUNKNOWN() throws Exception {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Can not generate a report of format : ");
		final HubIntRestService restService = new HubIntRestService(testProperties.getProperty("TEST_HUB_SERVER_URL"));

		restService.generateHubReport(null, ReportFormatEnum.UNKNOWN);
	}

	@Test
	public void testGenerateHubReportAndReadReport() throws Exception {
		final TestLogger logger = new TestLogger();

		final HubIntRestService restService = new HubIntRestService(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		restService.setLogger(logger);
		restService.setCookies(testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));

		final ProjectItem project = restService.getProjectByName(testProperties.getProperty("TEST_PROJECT"));

		final String versionId = restService.createHubVersion(project, "Report Version", PhaseEnum.DEVELOPMENT.name(),
				DistributionEnum.INTERNAL.name());

		final ReleaseItem release = restService.getVersion(project, "Report Version");

		String reportUrl = null;
		System.err.println(versionId);

		// Give the server a second to recognize the new version
		Thread.sleep(1000);

		reportUrl = restService.generateHubReport(release, ReportFormatEnum.JSON);

		assertNotNull(reportUrl, reportUrl);

		DateTime timeFinished = null;
		ReportInformationItem reportInfo = null;

		while (timeFinished == null) {
			Thread.sleep(5000);
			reportInfo = restService.getReportInformation(reportUrl);

			timeFinished = reportInfo.getTimeFinishedAt();
		}

		final List<MetaLink> links = reportInfo.get_meta().getLinks();

		MetaLink contentLink = null;
		for (final MetaLink link : links) {
			if (link.getRel().equalsIgnoreCase("content")) {
				contentLink = link;
				break;
			}
		}
		assertNotNull("Could not find the content link for the report at : " + reportUrl, contentLink);
		// The project specified in the test properties file will be deleted at the end of the tests
		// So we dont need to worry about cleaning up the reports

		final VersionReport report = restService.getReportContent(contentLink.getHref());
		assertNotNull(report);
		assertNotNull(report.getDetailedReleaseSummary());
		assertNotNull(report.getDetailedReleaseSummary().getPhase());
		assertNotNull(report.getDetailedReleaseSummary().getDistribution());
		assertNotNull(report.getDetailedReleaseSummary().getProjectId());
		assertNotNull(report.getDetailedReleaseSummary().getProjectName());
		assertNotNull(report.getDetailedReleaseSummary().getVersionId());
		assertNotNull(report.getDetailedReleaseSummary().getVersion());

		assertEquals(204, restService.deleteHubReport(reportUrl));

	}

	@Test
	public void testGetCodeLocations() throws Exception {
		final TestLogger logger = new TestLogger();

		final HubIntRestService restService = new HubIntRestService("FakeUrl");
		restService.setLogger(logger);

		final String fakeHost = "TestHost";
		final String serverPath1 = "/Test/Fake/Path";
		final String serverPath2 = "/Test/Fake/Path/Child/";
		final String serverPath3 = "/Test/Fake/File";

		final HubIntRestService restServiceSpy = Mockito.spy(restService);

		final ClientResource clientResource = new ClientResource("");
		final ClientResource resourceSpy = Mockito.spy(clientResource);

		Mockito.when(resourceSpy.handle()).then(new Answer<Representation>() {
			@Override
			public Representation answer(final InvocationOnMock invocation) throws Throwable {

				final ScanLocationResults scanLocationResults = new ScanLocationResults();
				scanLocationResults.setTotalCount(3);
				final ScanLocationItem sl1 = new ScanLocationItem();
				sl1.setHost(fakeHost);
				sl1.setPath(serverPath1);
				final ScanLocationItem sl2 = new ScanLocationItem();
				sl2.setHost(fakeHost);
				sl2.setPath(serverPath2);
				final ScanLocationItem sl3 = new ScanLocationItem();
				sl3.setHost(fakeHost);
				sl3.setPath(serverPath3);

				final List<ScanLocationItem> items = new ArrayList<ScanLocationItem>();
				items.add(sl1);
				items.add(sl2);
				items.add(sl3);

				scanLocationResults.setItems(items);

				final String scResults = new Gson().toJson(scanLocationResults);
				final StringRepresentation rep = new StringRepresentation(scResults);
				final Response response = new Response(null);
				response.setEntity(rep);

				resourceSpy.setResponse(response);
				return null;
			}
		});

		Mockito.when(restServiceSpy.createClientResource()).thenReturn(resourceSpy);

		final List<String> scanTargets = new ArrayList<String>();
		scanTargets.add("Test/Fake/Path/Child");
		scanTargets.add("Test\\Fake\\File");

		final List<ScanLocationItem> codeLocations = restServiceSpy.getScanLocations(fakeHost, scanTargets);

		assertNotNull(codeLocations);
		assertTrue(codeLocations.size() == 2);
		assertNotNull(codeLocations.get(0));
		assertNotNull(codeLocations.get(1));
	}

	@Test
	public void testGetCodeLocationsUnmatched() throws Exception {
		exception.expect(HubIntegrationException.class);
		exception.expectMessage("Could not determine the code location");

		final TestLogger logger = new TestLogger();

		final HubIntRestService restService = new HubIntRestService("FakeUrl");
		restService.setLogger(logger);

		final String fakeHost = "TestHost";

		final HubIntRestService restServiceSpy = Mockito.spy(restService);

		final ClientResource clientResource = new ClientResource("");
		final ClientResource resourceSpy = Mockito.spy(clientResource);

		Mockito.when(resourceSpy.handle()).then(new Answer<Representation>() {
			@Override
			public Representation answer(final InvocationOnMock invocation) throws Throwable {

				final ScanLocationResults scanLocationResults = new ScanLocationResults();
				scanLocationResults.setTotalCount(0);

				final List<ScanLocationItem> items = new ArrayList<ScanLocationItem>();

				scanLocationResults.setItems(items);

				final String scResults = new Gson().toJson(scanLocationResults);
				final StringRepresentation rep = new StringRepresentation(scResults);
				final Response response = new Response(null);
				response.setEntity(rep);

				resourceSpy.setResponse(response);
				return null;
			}
		});

		Mockito.when(restServiceSpy.createClientResource()).thenReturn(resourceSpy);

		final List<String> scanTargets = new ArrayList<String>();
		scanTargets.add("Test/Fake/Path/Child");

		restServiceSpy.getScanLocations(fakeHost, scanTargets);
	}

	@Test
	public void testGetPolicyStatus() throws Exception {
		final TestLogger logger = new TestLogger();
		final HubIntRestService restService = new HubIntRestService("FakeUrl");
		restService.setLogger(logger);

		final String overallStatus = PolicyStatusEnum.IN_VIOLATION.name();
		final String updatedAt = new DateTime().toString();

		final PolicyStatus policyStatus = new PolicyStatus(overallStatus, updatedAt, null, null);

		final HubIntRestService restServiceSpy = Mockito.spy(restService);

		final ClientResource clientResource = new ClientResource("");
		final ClientResource resourceSpy = Mockito.spy(clientResource);

		final String scResults = new Gson().toJson(policyStatus);
		final StringRepresentation rep = new StringRepresentation(scResults);
		final Response response = new Response(null);
		response.setEntity(rep);

		resourceSpy.setResponse(response);

		Mockito.when(resourceSpy.handle()).then(new Answer<Representation>() {
			@Override
			public Representation answer(final InvocationOnMock invocation) throws Throwable {
				final String scResults = new Gson().toJson(policyStatus);
				final StringRepresentation rep = new StringRepresentation(scResults);
				final Response response = new Response(null);
				response.setEntity(rep);

				resourceSpy.setResponse(response);
				return null;
			}
		});

		Mockito.when(restServiceSpy.createClientResource(Mockito.anyString())).thenReturn(resourceSpy);

		assertEquals(policyStatus, restServiceSpy.getPolicyStatus("policyUrl"));

		try {
			restServiceSpy.getPolicyStatus("");
		} catch (final IllegalArgumentException e) {
			assertEquals("Missing the policy status URL.", e.getMessage());
		}
	}

}
