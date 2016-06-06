/*
 * Copyright 2015 OpenCB
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

package org.opencb.opencga.app.cli.main;


import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.QueryResponse;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.models.File;
import org.opencb.opencga.catalog.models.Study;

import java.io.IOException;

/**
 * Created by imedina on 03/06/16.
 */
public class StudiesCommandExecutor extends OpencgaCommandExecutor {

    private OpencgaCliOptionsParser.StudyCommandsOptions studiesCommandOptions;

    public StudiesCommandExecutor(OpencgaCliOptionsParser.StudyCommandsOptions studiesCommandOptions) {
        super(studiesCommandOptions.commonOptions);
        this.studiesCommandOptions = studiesCommandOptions;
    }



    @Override
    public void execute() throws Exception {
        logger.debug("Executing studies command line");

        String subCommandString = studiesCommandOptions.getParsedSubCommand();
        switch (subCommandString) {
            case "create":
                create();
                break;
            case "info":
                info();
                break;
            case "resync":
                resync();
                break;
            case "list":
                list();
                break;
            case "check-files":
                checkFiles();
                break;
            case "status":
                status();
                break;
            case "annotate-variants":
                annotateVariants();
                break;
            default:
                logger.error("Subcommand not valid");
                break;
        }

    }

    private void create() throws CatalogException, IOException {
        logger.debug("Creating a new project");
        String alias = studiesCommandOptions.createCommand.alias;
        String name = studiesCommandOptions.createCommand.name;
        String description = studiesCommandOptions.createCommand.description;
        String projectId = studiesCommandOptions.createCommand.projectId;
        openCGAClient.getStudyClient().create(projectId,name,alias,description,null);
        /************************************************************************************************ OJO, ULTIMO PARAMETRO PARAMS NULL JUM */
        System.out.println("Done.");
    }

    private void info() throws CatalogException, IOException  {
        logger.debug("Getting the project info");
        QueryResponse<Study> info = openCGAClient.getStudyClient().get(studiesCommandOptions.infoCommand.id,null);
        System.out.println("Study: "+info);
    }
    private void resync() throws CatalogException, IOException {
        logger.debug("Scan the study folder to find changes\n");
        //TODO
    }

    private void list() throws CatalogException, IOException  {
        logger.debug("Listing files in folder");  /**************************************************************** Revisar si esto es file*////
        QueryResponse<File> files = openCGAClient.getStudyClient().getFiles(studiesCommandOptions.listCommand.id,null);
        System.out.println("Files= " + files);
    }
    private void checkFiles() throws CatalogException, IOException {
        logger.debug("Checking if files in study are correctly tracked.");
       //TODO
    }

    private void status() throws CatalogException, IOException {
        logger.debug("Scan the study folder to find untracked or missing files");
        QueryResponse<ObjectMap> status = openCGAClient.getStudyClient().getStatus(studiesCommandOptions.statusCommand.id, null);
        System.out.println("status = " + status);

    }
    private void annotateVariants() throws CatalogException, IOException {
        logger.debug("Annotating variants");
        //TODO
    }




}
