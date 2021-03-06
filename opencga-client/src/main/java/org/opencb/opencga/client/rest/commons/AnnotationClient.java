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

package org.opencb.opencga.client.rest.commons;

import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.QueryResponse;
import org.opencb.opencga.catalog.models.AnnotationSet;
import org.opencb.opencga.client.config.ClientConfiguration;
import org.opencb.opencga.client.rest.AbstractParentClient;

import java.io.IOException;

/**
 * Created by pfurio on 28/07/16.
 */
public abstract class AnnotationClient<T, U> extends AbstractParentClient<T, U> {

    protected AnnotationClient(String userId, String sessionId, ClientConfiguration configuration) {
        super(userId, sessionId, configuration);
    }

    public QueryResponse<AnnotationSet> createAnnotationSet(String id, String variableSetId, String annotationSetName,
                                                            ObjectMap annotations) throws IOException {
        ObjectMap params = new ObjectMap()
                .append("body", annotations)
                .append("variableSetId", variableSetId)
                .append("annotateSetName", annotationSetName);
        return execute(category, id, "annotationSets", null, "create", params, POST, AnnotationSet.class);
    }

    public QueryResponse<AnnotationSet> getAllAnnotationSets(String id, ObjectMap params) throws IOException {
        return execute(category, id, "annotationSets", null, "info", params, GET, AnnotationSet.class);
    }

    public QueryResponse<AnnotationSet> getAnnotationSet(String id, String annotationSetName, ObjectMap params) throws IOException {
        return execute(category, id, "annotationSets", annotationSetName, "info", params, GET, AnnotationSet.class);
    }

    public QueryResponse<AnnotationSet> searchAnnotationSets(String id, ObjectMap params) throws IOException {
        return execute(category, id, "annotationSets", null, "search", params, GET, AnnotationSet.class);
    }

    public QueryResponse<AnnotationSet> deleteAnnotationSet(String id, String annotationSetName, ObjectMap params) throws IOException {
        return execute(category, id, "annotationSets", annotationSetName, "delete", params, GET, AnnotationSet.class);
    }

    public QueryResponse<AnnotationSet> updateAnnotationSet(String id, String annotationSetName, ObjectMap annotations) throws IOException {
        ObjectMap params = new ObjectMap("body", annotations);
        return execute(category, id, "annotationSets", annotationSetName, "update", params, POST, AnnotationSet.class);
    }

}
