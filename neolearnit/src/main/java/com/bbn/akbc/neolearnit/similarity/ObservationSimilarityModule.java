package com.bbn.akbc.neolearnit.similarity;


import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.bbn.akbc.neolearnit.observations.similarity.*;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;

import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.utility.Pair;
import com.google.common.base.Optional;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

// import SubsamplingFilter;

public class ObservationSimilarityModule {

  private final Mappings mappings;

  private final Set<Seed> seedsInMappings;
  private final Map<PatternID,LearnitPattern> patternsInMappings;

  private Map<SeedPatternPair,Optional<SeedPatternPairSimilarity>> seedPatternPairSimilarityCache = new HashMap<>();

  //This map ensures that we don't unnecessarily load similarity matrices from the same mappings object again and again
  private static Map<Mappings,ObservationSimilarityModule> mappingsToSimModule = new HashMap<>();
  /*
    Retiring old way of doing lazy-lookup in favor of loading the similarity matrices at the beginning.
    We will still keep the above cache maps around.
   */
  private Map<Seed, SeedSimilarity> seedSimilarityMap = new HashMap<>();
  private Map<PatternID, PatternSimilarity> patternSimilarityMap = new HashMap<>();

  private final String seedSimilarityDir;
  private final String patternSimilarityDir;

  private final boolean honorSlotOrderingForSeeds;





//  public static void main(String[] args) throws IOException{
//    String paramFile = "/nfs/raid87/u15/users/msrivast/learnit/params/learnit/runs/red.eventFromAnnotation.params";
//    LearnItConfig.loadParams(new File(paramFile));
//    ObservationSimilarityModule module = ObservationSimilarityModule.create(
//            "everything",false);
//    module.getSeedSimilarity(Seed.from("english","get","attention"));
//    module.getSeedSimilarity(Seed.from("english","dummy","dummy"));
//    module.getPatternSimilarity(PatternID.from("{0} , according to {1}"));
//    module.getPatternSimilarity(PatternID.from("dummy"));
//  }

  private ObservationSimilarityModule(Mappings mappings, String suffix, boolean honorSlotOrderingForSeeds,
                                      Set<Seed> seedsInMappings, Map<PatternID,LearnitPattern> patternsInMappings) {
    this.mappings = mappings;

    // The following convention for similarity dir names is derived from the learnit sequence.
    String seedSimDirSuffix = suffix+File.separator+"min_freq_"+
            LearnItConfig.get("min_seed_frequency_per_batch")+"_"+LearnItConfig.get("seed_similarity_threshold");
    String patternSimDirSuffix = suffix+File.separator+"min_freq_"+
            LearnItConfig.get("min_pattern_frequency_per_batch")+"_"+LearnItConfig.get("pattern_similarity_threshold");

    this.seedSimilarityDir = LearnItConfig.get("seed_similarity_dir")+File.separator+seedSimDirSuffix;
    this.patternSimilarityDir = LearnItConfig.get("pattern_similarity_dir")+File.separator+patternSimDirSuffix;
    this.honorSlotOrderingForSeeds = honorSlotOrderingForSeeds;
    this.seedsInMappings = seedsInMappings;
    this.patternsInMappings = patternsInMappings;
  }

  public static ObservationSimilarityModule create(Mappings mappings, String suffix) throws IOException{
    if (mappingsToSimModule.get(mappings)!=null){
      System.out.println("Returning similarity module created for mappings object "+mappings.toString()+"....");
      return mappingsToSimModule.get(mappings);
    }
    //true by default
    boolean honorSlotOrderingForSeeds =
            !LearnItConfig.defined("honor_slot_ordering_for_seed_extraction") ||
                    LearnItConfig.optionalParamTrue("honor_slot_ordering_for_seed_extraction");

    ObservationSimilarityModule module = new ObservationSimilarityModule(mappings,suffix,honorSlotOrderingForSeeds,
            getAllSeedsFromMappings(mappings),getAllPatternsFromMappings(mappings));
    System.out.println("Loading seed-similarity matrix for mappings object "+mappings.toString()+"...");
    module.loadSeedSimilarityMatrices();
    System.out.println("Loading pattern-similarity matrix for mappings object "+mappings.toString()+"...");
    module.loadPatternSimilarityMatrices();
    mappingsToSimModule.put(mappings,module);
    return module;
  }

  private static Set<Seed> getAllSeedsFromMappings(Mappings mappings){
    return mappings.getAllSeeds().elementSet().stream().map(
            (Seed s)-> (
                    (s.withProperText(true,true))
            )).collect(Collectors.toSet());
  }

  private static Map<PatternID,LearnitPattern> getAllPatternsFromMappings(Mappings mappings){
    Map<PatternID,LearnitPattern> patternMap = new HashMap<>();
    for (LearnitPattern pattern : mappings.getAllPatterns().elementSet()){
      patternMap.put(PatternID.from(pattern),pattern);
    }
    return patternMap;
  }

  public Optional<SeedSimilarity> getSeedSimilarity(Seed seed) throws IOException{
    SeedSimilarity seedSim = seedSimilarityMap.get(seed);
    Optional<SeedSimilarity> value = Optional.fromNullable(seedSim);
    return value;
  }

  public Optional<PatternSimilarity> getPatternSimilarity(PatternID pattern) throws IOException{
    PatternSimilarity patternSim = patternSimilarityMap.get(pattern);
    Optional<PatternSimilarity> value = Optional.fromNullable(patternSim);
    return value;
  }

  public Optional<SeedPatternPairSimilarity> getSeedPatternPairSimilarity(SeedPatternPair seedPatternPair) throws IOException{
    if(this.seedPatternPairSimilarityCache.containsKey(seedPatternPair)){
      return this.seedPatternPairSimilarityCache.get(seedPatternPair);
    }
    Optional<SeedSimilarity> seedSimilarity = getSeedSimilarity(seedPatternPair.seed());
    Optional<PatternSimilarity> patternSimilarity = getPatternSimilarity(seedPatternPair.pattern());
    Optional<SeedPatternPairSimilarity> value = Optional.absent();
    if(seedSimilarity.isPresent()&& patternSimilarity.isPresent()){
      Map<SeedPatternPair,Double> similarObsMap = new HashMap<>();
      for(Pair<? extends LearnItObservation,Double> similarSeedObs : seedSimilarity.get().getSimilarObservationsAsSortedList()){
        Seed seed = (Seed)similarSeedObs.key;
        double seedSimScore = similarSeedObs.value;
        for(Pair<? extends LearnItObservation,Double> similarPatternObs : patternSimilarity.get().getSimilarObservationsAsSortedList()){
          PatternID pattern = (PatternID)similarPatternObs.key;
          double patternSimScore = similarPatternObs.value;
          similarObsMap.put(SeedPatternPair.create(seed,pattern),seedSimScore*patternSimScore);
        }
      }
      value = Optional.of(SeedPatternPairSimilarity.create(seedPatternPair,similarObsMap));
    }
    this.seedPatternPairSimilarityCache.put(seedPatternPair,value);
    return value;
  }

  private File[] loadSimilarityFiles(String simDir) {
    return (new File(simDir)).listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return pathname.isFile()&&pathname.getName().endsWith(".gz");
      }
    });
  }

  private List<Seed> getSeedsFromMappings(Seed seed, boolean honorSlotOrderingForSeeds){

    List<Seed> seedsToReturn = new ArrayList<>();

    if (this.seedsInMappings.contains(seed)){
      seedsToReturn.add(seed);
    }
    if (this.seedsInMappings.contains(seed.reversed())){
      seedsToReturn.add(seed.reversed());
    }
    return seedsToReturn;
  }

  private Optional<LearnitPattern> getPatternFromMappings(String patternIDString){
    PatternID patternID = PatternID.from(patternIDString);
    return Optional.fromNullable(this.patternsInMappings.get(patternID));
  }

  private void loadSeedSimilarityMatrices() throws IOException{
    for(File simFile : loadSimilarityFiles(this.seedSimilarityDir)){
        BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(simFile))));
        String line = null;
        int count = 0;
        while((line = br.readLine())!=null){
            count++;
            if (count%1000==0){
              System.out.println("Read "+count+" seeds so far...");
            }
            List<String> tokens = Arrays.asList(line.split("\t"));
            //TODO: Hardcoding of language should be removed
            Seed seed = Seed.from(LearnItConfig.getList("languages").get(0),tokens.get(0),tokens.get(1));
            List<Seed> seedsFromMappings = getSeedsFromMappings(seed,this.honorSlotOrderingForSeeds);
            Map<Seed, Double> similarSeedObservations = new HashMap<>();
            List<String> similarityTokens = new ArrayList<>();
            tokens = tokens.subList(2,tokens.size());
            for(int i=0;i<tokens.size();i++){
              if(i!=0 && (i%3 == 0 || i==tokens.size()-1)){
                if(i==tokens.size()-1){
                  similarityTokens.add(tokens.get(i));
                }
                Seed simSeed = Seed.from(seed.getLanguage(),similarityTokens.get(0),similarityTokens.get(1));
                List<Seed> similarSeeds = getSeedsFromMappings(simSeed,this.honorSlotOrderingForSeeds);
                double score = Double.parseDouble(similarityTokens.get(2));
                for(Seed similarSeed : similarSeeds) {
                  similarSeedObservations.put(similarSeed, score);
                }
                similarityTokens.clear();
              }
              similarityTokens.add(tokens.get(i));
            }
            for (Seed seedFromMappings : seedsFromMappings) {
              SeedSimilarity seedSimilarity = SeedSimilarity.create(seedFromMappings, similarSeedObservations);
              seedSimilarityMap.put(seedFromMappings, seedSimilarity);
            }
        }
    }
    //After similarity matrix have been loaded, clear up seedsInMappings
    this.seedsInMappings.clear();
  }

  private void loadPatternSimilarityMatrices() throws IOException{
    for(File simFile : loadSimilarityFiles(this.patternSimilarityDir)){
      BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(simFile))));
      String line = null;
      int count = 0;
      while((line = br.readLine())!=null){
        count++;
        if (count%1000==0){
          System.out.println("Read "+count+" seeds so far...");
        }
        List<String> tokens = Arrays.asList(line.split("\t"));
        LearnitPattern learnitPattern = getPatternFromMappings(tokens.get(0)).orNull();
        if (learnitPattern == null){ //if pattern is not found in mappings, continue
          continue;
        }
        PatternID pattern = PatternID.from(learnitPattern);
        Map<PatternID, Double> similarPatternObservations = new HashMap<>();
        List<String> similarityTokens = new ArrayList<>();
        tokens = tokens.subList(1,tokens.size());
        for(int i=0;i<tokens.size();i++){
          if(i!=0 && (i%2 == 0 || i==tokens.size()-1)){
            if(i==tokens.size()-1){
              similarityTokens.add(tokens.get(i));
            }
            LearnitPattern similarityPattern = getPatternFromMappings(similarityTokens.get(0)).orNull();
            if (similarityPattern == null){ //if similar pattern is not found in mappings, continue
              continue;
            }
            PatternID similarPattern = PatternID.from(similarityPattern);
            double score = Double.parseDouble(similarityTokens.get(1));
            similarPatternObservations.put(similarPattern,score);
            similarityTokens.clear();
          }
          similarityTokens.add(tokens.get(i));
        }
        PatternSimilarity patternSimilarity = PatternSimilarity.create(pattern,similarPatternObservations);
        patternSimilarityMap.put(pattern,patternSimilarity);
       }
    }
    //After similarity matrix have been loaded, clear up patternInMappings
    this.patternsInMappings.clear();
  }


//  private ObservationSimilarity getObservationSimilarity(LearnItObservation observation, String obsDir, String simDir) throws IOException{
//    boolean isSeed = observation instanceof Seed;
//    //look for the observation in the .tsv files in obsDir
//    for(String obsFileName : (new File(obsDir)).list()){
//      if(!obsFileName.endsWith(".tsv")){
//        continue;
//      }
//      List<String> lines = FileUtil.readLinesIntoList(new File(obsDir+File.separator+obsFileName));
//      List<LearnItObservation> matchingObservations = FluentIterable.from(lines).transform((String line)->{
//        {
//          String[] tokens = line.split("\t");
//          if(isSeed){
//            return Seed.from(((Seed)observation).getLanguage(),tokens[0],tokens[1]);
//          }
//          return PatternID.from(tokens[1]);
//        }
//      }).filter((LearnItObservation observation1)-> {
//          if(isSeed){
//            Seed observationWithReversedSlots = ((Seed)observation).makeSymmetric();
//            return observation.equals(observation1)||observationWithReversedSlots.equals(observation1);
//          }
//          return observation.equals(observation1);
//        }
//      ).toList();
//      if(!matchingObservations.isEmpty()){
//        String similarityFilePath = simDir+File.separator+obsFileName+".gz";
//        BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(similarityFilePath))));
//        String line = null;
//        String lookupString = isSeed?((Seed)observation).getSlot(0).asString()+"\t"+
//                ((Seed)observation).getSlot(1).asString():((PatternID)observation).getPatternIDString();
//        String alternativeLookupString = isSeed?((Seed)observation).getSlot(1).asString()+"\t"+
//                ((Seed)observation).getSlot(0).asString():((PatternID)observation).getPatternIDString();
//        while((line = br.readLine())!=null){
//          if(line.startsWith(lookupString)||line.startsWith(alternativeLookupString)){
//            String finalLookupString = line.startsWith(lookupString)?lookupString:alternativeLookupString;
//            line = line.substring(finalLookupString.length()+1);
//            String[] tokens = line.split("\t");
//            Map<Seed, Double> similarSeedObservations = new HashMap<>();
//            Map<PatternID, Double> similarPatternObservations = new HashMap<>();
//            int resetLength = isSeed?3:2;
//            List<String> similarityTokens = new ArrayList<>();
//            for(int i=0;i<tokens.length;i++){
//              if((i%resetLength == 0 || i==tokens.length-1)&&i!=0){
//                if(i==tokens.length-1){
//                  similarityTokens.add(tokens[i]);
//                }
//                if(isSeed){
//                  Seed similarSeed = Seed.from(((Seed)observation).getLanguage(),similarityTokens.get(0),similarityTokens.get(1));
//                  double score = Double.parseDouble(similarityTokens.get(2));
//                  similarSeedObservations.put(similarSeed,score);
//                }else{
//                  PatternID similarPattern = PatternID.from(similarityTokens.get(0));
//                  double score = Double.parseDouble(similarityTokens.get(1));
//                  similarPatternObservations.put(similarPattern,score);
//                }
//                similarityTokens.clear();
//              }
//              similarityTokens.add(tokens[i]);
//            }
//            if(isSeed){
//              return SeedSimilarity.create((Seed)observation,similarSeedObservations);
//            }else{
//              return PatternSimilarity.create((PatternID) observation,similarPatternObservations);
//            }
//          }
//        }
//      }
//    }
//    return null;
//  }


}
