package com.bbn.akbc.neolearnit.util;

import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.*;
import com.bbn.serif.theories.actors.ActorEntity;
import com.bbn.serif.types.EntityType;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import static com.bbn.serif.theories.Proposition.Argument.SUB_ROLE;

public class PrintOutAllSubsOfProposition {

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

    public static void main(String[] args)throws Exception{
        List<String> inputListPaths = GeneralUtils.readLinesIntoList(args[0]);
        String outputDir = args[1];
        SerifXMLLoader serifXMLLoader = SerifXMLLoader.builder().build();
        for(String serifPath:inputListPaths){
            DocTheory docTheory = serifXMLLoader.loadFrom(new File(serifPath));
            File outputPath = new File(outputDir+File.separator+docTheory.docid().asString()+".dump");
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputPath));
            for(SentenceTheory st:docTheory.nonEmptySentenceTheories()){
                for(Proposition proposition:st.propositions()){
                    for(Proposition.Argument argument:proposition.args()){
                        if(argument.roleIs(SUB_ROLE) && (argument instanceof Proposition.MentionArgument)){
                            Proposition.MentionArgument mentionArgument = (Proposition.MentionArgument)argument;
                            Mention mention = mentionArgument.mention();
                            if(mention.entityType().equals(EntityType.ORG) || mention.entityType().equals(EntityType.GPE)){
                                String canonicalName= getCanonicalName(mention,docTheory);
                                bufferedWriter.write(canonicalName+"\n");
                            }
                        }
                    }
                }
            }
            bufferedWriter.close();
        }
    }
}
