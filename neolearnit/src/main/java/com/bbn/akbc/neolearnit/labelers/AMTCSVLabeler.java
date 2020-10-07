package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.google.common.base.Optional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;

@Deprecated
public class AMTCSVLabeler implements MappingsLabeler {
    final String strInputCsvFile;

    AMTCSVLabeler(String strInputCsvFile) {
        this.strInputCsvFile = strInputCsvFile;
    }

    @Override
    public Annotation.InMemoryAnnotationStorage LabelMappings(Mappings original, Annotation.InMemoryAnnotationStorage labeledMappings) throws Exception {
        // TODO: Implement this for current CSV version
        // Please implement a CSV parser here
        // Please support CSV in batch mode as in python
        Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorage = new Annotation.InMemoryAnnotationStorage();
        CSVParser instanceIdentifierInfoParser = CSVParser.parse(new FileReader(strInputCsvFile), CSVFormat.DEFAULT);
        for (CSVRecord csvRecord : instanceIdentifierInfoParser) {
            if (csvRecord.get(0).equals("docId")) continue;
            final String docId = csvRecord.get(0);
            final int sentId = Integer.parseInt(csvRecord.get(1));
            final int slot0Start = Integer.parseInt(csvRecord.get(2));
            final int slot0End = Integer.parseInt(csvRecord.get(3));
            final int slot1Start = Integer.parseInt(csvRecord.get(4));
            final int slot1End = Integer.parseInt(csvRecord.get(5));
            boolean causalRelationExist = false;
            for (int i = 7; i <= 11; ++i) {
                try {
                    int buf = Integer.parseInt(csvRecord.get(i));
                    if (buf == 5) {
                        causalRelationExist = true;
                        break;
                    }
                } catch (NumberFormatException e) {
                    continue;
                }
            }
            if (!causalRelationExist) continue;
            // TODO: Implement this for current CSV version
            // @hqiu: This is dummy code. "generic" is dummy and LabelPattern is dummy.
            InstanceIdentifier originalInstanceIdentifier = new InstanceIdentifier(
                    docId,
                    sentId,
                    slot0Start,
                    slot0End,
                    InstanceIdentifier.SpanningType.EventMention,
                    Optional.absent(),
                    "generic",
                    false,
                    slot1Start, slot1End, InstanceIdentifier.SpanningType.EventMention,
                    Optional.absent(),
                    "generic",
                    false);
            inMemoryAnnotationStorage.addAnnotation(originalInstanceIdentifier, new LabelPattern("Has_Causal_Relation", Annotation.FrozenState.FROZEN_GOOD));
        }
        return inMemoryAnnotationStorage;
    }
}
