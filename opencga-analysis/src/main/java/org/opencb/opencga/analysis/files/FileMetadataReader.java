package org.opencb.opencga.analysis.files;

import org.opencb.biodata.models.alignment.AlignmentHeader;
import org.opencb.biodata.models.variant.VariantSource;
import org.opencb.datastore.core.ObjectMap;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.datastore.core.QueryResult;
import org.opencb.opencga.catalog.CatalogException;
import org.opencb.opencga.catalog.CatalogManager;
import org.opencb.opencga.catalog.ParamsUtils;
import org.opencb.opencga.catalog.beans.File;
import org.opencb.opencga.catalog.beans.Sample;
import org.opencb.opencga.catalog.beans.Study;
import org.opencb.opencga.storage.core.StorageManagerException;
import org.opencb.opencga.storage.core.variant.VariantStorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public class FileMetadataReader {

    private final CatalogManager catalogManager;
    protected static Logger logger = LoggerFactory.getLogger(FileMetadataReader.class);
    public static final String CREATE_MISSING_SAMPLES = "createMissingSamples";


    public FileMetadataReader(CatalogManager catalogManager) {
        this.catalogManager = catalogManager;
    }

    /**
     * Creates a file entry in catalog reading metadata information from the fileUri.
     * Do not upload or sync file. Created file status will be <b>UPLOADING</b>
     *
     * @param studyId           Study on where the file entry is created
     * @param fileUri           File URI to read metadata information.
     * @param path              File path, relative to the study
     * @param description       File description (optional)
     * @param parents           Create parent folders or not
     * @param options           Other options
     * @param sessionId         User sessionId
     * @return                  The created file with status <b>UPLOADING</b>
     * @throws CatalogException
     */
    public QueryResult<File> create(int studyId, URI fileUri, String path, String description, boolean parents, QueryOptions options, String sessionId) throws CatalogException {

        File.Type type = fileUri.getPath().endsWith("/") ? File.Type.FOLDER : File.Type.FILE;
        File.Format format = FormatDetector.detect(fileUri);
        File.Bioformat bioformat = BioformatDetector.detect(fileUri);


        QueryResult<File> fileResult = catalogManager.createFile(studyId, type, format, bioformat, path, null, null, description,
                File.Status.UPLOADING, 0, -1, null, -1, null, null, parents, options, sessionId);

        File modifiedFile = null;

        try {
            modifiedFile = setMetadataInformation(fileResult.first(), fileUri, options, sessionId, false);
        } catch (CatalogException | StorageManagerException e) {
            logger.error("Fail at getting the metadata information", e);
        }
        fileResult.setResult(Collections.singletonList(modifiedFile));

        return fileResult;
    }

    /**
     * Reads the file and modifies the Catalog file entry with metadata information. The metadata information read is:
     *      Bioformat
     *      Format
     *      FileHeader (for known bioformats)
     *      SampleIds
     *
     * @param file
     * @param fileUri
     * @param options
     * @param sessionId
     * @param simulate
     * @return
     * @throws CatalogException
     * @throws StorageManagerException
     */
    public File setMetadataInformation(File file, URI fileUri, QueryOptions options, String sessionId, boolean simulate)
            throws CatalogException, StorageManagerException {
        int studyId = catalogManager.getStudyIdByFileId(file.getId());
        if (fileUri == null) {
            fileUri = catalogManager.getFileUri(file);
        }
        ObjectMap modifyParams = new ObjectMap();

        //Get metadata information

        File.Format format = FormatDetector.detect(fileUri);
        File.Bioformat bioformat = BioformatDetector.detect(fileUri);

        if (!format.equals(file.getFormat())) {
            modifyParams.put("format", format);
        }
        if (!bioformat.equals(file.getBioformat())) {
            modifyParams.put("bioformat", bioformat);
        }

        Study study = catalogManager.getStudy(studyId, sessionId).first();
        if (catalogManager.getCatalogIOManagerFactory().get(fileUri).exists(fileUri)) {
            switch (bioformat) {
                case ALIGNMENT:
                    break;
                case VARIANT:
                    VariantSource variantSource = readVariantSource(study, file, fileUri);
                    HashMap<String, Object> attributes = new HashMap<>();
                    attributes.put("variantSource", variantSource);
                    modifyParams.put("attributes", attributes);
                    break;
                default:
                    break;
            }
        }
        List<Sample> fileSamples = getFileSamples(study, file, fileUri, modifyParams, simulate, options, sessionId);

        if (!modifyParams.isEmpty()) {
            catalogManager.modifyFile(file.getId(), modifyParams, sessionId);
            return catalogManager.getFile(file.getId(), sessionId).first();
        }

        return file;
    }

    public List<Sample> getFileSamples(Study study, File file, URI fileUri, final ObjectMap fileModifyParams,
                                       boolean simulate, QueryOptions options, String sessionId)
            throws CatalogException, StorageManagerException {
        options = ParamsUtils.defaultObject(options, QueryOptions::new);

        List<Sample> sampleList;

        if (!fileModifyParams.containsKey("attributes")) {
            fileModifyParams.put("attributes", new HashMap<String, Object>());
        }

        List<String> includeSampleNameId = Arrays.asList("projects.studies.samples.id", "projects.studies.samples.name");
        if (file.getSampleIds() == null || file.getSampleIds().isEmpty()) {
            //Read samples from file
            List<String> sampleNames = null;
            switch (fileModifyParams.containsKey("bioformat")? (File.Bioformat) fileModifyParams.get("bioformat") : file.getBioformat()) {
                case VARIANT: {
                    Object variantSourceObj = null;
                    if (file.getAttributes().containsKey("variantSource")) {
                        variantSourceObj = file.getAttributes().get("variantSource");
                    } else if (fileModifyParams.getMap("attributes").containsKey("variantSource")) {
                        variantSourceObj = fileModifyParams.getMap("attributes").get("variantSource");
                    }
                    if (variantSourceObj != null) {
                        if (variantSourceObj instanceof VariantSource) {
                            sampleNames = ((VariantSource) variantSourceObj).getSamples();
                        } else if (variantSourceObj instanceof Map) {
                            sampleNames = new ObjectMap((Map) variantSourceObj).getAsStringList("samples");
                        } else {
                            logger.warn("Unexpected object type of variantSource ({}) in file attributes. Expected {} or {}", variantSourceObj.getClass(), VariantSource.class, Map.class);
                        }
                    }

                    if (sampleNames == null) {
                        VariantSource variantSource = readVariantSource(study, file, fileUri);
                        fileModifyParams.get("attributes", Map.class).put("variantSource", variantSource);
                        sampleNames = variantSource.getSamples();
                    }
                }
                break;
                default:
                    return new LinkedList<>();
//                    throw new CatalogException("Unknown to get samples names from bioformat " + file.getBioformat());
            }

            //Find matching samples in catalog with the sampleName from the VariantSource.
            QueryOptions sampleQueryOptions = new QueryOptions("include", includeSampleNameId);
            sampleQueryOptions.add("name", sampleNames);
            sampleList = catalogManager.getAllSamples(study.getId(), sampleQueryOptions, sessionId).getResult();

            //check if all file samples exists on Catalog
            if (sampleList.size() != sampleNames.size()) {   //Size does not match. Find the missing samples.
                Set<String> set = new HashSet<>(sampleNames);
                for (Sample sample : sampleList) {
                    set.remove(sample.getName());
                }
                logger.warn("Missing samples: m{}", set);
                if (options.getBoolean(CREATE_MISSING_SAMPLES, true)) {
                    for (String sampleName : set) {
                        if (simulate) {
                            sampleList.add(new Sample(-1, sampleName, file.getName(), null, null));
                        } else {
                            try {
                                sampleList.add(catalogManager.createSample(study.getId(), sampleName, file.getName(), null, null, null, sessionId).first());
                            } catch (CatalogException e) {
                                QueryOptions queryOptions = new QueryOptions("name", sampleName);
                                queryOptions.add("include", includeSampleNameId);
                                if (catalogManager.getAllSamples(study.getId(), queryOptions, sessionId).getResult().isEmpty()) {
                                    throw e; //Throw exception if sample does not exist.
                                } else {
                                    logger.debug("Do not create the sample \"" + sampleName + "\". It has magically appeared");
                                }
                            }
                        }
                    }
                } else {
                    throw new CatalogException("Can not find samples " + set + " in catalog"); //FIXME: Create missing samples??
                }
            }
        } else {
            //Get samples from file.sampleIds
            sampleList = catalogManager.getAllSamples(study.getId(), new QueryOptions("id", file.getSampleIds()), sessionId).getResult();
        }

        List<Integer> sampleIdsList = sampleList.stream().map(Sample::getId).collect(Collectors.toList());
        fileModifyParams.put("sampleIds", sampleIdsList);

        return sampleList;
    }

    public static VariantSource readVariantSource(Study study, File file, URI fileUri)
            throws StorageManagerException {
        //TODO: Fix aggregate and studyType
        VariantSource source = new VariantSource(file.getName(), Integer.toString(file.getId()), Integer.toString(study.getId()), study.getName());
        return VariantStorageManager.readVariantSource(Paths.get(fileUri.getPath()), source);
    }

    public static AlignmentHeader readAlignmentHeader(Study study, File file, URI fileUri) {
        logger.warn("Unimplemented method readAlignmentHeader");
        return null;
    }

    public static FileMetadataReader get(CatalogManager catalogManager) {
        return new FileMetadataReader(catalogManager);
    }
}
