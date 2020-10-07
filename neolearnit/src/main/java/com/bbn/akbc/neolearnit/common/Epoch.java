package com.bbn.akbc.neolearnit.common;

import com.bbn.akbc.neolearnit.mappings.filters.TargetFilterWithCache;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.similarity.ObservationSimilarityModule;
import com.bbn.akbc.neolearnit.util.GeneralUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Epoch {

    public static int getCurrentEpoch(){
        int ret = 0;
        if(new File(LearnItConfig.get("learnit_data_root")).exists()){
            for(File file : new File(LearnItConfig.get("learnit_data_root")).listFiles()){
                File lockFile = new File(file.toString() + File.separator+"lock");
                if(!lockFile.exists()){
                    int newEpoch = Integer.parseInt(file.getName());
                    ret = Math.max(newEpoch,ret);
                }
            }
        }
        return ret;
    }
    static int focusEpoch = 0;
    public static void setFocusEpoch(int epoch){
        focusEpoch = epoch;
    }
    public static int getFocusEpoch(){
        return focusEpoch;
    }

    public static File getMappingsPathForTarget(String target,int epoch){
        return new File(LearnItConfig.get("learnit_data_root")+File.separator+epoch+File.separator+"mappings" + File.separator +target+File.separator+"mappings.master.sjson");
    }

    public static File getSimilarPathForTarget(String target,int epoch){
        return new File(LearnItConfig.get("learnit_data_root")+File.separator+epoch+File.separator+"similarity" + File.separator +target);

    }

    static Map<Integer, Map<String,String>> epochSerifPathCache = new HashMap<>();

    public static String getSerifPathFromEpoch(String docId, int epoch) throws IOException {
        if(!epochSerifPathCache.containsKey(epoch)){
            String epochRoot = LearnItConfig.get("learnit_data_root")+File.separator+epoch+File.separator+"decoding"+File.separator+"serifxmls.list";
            Map<String,String> docIdToPath = new HashMap<>();
            if(epoch<1){
                epochRoot = LearnItConfig.get("bootstrap_serifxml_list");
            }
            for(String line: GeneralUtils.readLinesIntoList(epochRoot)){
                String docIdPending = line.substring(line.lastIndexOf(File.separator)+1,line.lastIndexOf("."));
                docIdToPath.put(docIdPending,line);
            }
            epochSerifPathCache.put(epoch,docIdToPath);

        }
        return epochSerifPathCache.get(epoch).getOrDefault(docId,null);
    }

    public static class MappingsEntryUnderEpoch{
        Mappings mappings;
        int epoch;
        TargetFilterWithCache targetFilterWithCache;
        ObservationSimilarityModule observationSimilarityModule;
        public MappingsEntryUnderEpoch(Mappings mappings,int epoch,TargetFilterWithCache targetFilterWithCache,ObservationSimilarityModule observationSimilarityModule){
            this.mappings = mappings;
            this.epoch = epoch;
            this.targetFilterWithCache = targetFilterWithCache;
            this.observationSimilarityModule = observationSimilarityModule;
        }
        public Mappings getMappings(){
            return this.mappings;
        }
        public int getEpoch(){
            return this.epoch;
        }
        public TargetFilterWithCache getTargetFilterWithCache(){
            return this.targetFilterWithCache;
        }
        public ObservationSimilarityModule getObservationSimilarityModule(){
            return this.observationSimilarityModule;
        }
    }

    public static File getMappingsPath(String targetName, int epoch){
        String convertedTargetName = Domain.getConvertedTargetName(targetName);
        return getMappingsPathForTarget(convertedTargetName,epoch);
    }

    public static File getSimilarityPath(String targetName, int epoch){
        String convertedTargetName = Domain.getConvertedTargetName(targetName);
        return getSimilarPathForTarget(convertedTargetName,epoch);
    }
}
