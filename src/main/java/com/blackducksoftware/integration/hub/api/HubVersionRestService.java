/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
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
 *******************************************************************************/
package com.blackducksoftware.integration.hub.api;

import static com.blackducksoftware.integration.hub.api.UrlConstants.QUERY_VERSION;
import static com.blackducksoftware.integration.hub.api.UrlConstants.SEGMENT_API;
import static com.blackducksoftware.integration.hub.api.UrlConstants.SEGMENT_CURRENT_VERSION;
import static com.blackducksoftware.integration.hub.api.UrlConstants.SEGMENT_CURRENT_VERSION_COMPARISON;
import static com.blackducksoftware.integration.hub.api.UrlConstants.SEGMENT_V1;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import com.blackducksoftware.integration.hub.api.version.VersionComparison;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.ResourceDoesNotExistException;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.google.gson.JsonObject;

public class HubVersionRestService extends HubRestService<VersionComparison> {
    private static final List<String> CURRENT_VERSION_SEGMENTS = Arrays.asList(SEGMENT_API, SEGMENT_V1, SEGMENT_CURRENT_VERSION);

    private static final List<String> CURRENT_VERSION_COMPARISON_SEGMENTS = Arrays.asList(SEGMENT_API, SEGMENT_V1, SEGMENT_CURRENT_VERSION_COMPARISON);

    public HubVersionRestService(RestConnection restConnection) {
        super(restConnection, VersionComparison.class);
    }

    public String getHubVersion() throws IOException, ResourceDoesNotExistException, URISyntaxException, BDRestException {
        final String hubVersionWithPossibleSurroundingQuotes = getString(CURRENT_VERSION_SEGMENTS);
        final String hubVersion = hubVersionWithPossibleSurroundingQuotes.replace("\"", "");

        return hubVersion;
    }

    public VersionComparison getHubVersionComparison(String consumerVersion) throws IOException, URISyntaxException, BDRestException {
        final HubRequest hubVersionRequest = getHubRequestFactory().createGetRequest(CURRENT_VERSION_COMPARISON_SEGMENTS).addQueryParameter(QUERY_VERSION,
                consumerVersion);
        final JsonObject jsonObject = hubVersionRequest.executeForResponseJson();
        final VersionComparison versionComparison = getItem(jsonObject, VersionComparison.class);
        return versionComparison;
    }

    public boolean isConsumerVersionLessThanOrEqualToServerVersion(String consumerVersion) throws IOException, URISyntaxException, BDRestException {
        final VersionComparison versionComparison = getHubVersionComparison(consumerVersion);
        if (versionComparison.getNumericResult() <= 0) {
            return true;
        } else {
            return false;
        }
    }

}
