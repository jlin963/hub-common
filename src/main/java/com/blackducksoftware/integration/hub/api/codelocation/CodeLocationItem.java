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
package com.blackducksoftware.integration.hub.api.codelocation;

import java.util.Date;

import com.blackducksoftware.integration.hub.api.item.HubItem;
import com.blackducksoftware.integration.hub.meta.MetaInformation;

public class CodeLocationItem extends HubItem {
    private final CodeLocationTypeEnum type;

    private final String url;

    private final String mappedProjectVersion;

    private final Date createdAt;

    private final Date updatedAt;

    public CodeLocationItem(final MetaInformation meta, final CodeLocationTypeEnum type, final String url,
            final String mappedProjectVersion, final Date createdAt, final Date updatedAt) {
        super(meta);
        this.type = type;
        this.url = url;
        this.mappedProjectVersion = mappedProjectVersion;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public CodeLocationTypeEnum getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getMappedProjectVersion() {
        return mappedProjectVersion;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

}
