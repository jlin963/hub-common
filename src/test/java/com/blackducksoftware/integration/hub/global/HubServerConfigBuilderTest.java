/**
 * Hub Common
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hub.global;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder;
import com.blackducksoftware.integration.hub.validator.HubServerConfigValidator;
import com.blackducksoftware.integration.validator.ValidationResults;

public class HubServerConfigBuilderTest {

    private static final String VALID_TIMEOUT_STRING = "120";

    private static final String VALID_USERNAME_STRING = "username";

    private static final String VALID_PASSWORD_STRING = "password";

    private static final int VALID_TIMEOUT_INTEGER = 120;

    private static final String VALID_URL = "https://www.google.com";

    private static final String ERROR_MSG_NO_HUB_TIMEOUT = "No Hub Timeout was found.";

    private static final int VALID_PROXY_PORT = 2303;

    private static final String VALID_PROXY_HOST = "just need a non-empty string";

    private static final String VALID_IGNORE_HOST_LIST = ".*google.*,.*msn.*,.*yahoo.*";

    private List<String> expectedMessages;

    private List<String> actualMessages;

    @Before
    public void setUp() {
        expectedMessages = new ArrayList<>();
        actualMessages = new ArrayList<>();
    }

    @After
    public void tearDown() {
        assertEquals("Too many/not enough messages expected: \n" + actualMessages.size(), expectedMessages.size(),
                actualMessages.size());

        for (final String expectedMessage : expectedMessages) {
            boolean foundExpectedMessage = false;
            for (final String actualMessage : actualMessages) {
                if (actualMessage.contains(expectedMessage)) {
                    foundExpectedMessage = true;
                    break;
                }
            }
            assertTrue("Did not find the expected message : " + expectedMessage,
                    foundExpectedMessage);
        }
    }

    private List<String> getMessages(final ValidationResults result) {
        final List<String> messageList = new ArrayList<>();
        final Map<Object, Set<String>> resultMap = result.getResultMap();
        for (final Object key : resultMap.keySet()) {
            final Set<String> resultList = resultMap.get(key);

            for (final String item : resultList) {
                if (StringUtils.isNotBlank(item)) {
                    messageList.add(item);
                }
            }
        }
        return messageList;
    }

    @Test
    public void testValidateHubURLEmpty() throws Exception {
        expectedMessages.add(HubServerConfigValidator.ERROR_MSG_URL_NOT_FOUND);

        final HubServerConfigValidator validator = new HubServerConfigValidator();
        final ValidationResults result = new ValidationResults();
        validator.validateHubUrl(result);
        assertFalse(result.isSuccess());

        actualMessages = getMessages(result);
    }

    @Test
    public void testValidateHubURLMalformed() throws Exception {
        expectedMessages.add(HubServerConfigValidator.ERROR_MSG_URL_NOT_VALID);

        final HubServerConfigValidator validator = new HubServerConfigValidator();
        validator.setHubUrl("TestString");
        final ValidationResults result = new ValidationResults();
        validator.validateHubUrl(result);
        assertFalse(result.isSuccess());

        actualMessages = getMessages(result);
    }

    @Test
    public void testValidateHubURLMalformed2() throws Exception {
        expectedMessages.add(HubServerConfigValidator.ERROR_MSG_UNREACHABLE_PREFIX);

        final HubServerConfigValidator validator = new HubServerConfigValidator();
        validator.setHubUrl("http:TestString");
        final ValidationResults result = new ValidationResults();
        validator.validateHubUrl(result);
        assertFalse(result.isSuccess());

        actualMessages = getMessages(result);
    }

    @Test
    public void testValidateHubURLNotExisting() throws Exception {
        expectedMessages.add(HubServerConfigValidator.ERROR_MSG_UNREACHABLE_PREFIX);

        final HubServerConfigValidator validator = new HubServerConfigValidator();
        validator.setHubUrl("http://TestString");
        final ValidationResults result = new ValidationResults();
        validator.validateHubUrl(result);
        assertFalse(result.isSuccess());

        actualMessages = getMessages(result);
    }

    @Test
    public void testValidateHubURL() throws Exception {
        final HubServerConfigValidator validator = new HubServerConfigValidator();
        validator.setHubUrl(VALID_URL);
        final ValidationResults result = new ValidationResults();
        validator.validateHubUrl(result);
        assertTrue(result.isSuccess());

        actualMessages = getMessages(result);
    }

    @Test
    public void testValidateHubTimeoutEmpty() throws Exception {
        expectedMessages.add(ERROR_MSG_NO_HUB_TIMEOUT);

        final HubServerConfigValidator validator = new HubServerConfigValidator();
        final ValidationResults result = new ValidationResults();
        validator.validateTimeout(result);
        assertFalse(result.isSuccess());

        actualMessages = getMessages(result);
    }

    @Test
    public void testValidateHubTimeoutInvalid() throws Exception {
        expectedMessages.add("The String : TestString , is not an Integer");

        final HubServerConfigValidator validator = new HubServerConfigValidator();
        validator.setTimeout("TestString");
        final ValidationResults result = new ValidationResults();
        validator.validateTimeout(result);
        assertFalse(result.isSuccess());

        actualMessages = getMessages(result);
    }

    @Test
    public void testValidateNegativeHubTimeout() throws Exception {
        expectedMessages.add("The Timeout must be greater than 0.");
        final HubServerConfigValidator validator = new HubServerConfigValidator();
        validator.setTimeout(-678);
        final ValidationResults result = new ValidationResults();
        validator.validateTimeout(result);
        assertFalse(result.isSuccess());

        actualMessages = getMessages(result);
    }

    @Test
    public void testValidateHubTimeout() throws Exception {
        final HubServerConfigValidator validator = new HubServerConfigValidator();
        validator.setTimeout("678");
        final ValidationResults result = new ValidationResults();
        validator.validateTimeout(result);
        assertTrue(result.isSuccess());

        actualMessages = getMessages(result);
    }

    @Test
    public void testValidateHubTimeoutInteger() throws Exception {
        final HubServerConfigValidator validator = new HubServerConfigValidator();
        validator.setTimeout(678);
        final ValidationResults result = new ValidationResults();
        validator.validateTimeout(result);
        assertTrue(result.isSuccess());

        actualMessages = getMessages(result);
    }

    @Test
    public void testValidBuild() throws Exception {
        final HubServerConfigBuilder builder = new HubServerConfigBuilder();
        builder.setHubUrl(VALID_URL);
        builder.setTimeout(VALID_TIMEOUT_INTEGER);
        builder.setPassword(VALID_PASSWORD_STRING);
        builder.setUsername(VALID_USERNAME_STRING);
        final HubServerConfig config = builder.build();
        assertEquals(VALID_URL, config.getHubUrl().toString());
        assertEquals(VALID_TIMEOUT_INTEGER, config.getTimeout());
        assertEquals(VALID_USERNAME_STRING, config.getGlobalCredentials().getUsername());
        assertEquals(VALID_PASSWORD_STRING, config.getGlobalCredentials().getDecryptedPassword());
    }

    @Test
    public void testValidBuildTimeoutString() throws Exception {
        final HubServerConfigBuilder builder = new HubServerConfigBuilder();
        builder.setHubUrl(VALID_URL);
        builder.setTimeout(VALID_TIMEOUT_STRING);
        builder.setPassword(VALID_PASSWORD_STRING);
        builder.setUsername(VALID_USERNAME_STRING);
        final HubServerConfig config = builder.build();

        assertEquals(VALID_URL, config.getHubUrl().toString());
        assertEquals(120, config.getTimeout());
        assertEquals(VALID_USERNAME_STRING, config.getGlobalCredentials().getUsername());
        assertEquals(VALID_PASSWORD_STRING, config.getGlobalCredentials().getDecryptedPassword());
    }

    @Test
    public void testValidBuildWithProxy() throws Exception {
        final HubServerConfigBuilder builder = new HubServerConfigBuilder();
        builder.setHubUrl(VALID_URL);
        builder.setTimeout(VALID_TIMEOUT_STRING);
        builder.setPassword(VALID_PASSWORD_STRING);
        builder.setUsername(VALID_USERNAME_STRING);
        builder.setProxyHost(VALID_PROXY_HOST);
        builder.setProxyPort(VALID_PROXY_PORT);
        builder.setIgnoredProxyHosts(VALID_IGNORE_HOST_LIST);
        final HubServerConfig config = builder.build();

        assertEquals(VALID_URL, config.getHubUrl().toString());
        assertEquals(VALID_TIMEOUT_INTEGER, config.getTimeout());
        assertEquals(VALID_USERNAME_STRING, config.getGlobalCredentials().getUsername());
        assertEquals(VALID_PASSWORD_STRING, config.getGlobalCredentials().getDecryptedPassword());
        assertEquals(VALID_PROXY_HOST, config.getProxyInfo().getHost());
        assertEquals(VALID_PROXY_PORT, config.getProxyInfo().getPort());
        assertEquals(VALID_IGNORE_HOST_LIST, config.getProxyInfo().getIgnoredProxyHosts());
    }

    @Test
    public void testUrlwithTrailingSlash() throws Exception {
        final HubServerConfigBuilder builder = new HubServerConfigBuilder();
        builder.setHubUrl("https://www.google.com:443/");
        builder.setTimeout(VALID_TIMEOUT_STRING);
        builder.setPassword(VALID_PASSWORD_STRING);
        builder.setUsername(VALID_USERNAME_STRING);
        final HubServerConfig config = builder.build();
        assertFalse(config.getHubUrl().toString().endsWith("/"));
        assertEquals("https", config.getHubUrl().getProtocol());
        assertEquals("www.google.com", config.getHubUrl().getHost());
        assertEquals(443, config.getHubUrl().getPort());
    }

    @Test
    public void testUrlwithTrailingPath() {
        final HubServerConfigBuilder builder = new HubServerConfigBuilder();
        builder.setHubUrl("https://github.com:443/blackducksoftware");
        builder.setTimeout(VALID_TIMEOUT_STRING);
        builder.setPassword(VALID_PASSWORD_STRING);
        builder.setUsername(VALID_USERNAME_STRING);
        final HubServerConfig config = builder.build();
        assertFalse(config.getHubUrl().toString().endsWith("/"));
        assertEquals("https", config.getHubUrl().getProtocol());
        assertEquals("github.com", config.getHubUrl().getHost());
        assertEquals(443, config.getHubUrl().getPort());
        assertEquals("/blackducksoftware", config.getHubUrl().getPath());
    }

    @Test
    public void testValidBuildWithProxyPortZero() throws Exception {
        final HubServerConfigBuilder builder = new HubServerConfigBuilder();
        builder.setHubUrl(VALID_URL);
        builder.setPassword(VALID_PASSWORD_STRING);
        builder.setUsername(VALID_USERNAME_STRING);
        HubServerConfig config = builder.build();
        assertFalse(config.shouldUseProxyForHub());

        builder.setProxyPort(0);
        config = builder.build();
        assertFalse(config.shouldUseProxyForHub());

        builder.setProxyPort("0");
        config = builder.build();
        assertFalse(config.shouldUseProxyForHub());

        builder.setProxyPort(1);
        try {
            config = builder.build();
            fail("Should have thrown an IllegalStateException with invalid proxy state");
        } catch (final IllegalStateException e) {
            assertTrue(e.getMessage().contains("proxy"));
        }
    }

}
