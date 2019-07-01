package com.bbn.akbc.neolearnit.serializers;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.util.SourceListsReader;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ExternalAnnotationBuilder {

    protected final Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorage;

    protected ExternalAnnotationBuilder() {
        this.inMemoryAnnotationStorage = new Annotation.InMemoryAnnotationStorage();
    }

    protected static String getDocPath(String docId) throws Exception {
        return new File(SourceListsReader.getFullPath(docId)).getAbsolutePath();
    }

    final public void observe(Mappings mappings) {
        this.inMemoryAnnotationStorage.MergeOther(new Annotation.InMemoryAnnotationStorage(mappings));
    }

    protected Map<InstanceIdentifier, DocTheory> resolveDocTheory() throws Exception {
        Map<InstanceIdentifier, DocTheory> ret = new ConcurrentHashMap<>();
        Map<Symbol, Set<InstanceIdentifier>> docPathToInstanceIdetifiers = new HashMap<>();
        for (InstanceIdentifier instanceIdentifier : this.inMemoryAnnotationStorage.getAllInstanceIdentifier()) {
            Symbol docPath = Symbol.from(new File(SourceListsReader.getFullPath(instanceIdentifier.getDocid())).getAbsolutePath());
            Set<InstanceIdentifier> buf = docPathToInstanceIdetifiers.getOrDefault(docPath, new HashSet<>());
            buf.add(instanceIdentifier);
            docPathToInstanceIdetifiers.put(docPath, buf);
        }
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (Symbol docPath : docPathToInstanceIdetifiers.keySet()) {
            tasks.add(new DocTheorySingleDocumentWorker(docPath, docPathToInstanceIdetifiers.get(docPath), ret));
        }
        GeneralUtils.GeneralDocBasedWorkerScheduler(tasks);
        return ret;
    }

    protected Map<InstanceIdentifier, SentenceTheory> resolveSentenceTheory() throws Exception {
        Map<InstanceIdentifier, SentenceTheory> ret = new ConcurrentHashMap<>();
        Map<Symbol, Set<InstanceIdentifier>> docPathToInstanceIdetifiers = new HashMap<>();
        for (InstanceIdentifier instanceIdentifier : this.inMemoryAnnotationStorage.getAllInstanceIdentifier()) {
            Symbol docPath = Symbol.from(new File(SourceListsReader.getFullPath(instanceIdentifier.getDocid())).getAbsolutePath());
            Set<InstanceIdentifier> buf = docPathToInstanceIdetifiers.getOrDefault(docPath, new HashSet<>());
            buf.add(instanceIdentifier);
            docPathToInstanceIdetifiers.put(docPath, buf);
        }
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (Symbol docPath : docPathToInstanceIdetifiers.keySet()) {
            tasks.add(new SentenceTheorySingleDocumentWorker(docPath, docPathToInstanceIdetifiers.get(docPath), ret));
        }
        GeneralUtils.GeneralDocBasedWorkerScheduler(tasks);
        return ret;
    }

    public abstract void build() throws Exception;

    public static class SentenceTheorySingleDocumentWorker implements Callable<Boolean> {
        final Symbol docPath;
        final Set<InstanceIdentifier> instanceIdentifiers;
        final Map<InstanceIdentifier, SentenceTheory> instanceIdentifierSentenceTheoryMap;

        public SentenceTheorySingleDocumentWorker(Symbol docPath, Set<InstanceIdentifier> instanceIdentifiers, Map<InstanceIdentifier, SentenceTheory> instanceIdentifierSentenceTheoryMap) {
            this.docPath = docPath;
            this.instanceIdentifiers = instanceIdentifiers;
            this.instanceIdentifierSentenceTheoryMap = instanceIdentifierSentenceTheoryMap;
        }


        @Override
        public Boolean call() throws Exception {
            final SerifXMLLoader serifxmlLoader =
                    LearnItConfig.params().getOptionalBoolean("load_serifxml_with_sloppy_offsets").or(false) ?
                            new SerifXMLLoader.Builder().allowSloppyOffsets().build() :
                            SerifXMLLoader.createFrom(LearnItConfig.params());
            DocTheory docTheory = serifxmlLoader.loadFrom(new File(docPath.asString()));
            for (InstanceIdentifier instanceIdentifier : instanceIdentifiers) {
                SentenceTheory sentenceTheory = docTheory.sentenceTheory(instanceIdentifier.getSentid());
                this.instanceIdentifierSentenceTheoryMap.put(instanceIdentifier, sentenceTheory);
            }
            return true;
        }
    }

    public static class DocTheorySingleDocumentWorker implements Callable<Boolean> {
        final Symbol docPath;
        final Set<InstanceIdentifier> instanceIdentifiers;
        final Map<InstanceIdentifier, DocTheory> instanceIdentifierSentenceTheoryMap;

        public DocTheorySingleDocumentWorker(Symbol docPath, Set<InstanceIdentifier> instanceIdentifiers, Map<InstanceIdentifier, DocTheory> instanceIdentifierSentenceTheoryMap) {
            this.docPath = docPath;
            this.instanceIdentifiers = instanceIdentifiers;
            this.instanceIdentifierSentenceTheoryMap = instanceIdentifierSentenceTheoryMap;
        }


        @Override
        public Boolean call() throws Exception {
            final SerifXMLLoader serifxmlLoader =
                    LearnItConfig.params().getOptionalBoolean("load_serifxml_with_sloppy_offsets").or(false) ?
                            new SerifXMLLoader.Builder().allowSloppyOffsets().build() :
                            SerifXMLLoader.createFrom(LearnItConfig.params());
            DocTheory docTheory = serifxmlLoader.loadFrom(new File(docPath.asString()));
            for (InstanceIdentifier instanceIdentifier : instanceIdentifiers) {
                this.instanceIdentifierSentenceTheoryMap.put(instanceIdentifier, docTheory);
            }
            return true;
        }
    }
}
