package com.bbn.akbc.neolearnit.common;

import com.bbn.akbc.neolearnit.common.util.TabularPathListsConverter;
import com.bbn.bue.common.parameters.exceptions.MissingRequiredParameter;
import com.google.common.base.Optional;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class DocPathResolver {

    public static class Monolingual{
        public static Optional<File> getDocFileUsingCorpusName(String docid) {
            File docFile = null;
            String corpusName = null;
            try {
                corpusName = LearnItConfig.get("corpus_name");
            } catch (MissingRequiredParameter e) {
                return Optional.absent();
            }
            if (corpusName.equals("coldstart_cs2016_chinese_mini")) {
                docFile = new File(
                        "/nfs/mercury-04/u10/resources/KBP/CS/2016/corpus_chinese_mini/serifxmls/"
                                + docid + ".xml");
            } else if (corpusName.equals("coldstart_cs2015")) {
                docFile = new File(
                        "/nfs/mercury-04/u42/bmin/runjobs2/expts/experiments/2015CS_trail.v1.no_indoc_split.statRE/serifxml/"
                                + docid + ".xml");
                if (!docFile.exists())
                    docFile = new File(
                            "/nfs/mercury-04/u42/bmin/runjobs2/expts/experiments/2015CS_trail.v1.no_indoc_split.statRE/serifxml/"
                                    + docid + ".sgm.xml");
            } else if (corpusName.equals("coldstart_cs2015_mini")) {
                docFile = new File("/nfs/mercury-04/u42/bmin/runjobs2/expts/scripts/2015CS_test_for_2016.bbn2.v1.no_indoc_split.megaDefender.v12.serif20160706_mgCoref.new_actorDB.e2e.with_empty_namelist.df_bolt_parser/serifxml/"
                        + docid + ".mpdf.serifxml.xml");
                if (!docFile.exists()) {
                    docFile = new File("/nfs/mercury-04/u42/bmin/runjobs2/expts/scripts/2015CS_test_for_2016.bbn2.v1.no_indoc_split.megaDefender.v12.serif20160706_mgCoref.new_actorDB.e2e.with_empty_namelist.df_bolt_parser/serifxml/"
                            + docid + ".serifxml.xml");
                }
            } else if (corpusName.equals("coldstart_cs2014_mini")) {
                docFile = new File(
                        "/nfs/mercury-04/u10/resources/KBP/CS/2014/corpus/mini_corpus/data_for_learnit/serifxml/"
                                + docid + ".xml");
                if (!docFile.exists())
                    docFile = new File(
                            "/nfs/mercury-04/u10/resources/KBP/CS/2014/corpus/mini_corpus/data_for_learnit/serifxml/"
                                    + docid + ".sgm.xml");
                if (!docFile.exists())
                    docFile = new File(
                            "/nfs/mercury-04/u10/resources/KBP/CS/2014/corpus/mini_corpus/data_for_learnit/serifxml/"
                                    + docid + ".sgm.xml");
            } else if (corpusName.equals("coldstart_sf2014_mini")) {
                docFile = new File(
                        "/nfs/mercury-04/u10/resources/KBP/SF/2014/mini_corpus/data_for_learnit/serifxml/"
                                + docid + ".sgm.xml");
            } else if (corpusName.equals("coldstart_cs2013_mini")) {
                docFile = new File(
                        "/nfs/mercury-04/u10/resources/KBP/CS/2013/corpus/mini_corpus/data_for_learnit/serifxml/"
                                + docid + ".xml");
            } else if (corpusName.equals("coldstart_sf2013_mini")) {
                docFile = new File(
                        "/nfs/mercury-04/u10/resources/KBP/SF/2013/mini_corpus/data_for_learnit/serifxml/"
                                + docid + ".sgm.xml");
            } else if (corpusName.equals("coldstart_sf2012_mini")) {
                docFile = new File(
                        "/nfs/mercury-04/u10/resources/KBP/SF/2012/mini_corpus/data_for_learnit/serifxml/"
                                + docid + ".sgm.xml");
            } else if (corpusName.equals("ere_LDC2015E29_and_LDC2015E68")) {
                docFile = new File(
                        "/nfs/mercury-04/u41/learnit/ERE/serifxml/"
                                + docid + ".xml");
                if (!docFile.exists())
                    docFile = new File(
                            "/nfs/mercury-04/u41/learnit/ERE/serifxml/"
                                    + docid + ".mpdf.xml");
                if (!docFile.exists())
                    docFile = new File(
                            "/nfs/mercury-04/u41/learnit/ERE/serifxml/"
                                    + docid + ".cmp.txt.xml");
            } else if (corpusName.equals("ere_event_test_LDC2015E29_and_LDC2015E68")) {
                docFile = new File(
                        "/nfs/mercury-04/u41/learnit/ERE.withVerbsAsEventMentions/serifxml/"
                                + docid + ".xml");
            } else if (corpusName.equals("ere_LDC2015E29_DEFT_Rich_ERE_English_Training_Annotation_V1")) {
                docFile = new File(
                        "/nfs/mercury-04/u42/bmin/everything/projects/ere/data_learnit/serifxml/"
                                + docid + ".xml");
                if (!docFile.exists())
                    docFile = new File(
                            "/nfs/mercury-04/u42/bmin/everything/projects/ere/data_learnit/serifxml/"
                                    + docid + ".mpdf.xml");
            } else if (corpusName.equals("pdtb_v2")) {
                docFile = new File(
                        "/nfs/mercury-04/u41/learnit/pdtb_v2/serifxml/"
                                + docid + ".xml");
            } else if (corpusName.equals("pdtb_v2.withVerbsAsEventMentions")) {
                docFile = new File(
                        "/nfs/mercury-04/u41/learnit/pdtb_v2.withVerbsAsEventMentions/serifxml/"
                                + docid + ".xml");
            } else if (corpusName.equals("red.eventFromAnnotation")) {
                docFile = new File(
                        "/nfs/mercury-04/u41/learnit/RED.eventFromAnnotation/serifxml/"
                                + docid + ".xml");
            } else if (corpusName.equals("wm_starter")) {
                docFile = new File(
                        "/nfs/mercury-04/u41/learnit/WM_starter/serifxml/"
                                + docid + ".xml");
            } else if (corpusName.equals("causeex-m5")) {
                docFile = new File(
                        "/nfs/mercury-04/u41/learnit/CauseEx-M5/serifxml/"
                                + docid + ".xml");
            } else if (corpusName.equals("causeex-m5-pos")) {
                docFile = new File(
                        "/nfs/mercury-04/u41/learnit/CauseEx-M5.add_verbs_and_nouns/serifxml/"
                                + docid + ".xml");
            } else if (corpusName.equals("wm_m6")) {
                docFile = new File(
                        "/nfs/mercury-04/u41/learnit/WM_m6/serifxml/"
                                + docid + ".serifxml");
            } else if (LearnItConfig.get("corpus_name")
                    .equals("causeex-m9")) {
                docFile = new File(
                        "/nfs/mercury-04/u42/bmin/projects/CauseEx/M9_assessment/relations/learnit/causeex_m9.add_verbs_nouns/serifxml/"
                                + docid + ".serifxml");
                if (!docFile.exists()) {
                    docFile = new File(
                            "/nfs/mercury-04/u42/bmin/projects/CauseEx/M9_assessment/relations/learnit/causeex_m9.add_verbs_nouns/serifxml/"
                                    + docid + ".xml");
                }
            }
            return Optional.fromNullable(docFile);
        }

        public static String KBPCS2016(String docid){
            return new File("/nfs/mercury-04/u10/resources/KBP/CS/2016/corpus_chinese_mini/serifxmls/" // by default, 2016cs chinese mini corpus
                    + docid + ".xml").toString();
        }

        public static String getPath(String docId) throws IOException {
            return Epoch.getSerifPathFromEpoch(docId,Epoch.getFocusEpoch());
//            return SourceListsReader.getFullPath(docId);
        }

    }

    public static class Bilingual{

        public static Map<String,String> getPath(String docId) throws IOException {
            Map<String, String> biEntries = TabularPathListsConverter.getPathEntries(docId);
            return biEntries;
        }
    }

}
