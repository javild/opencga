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

package org.opencb.opencga.server.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.ga4gh.methods.SearchVariantsRequest;
import org.ga4gh.methods.SearchVariantsResponse;
import org.ga4gh.models.Variant;
import org.opencb.biodata.models.core.Region;
import org.opencb.opencga.core.exception.VersionException;
import org.opencb.opencga.analysis.storage.variant.VariantFetcher;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.List;

import static org.opencb.opencga.storage.core.variant.adaptors.VariantDBAdaptor.VariantQueryParams.*;

/**
 * Created on 09/10/15
 *
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */

@Path("/{version}/ga4gh")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "GA4GH", position = 13, description = "Global Alliance for Genomics & Health RESTful API")
public class Ga4ghWSServer extends OpenCGAWSServer {

    public Ga4ghWSServer(@Context UriInfo uriInfo, @Context HttpServletRequest httpServletRequest) throws IOException, VersionException {
        super(uriInfo, httpServletRequest);
    }

    @POST
    @Path("/variants/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Description", position = 1, notes = "Notes")
    public Response searchVariants(SearchVariantsRequest request) {
        String method = "ga4gh/variants/search";
        try {

            if (request.getVariantSetId() == null || request.getVariantSetId().isEmpty()) {
                return createErrorResponse(method, "Required referenceName or referenceId");
            }
            queryOptions.append(STUDIES.key(), request.getVariantSetId());
            String studyIdStr = queryOptions.getAsStringList(STUDIES.key()).get(0);
            long studyId = catalogManager.getStudyId(studyIdStr);

//        queryOptions.append(, request.getVariantName()); //TODO
            if (request.getCallSetIds() != null) {
                queryOptions.append(RETURNED_SAMPLES.key(), request.getCallSetIds());
            }

            CharSequence chr = null;
            if (request.getReferenceName() != null) {
                chr = request.getReferenceName();
            }
            if (chr == null) {
                return createErrorResponse(method, "Required referenceName or referenceId");
            }
            if (request.getStart() == null || request.getStart() < 0) {
                return createErrorResponse(method, "Required start position");
            }
            if (request.getEnd() == null || request.getEnd() < 0) {
                return createErrorResponse(method, "Required end position");
            }
            long delta = request.getEnd() - request.getStart();
            if (delta < 0/* || delta > 20000*/) {
                return createErrorResponse(method, "End must be behind the start");
            }
            queryOptions.append(REGION.key(), new Region(chr.toString(), request.getStart().intValue(), request.getEnd().intValue()));

            if (request.getPageSize() == null || request.getPageSize() <= 0 || request.getPageSize() > 4000) {
                this.queryOptions.add("limit", 1000);
            } else {
                this.queryOptions.add("limit", request.getPageSize());
            }

            int page = 0;
            if (request.getPageToken() != null) {
                try {
                    page = Integer.parseInt(request.getPageToken().toString());
                    this.queryOptions.put("skip", this.queryOptions.getInt("limit") * page);
                } catch (Exception e) {
                    return createErrorResponse(method, "Invalid page token \"" + request.getPageToken() + "\"");
                }
            }

            queryOptions.add("model", "ga4gh");
            SearchVariantsResponse response = new SearchVariantsResponse();

            VariantFetcher variantFetcher = new VariantFetcher(catalogManager, storageManagerFactory);
            List<Variant> variants = variantFetcher.getVariantsPerStudy((int) studyId, queryOptions.getString(REGION.key()), false, null, 0, sessionId, queryOptions).getResult();
            response.setNextPageToken(Integer.toString(++page));
            response.setVariants(variants);
            return buildResponse(Response.ok(response.toString(), MediaType.APPLICATION_JSON_TYPE));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

//    @GET
//    @Path("/variants/{id}")
//    @Consumes(MediaType.APPLICATION_JSON)
//    @ApiOperation(value = "Description", position = 1, notes = "Notes")
//    public Response getVariant(@PathParam("id") String id) {
//
//
//        SearchVariantsResponse response = new SearchVariantsResponse();
//        return createOkResponse(response);
//    }



//    @Path("/variantsets/search")
//    @Path("/variantsets/{id}/sequences/search")
//    @Path("/variantsets/{id}/sequences/{id}")
//    @Path("/alleles/search")
//    @Path("/alleles/{id}")
//    @Path("/callsets/search")
//    @Path("/callsets/{id}")
//    @Path("/call/search")
//    @Path("/allelecalls/search")

}
