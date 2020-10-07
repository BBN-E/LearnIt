package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.mappings.filters.RelevantInstancesFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.storage.impl.HashMapStorage;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.actors.ActorEntity;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FilterMappingsThatDontHaveCertainEntityMentionInInstances {
    public static String getCanonicalName(Mention mention, DocTheory docTheory){
        Optional<String> argCanonicalNameOptional = Optional.absent();
        Optional<Entity> entityOptional = mention.entity(docTheory);
        if(entityOptional.isPresent()) {
            ImmutableSet<ActorEntity> actorEntities = docTheory.actorEntities().forEntity(entityOptional.get());
            if(!actorEntities.isEmpty()) {
                ActorEntity actorEntity = actorEntities.iterator().next();
                argCanonicalNameOptional = Optional.of(actorEntity.actorName().asString());
            }
            else if(entityOptional.get().representativeName().isPresent()){
                argCanonicalNameOptional = Optional.of(entityOptional.get().representativeName().get().mention().span().tokenizedText().utf16CodeUnits());
            }
        }
        return argCanonicalNameOptional.isPresent()?argCanonicalNameOptional.get():mention.span().tokenizedText().utf16CodeUnits();
    }
    public static void main(String[] args) throws Exception{
        String learnitConfigPath = args[0];
        LearnItConfig.loadParams(new File(learnitConfigPath));

        Mappings mappings = Mappings.deserialize(new File(args[1]),true);
        List<String> buf = GeneralUtils.readLinesIntoList(args[2]);
        Set<String> requiredCanonicalNames = new HashSet<>();
        for(String canonicalName:buf){
            requiredCanonicalNames.add(canonicalName.trim());
        }
        Set<LearnitPattern> goodPatterns = new HashSet<>();

        for(LearnitPattern learnitPattern: mappings.getAllPatterns().elementSet()){

//            InstanceIdentifier.preLoadDocThoery(mappings.getInstancesForPattern(learnitPattern));
            boolean shouldKeepPattern = false;
            for(InstanceIdentifier instanceIdentifier: mappings.getInstancesForPattern(learnitPattern)){
                MatchInfo matchInfo = instanceIdentifier.reconstructMatchInfo(TargetFactory.makeUnaryEventTarget());
                for(Mention mention : matchInfo.getPrimaryLanguageMatch().getSentTheory().mentions()){
                    String canonicalName = getCanonicalName(mention,matchInfo.getPrimaryLanguageMatch().getDocTheory());
                    if(requiredCanonicalNames.contains(canonicalName)){
                        System.out.println("[AAAA]"+canonicalName);
                        goodPatterns.add(learnitPattern);
                        shouldKeepPattern = true;
                        break;
                    }
                }
                if(shouldKeepPattern){
                    break;
                }
            }
        }

        HashMapStorage.Builder<InstanceIdentifier, LearnitPattern> i2p = new HashMapStorage.Builder<>();
        for(LearnitPattern learnitPattern:goodPatterns){
            for(InstanceIdentifier instanceIdentifier: mappings.getInstancesForPattern(learnitPattern)){
                i2p.put(instanceIdentifier, learnitPattern);
            }
        }
        Mappings newMappings = new Mappings(mappings.getInstance2Seed().getStorage(), i2p.build());
        RelevantInstancesFilter relevantInstancesFilter = new RelevantInstancesFilter();
        newMappings = relevantInstancesFilter.makeFiltered(newMappings);
        newMappings.serialize(new File(args[3]),true);
    }
}
