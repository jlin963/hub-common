package com.blackducksoftware.integration.hub.dataservices.transforms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.blackducksoftware.integration.hub.api.ComponentVersionRestService;
import com.blackducksoftware.integration.hub.api.NotificationRestService;
import com.blackducksoftware.integration.hub.api.PolicyRestService;
import com.blackducksoftware.integration.hub.api.ProjectVersionRestService;
import com.blackducksoftware.integration.hub.api.VersionBomPolicyRestService;
import com.blackducksoftware.integration.hub.api.component.BomComponentVersionPolicyStatus;
import com.blackducksoftware.integration.hub.api.component.ComponentVersion;
import com.blackducksoftware.integration.hub.api.component.ComponentVersionStatus;
import com.blackducksoftware.integration.hub.api.notification.PolicyOverrideNotificationContent;
import com.blackducksoftware.integration.hub.api.notification.PolicyOverrideNotificationItem;
import com.blackducksoftware.integration.hub.api.policy.PolicyRule;
import com.blackducksoftware.integration.hub.api.version.ReleaseItem;
import com.blackducksoftware.integration.hub.dataservices.items.NotificationContentItem;
import com.blackducksoftware.integration.hub.dataservices.items.PolicyOverrideContentItem;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.NotificationServiceException;

public class PolicyOverrideTransformTest {

	private final static String PROJECT_NAME = "test project";
	private final static String PROJECT_VERSION = "0.1.0";
	private final static String COMPONENT_NAME = "component 1";
	private final static String COMPONENT_VERSION = "0.9.8";
	private final static String POLICY_NAME = "Policy Name";
	private final static String FIRST_NAME = "myName";
	private final static String LAST_NAME = "noMyName";

	private NotificationRestService notificationService;
	private ProjectVersionRestService projectVersionService;
	private PolicyRestService policyService;
	private VersionBomPolicyRestService bomVersionPolicyService;
	private ComponentVersionRestService componentVersionService;
	private PolicyViolationOverrideTransform transformer;
	private List<String> policyNameList;

	private NotificationRestService createNotificationService() {
		final NotificationRestService service = Mockito.mock(NotificationRestService.class);
		return service;
	}

	private ProjectVersionRestService createProjectVersionService()
			throws IOException, BDRestException, URISyntaxException {

		final ProjectVersionRestService service = Mockito.mock(ProjectVersionRestService.class);
		final ReleaseItem releaseItem = Mockito.mock(ReleaseItem.class);
		Mockito.when(releaseItem.getVersionName()).thenReturn(PROJECT_VERSION);
		Mockito.when(service.getProjectVersionReleaseItem(Mockito.anyString())).thenReturn(releaseItem);
		return service;
	}

	private PolicyRestService createPolicyService() throws IOException, BDRestException, URISyntaxException {
		final PolicyRule rule = Mockito.mock(PolicyRule.class);
		Mockito.when(rule.getName()).thenReturn(POLICY_NAME);
		final PolicyRestService service = Mockito.mock(PolicyRestService.class);
		Mockito.when(service.getPolicyRule(Mockito.anyString())).thenReturn(rule);
		return service;
	}

	private VersionBomPolicyRestService createBomVersionService()
			throws IOException, BDRestException, URISyntaxException {
		final List<String> policyRuleList = new ArrayList<>();
		policyRuleList.add("url1");
		final BomComponentVersionPolicyStatus status = Mockito.mock(BomComponentVersionPolicyStatus.class);
		Mockito.when(status.getLinks(Mockito.anyString())).thenReturn(policyRuleList);
		final VersionBomPolicyRestService service = Mockito.mock(VersionBomPolicyRestService.class);
		Mockito.when(service.getPolicyStatus(Mockito.anyString())).thenReturn(status);
		return service;
	}

	private ComponentVersionRestService createComponentVersionService()
			throws NotificationServiceException, IOException, BDRestException, URISyntaxException {

		final ComponentVersion componentVersion = Mockito.mock(ComponentVersion.class);
		Mockito.when(componentVersion.getVersionName()).thenReturn(COMPONENT_VERSION);
		final ComponentVersionRestService service = Mockito.mock(ComponentVersionRestService.class);
		Mockito.when(service.getComponentVersion(Mockito.anyString())).thenReturn(componentVersion);
		return service;
	}

	@Before
	public void initTest() throws Exception {
		notificationService = createNotificationService();
		projectVersionService = createProjectVersionService();
		policyService = createPolicyService();
		bomVersionPolicyService = createBomVersionService();
		componentVersionService = createComponentVersionService();
		policyNameList = new ArrayList<>();
		policyNameList.add("Policy 1");
		policyNameList.add("Policy 2");
		transformer = new PolicyViolationOverrideTransform(notificationService, projectVersionService, policyService,
				bomVersionPolicyService, componentVersionService);

	}

	private PolicyOverrideNotificationItem createNotificationItem() {
		final PolicyOverrideNotificationItem item = Mockito.mock(PolicyOverrideNotificationItem.class);
		final PolicyOverrideNotificationContent content = Mockito.mock(PolicyOverrideNotificationContent.class);
		final List<ComponentVersionStatus> versionStatusList = new ArrayList<>();
		final ComponentVersionStatus status = Mockito.mock(ComponentVersionStatus.class);
		Mockito.when(content.getComponentName()).thenReturn(COMPONENT_NAME);
		versionStatusList.add(status);
		Mockito.when(item.getContent()).thenReturn(content);
		Mockito.when(content.getProjectName()).thenReturn(PROJECT_NAME);
		Mockito.when(content.getFirstName()).thenReturn(FIRST_NAME);
		Mockito.when(content.getLastName()).thenReturn(LAST_NAME);
		return item;
	}

	@Test
	public void testTransform() throws Exception {
		final List<NotificationContentItem> itemList = transformer.transform(createNotificationItem());
		for (final NotificationContentItem item : itemList) {
			final PolicyOverrideContentItem contentItem = (PolicyOverrideContentItem) item;
			assertEquals(PROJECT_NAME, contentItem.getProjectName());
			assertEquals(PROJECT_VERSION, contentItem.getProjectVersion());
			assertEquals(COMPONENT_NAME, contentItem.getComponentName());
			assertEquals(COMPONENT_VERSION, contentItem.getComponentVersion());
			assertEquals(FIRST_NAME, contentItem.getFirstName());
			assertEquals(LAST_NAME, contentItem.getLastName());
			assertEquals(1, contentItem.getPolicyNameList().size());
			assertTrue(contentItem.getPolicyNameList().contains(POLICY_NAME));
		}
	}
}