/*
 * Copyright 2015-2016 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.client.rest;


import org.opencb.opencga.catalog.models.Tool;
import org.opencb.opencga.client.config.ClientConfiguration;
import org.opencb.opencga.catalog.models.acls.permissions.ToolAclEntry;


/**
 * Created by sgallego on 6/30/16.
 */
public class ToolClient extends AbstractParentClient<Tool, ToolAclEntry> {

    private static final String TOOLS_URL = "tools";

    protected ToolClient(String userId, String sessionId, ClientConfiguration configuration) {
        super(userId, sessionId, configuration);

        this.category = TOOLS_URL;
        this.clazz = Tool.class;
        this.aclClass = ToolAclEntry.class;
    }
}
