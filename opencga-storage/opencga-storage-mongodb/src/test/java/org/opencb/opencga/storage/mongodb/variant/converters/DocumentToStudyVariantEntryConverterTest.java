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

package org.opencb.opencga.storage.mongodb.variant.converters;

import com.google.common.collect.Lists;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.opencb.biodata.models.variant.StudyEntry;
import org.opencb.biodata.models.variant.avro.FileEntry;
import org.opencb.opencga.storage.core.metadata.StudyConfiguration;
import org.opencb.opencga.storage.mongodb.variant.MongoDBVariantStorageManager;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Cristina Yenyxe Gonzalez Garcia <cyenyxe@ebi.ac.uk>
 */
public class DocumentToStudyVariantEntryConverterTest {

    private StudyEntry studyEntry;
    private Document mongoStudy;
    private Document mongoFileWithIds;

    private List<String> sampleNames;
    private Integer fileId;
    private Integer studyId;
    private StudyConfiguration studyConfiguration;

    @Before
    public void setUp() {
        // Java native class
        studyId = 1;
        fileId = 2;
        studyEntry = new StudyEntry(studyId.toString());
        FileEntry fileEntry = new FileEntry(fileId.toString(), "", new HashMap<>());
        fileEntry.getAttributes().put("QUAL", "0.01");
        fileEntry.getAttributes().put("AN", "2.0");
        fileEntry.getAttributes().put("do.we.accept.attribute.with.dots?", "yes");
        studyEntry.setFiles(Collections.singletonList(fileEntry));
        studyEntry.setFormatAsString("GT");

        Map<String, String> na001 = new HashMap<>();
        na001.put("GT", "0/0");
        studyEntry.addSampleData("NA001", na001);
        Map<String, String> na002 = new HashMap<>();
        na002.put("GT", "0/1");
        studyEntry.addSampleData("NA002", na002);
        Map<String, String> na003 = new HashMap<>();
        na003.put("GT", "1/1");
        studyEntry.addSampleData("NA003", na003);

        // MongoDB object
        mongoStudy = new Document(DocumentToStudyVariantEntryConverter.STUDYID_FIELD, studyId);

        Document mongoFile = new Document(DocumentToStudyVariantEntryConverter.FILEID_FIELD, fileId);
        String dot = DocumentToStudyConfigurationConverter.TO_REPLACE_DOTS;
        mongoFile.append(DocumentToStudyVariantEntryConverter.ATTRIBUTES_FIELD,
                new Document("QUAL", 0.01)
                        .append("AN", 2.0)
                        .append("do" + dot + "we" + dot + "accept" + dot + "attribute" + dot + "with" + dot + "dots?", "yes")
        ).append(DocumentToStudyVariantEntryConverter.SAMPLE_DATA_FIELD, new Document());
//        mongoFile.append(DocumentToVariantSourceEntryConverter.FORMAT_FIELD, file.getFormat());
        mongoStudy.append(DocumentToStudyVariantEntryConverter.FILES_FIELD, Collections.singletonList(mongoFile));

        Document genotypeCodes = new Document();
//        genotypeCodes.append("def", "0/0");
        genotypeCodes.append("0/1", Collections.singletonList(1));
        genotypeCodes.append("1/1", Collections.singletonList(2));
        mongoStudy.append(DocumentToStudyVariantEntryConverter.GENOTYPES_FIELD, genotypeCodes);

        studyConfiguration = new StudyConfiguration(studyId, "");
        Map<String, Integer> sampleIds = new HashMap<>();
        sampleIds.put("NA001", 15);
        sampleIds.put("NA002", 25);
        sampleIds.put("NA003", 35);
        studyConfiguration.setSampleIds(sampleIds);
        studyConfiguration.getIndexedFiles().add(fileId);
        studyConfiguration.getSamplesInFiles().put(fileId, new LinkedHashSet<>(Arrays.asList(15, 25, 35)));
        studyConfiguration.getAttributes().put(MongoDBVariantStorageManager.MongoDBVariantOptions.DEFAULT_GENOTYPE.key(), Collections.singleton("0/0"));

        sampleNames = Lists.newArrayList("NA001", "NA002", "NA003");


        mongoFileWithIds = new Document((this.mongoStudy));
        mongoFileWithIds.put(DocumentToStudyVariantEntryConverter.GENOTYPES_FIELD, new Document());
//        ((Document) mongoFileWithIds.get("samp")).put("def", "0/0");
        ((Document) mongoFileWithIds.get(DocumentToStudyVariantEntryConverter.GENOTYPES_FIELD)).put("0/1", Collections.singletonList(25));
        ((Document) mongoFileWithIds.get(DocumentToStudyVariantEntryConverter.GENOTYPES_FIELD)).put("1/1", Collections.singletonList(35));
    }

    /* TODO move to variant converter: sourceEntry does not have stats anymore
    @Test
    public void testConvertToDataModelTypeWithStats() {
        VariantStats stats = new VariantStats(null, -1, null, null, Variant.VariantType.SNV, 0.1f, 0.01f, "A", "A/A", 10, 5, -1, -1, -1,
        -1, -1);
        stats.addGenotype(new Genotype("0/0"), 100);
        stats.addGenotype(new Genotype("0/1"), 50);
        stats.addGenotype(new Genotype("1/1"), 10);
        file.setStats(stats);
        file.getSamplesData().clear(); // TODO Samples can't be tested easily, needs a running Mongo instance
        
        Document mongoStats = new Document(DocumentToVariantStatsConverter.MAF_FIELD, 0.1);
        mongoStats.append(DocumentToVariantStatsConverter.MGF_FIELD, 0.01);
        mongoStats.append(DocumentToVariantStatsConverter.MAFALLELE_FIELD, "A");
        mongoStats.append(DocumentToVariantStatsConverter.MGFGENOTYPE_FIELD, "A/A");
        mongoStats.append(DocumentToVariantStatsConverter.MISSALLELE_FIELD, 10);
        mongoStats.append(DocumentToVariantStatsConverter.MISSGENOTYPE_FIELD, 5);
        Document genotypes = new Document();
        genotypes.append("0/0", 100);
        genotypes.append("0/1", 50);
        genotypes.append("1/1", 10);
        mongoStats.append(DocumentToVariantStatsConverter.NUMGT_FIELD, genotypes);
        mongoStudy.append(DocumentToVariantSourceEntryConverter.STATS_FIELD, mongoStats);
        
        List<String> sampleNames = null;
        DocumentToVariantSourceEntryConverter converter = new DocumentToVariantSourceEntryConverter(
                true, new DocumentToSamplesConverter(sampleNames));
        VariantSourceEntry converted = converter.convertToDataModelType(mongoStudy);
        assertEquals(file, converted);
    }

    @Test
    public void testConvertToStorageTypeWithStats() {
        VariantStats stats = new VariantStats(null, -1, null, null, Variant.VariantType.SNV, 0.1f, 0.01f, "A", "A/A", 10, 5, -1, -1, -1,
        -1, -1);
        stats.addGenotype(new Genotype("0/0"), 100);
        stats.addGenotype(new Genotype("0/1"), 50);
        stats.addGenotype(new Genotype("1/1"), 10);
        file.setStats(stats);
        
        Document mongoStats = new Document(DocumentToVariantStatsConverter.MAF_FIELD, 0.1);
        mongoStats.append(DocumentToVariantStatsConverter.MGF_FIELD, 0.01);
        mongoStats.append(DocumentToVariantStatsConverter.MAFALLELE_FIELD, "A");
        mongoStats.append(DocumentToVariantStatsConverter.MGFGENOTYPE_FIELD, "A/A");
        mongoStats.append(DocumentToVariantStatsConverter.MISSALLELE_FIELD, 10);
        mongoStats.append(DocumentToVariantStatsConverter.MISSGENOTYPE_FIELD, 5);
        Document genotypes = new Document();
        genotypes.append("0/0", 100);
        genotypes.append("0/1", 50);
        genotypes.append("1/1", 10);
        mongoStats.append(DocumentToVariantStatsConverter.NUMGT_FIELD, genotypes);
        mongoStudy.append(DocumentToVariantSourceEntryConverter.STATS_FIELD, mongoStats);

        DocumentToVariantSourceEntryConverter converter = new DocumentToVariantSourceEntryConverter(
                true, new DocumentToSamplesConverter(sampleNames));
        Document converted = converter.convertToStorageType(file);
        
        assertEquals(mongoStudy.get(DocumentToVariantStatsConverter.MAF_FIELD), converted.get(DocumentToVariantStatsConverter.MAF_FIELD));
        assertEquals(mongoStudy.get(DocumentToVariantStatsConverter.MGF_FIELD), converted.get(DocumentToVariantStatsConverter.MGF_FIELD));
        assertEquals(mongoStudy.get(DocumentToVariantStatsConverter.MAFALLELE_FIELD), converted.get(DocumentToVariantStatsConverter
        .MAFALLELE_FIELD));
        assertEquals(mongoStudy.get(DocumentToVariantStatsConverter.MGFGENOTYPE_FIELD), converted.get(DocumentToVariantStatsConverter
        .MGFGENOTYPE_FIELD));
        assertEquals(mongoStudy.get(DocumentToVariantStatsConverter.MISSALLELE_FIELD), converted.get(DocumentToVariantStatsConverter
        .MISSALLELE_FIELD));
        assertEquals(mongoStudy.get(DocumentToVariantStatsConverter.MISSGENOTYPE_FIELD), converted.get(DocumentToVariantStatsConverter
        .MISSGENOTYPE_FIELD));
        assertEquals(mongoStudy.get(DocumentToVariantStatsConverter.NUMGT_FIELD), converted.get(DocumentToVariantStatsConverter
        .NUMGT_FIELD));
    }
    */
    @Test
    public void testConvertToDataModelTypeWithoutStats() {
        studyEntry.getSamplesData().clear(); // TODO Samples can't be tested easily, needs a running Mongo instance
        List<String> sampleNames = null;

        // Test with no stats converter provided
        DocumentToStudyVariantEntryConverter converter = new DocumentToStudyVariantEntryConverter(true, fileId, new
                DocumentToSamplesConverter(studyId, sampleNames, "0/0"));
        StudyEntry converted = converter.convertToDataModelType(mongoStudy);
        assertEquals(studyEntry, converted);
    }

    @Test
    public void testConvertToDataModelTypeWithoutStatsWithStatsConverter() {
        studyEntry.getSamplesData().clear(); // TODO Samples can't be tested easily, needs a running Mongo instance
        List<String> sampleNames = null;
        // Test with a stats converter provided but no stats object
        DocumentToStudyVariantEntryConverter converter = new DocumentToStudyVariantEntryConverter(true, fileId, new
                DocumentToSamplesConverter(studyId, sampleNames, "0/0"));
        StudyEntry converted = converter.convertToDataModelType(mongoStudy);
        assertEquals(studyEntry, converted);
    }

    @Test
    public void testConvertToStorageTypeWithoutStats() {
        // Test with no stats converter provided
        DocumentToStudyVariantEntryConverter converter = new DocumentToStudyVariantEntryConverter(true, fileId,
                new DocumentToSamplesConverter(studyId, fileId, sampleNames, "0/0"));
        Document converted = converter.convertToStorageType(studyEntry);
        assertEquals(mongoStudy, converted);
    }

    @Test
    public void testConvertToStorageTypeWithoutStatsWithSampleIds() {
        DocumentToStudyVariantEntryConverter converter;
        Document convertedMongo;
        StudyEntry convertedFile;


        // Test with no stats converter provided
        DocumentToSamplesConverter samplesConverter = new DocumentToSamplesConverter(studyConfiguration);
        converter = new DocumentToStudyVariantEntryConverter(
                true, fileId,
                samplesConverter
        );
        convertedMongo = converter.convertToStorageType(studyEntry);
        assertEquals(mongoFileWithIds, convertedMongo);
        convertedFile = converter.convertToDataModelType(convertedMongo);
        assertEquals(studyEntry, convertedFile);

    }

    @Test
    public void testConvertToDataTypeWithoutStatsWithSampleIds() {
        DocumentToStudyVariantEntryConverter converter;
        Document convertedMongo;
        StudyEntry convertedFile;


        // Test with no stats converter provided
        DocumentToSamplesConverter samplesConverter = new DocumentToSamplesConverter(studyConfiguration);
        converter = new DocumentToStudyVariantEntryConverter(
                true, fileId,
                samplesConverter
        );
        convertedFile = converter.convertToDataModelType(mongoFileWithIds);
        convertedMongo = converter.convertToStorageType(convertedFile);
        assertEquals(studyEntry, convertedFile);
        assertEquals(mongoFileWithIds, convertedMongo);

    }

}
