package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.loaders.LoaderUtils;
import com.bbn.akbc.neolearnit.loaders.impl.MonolingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.mappings.filters.InstanceIdentifierFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.modules.MonolingualExtractionModule;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.tables.PatternScoreTable;
import com.bbn.akbc.neolearnit.serializers.observations.CausealJson;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Spanning;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ServerModeDecoder {

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
    public static final class SingleDocumentDecoder {

        // I think extractors could be not final because it will enable us to modify the extractors on the fly.
        private Map<String,TargetAndScoreTables> extractors;
        private final Target target;

        public SingleDocumentDecoder(Target target,String extractorDirectory) throws IOException{
            this.extractors = GeneralUtils.loadExtractors(extractorDirectory);
            this.target = checkNotNull(target);
        }

        public final Pair<DocTheory,Mappings> LoadDocument(final String serifXMLString) throws IOException, ExecutionException, InterruptedException {
            final SerifXMLLoader serifxmlLoader =
                    LearnItConfig.params().getOptionalBoolean("load_serifxml_with_sloppy_offsets").or(false)?
                            new SerifXMLLoader.Builder().allowSloppyOffsets().build():
                            SerifXMLLoader.createFrom(LearnItConfig.params());
            final DocTheory docTheory = serifxmlLoader.loadFromString(serifXMLString);
            MonolingualExtractionModule module = new MonolingualExtractionModule(false);
            MonolingualDocTheoryInstanceLoader docTheoryLoader = module.getDocTheoryLoader(this.target);
            List<DocTheory> docTheoryList = new ArrayList<>();
            docTheoryList.add(docTheory);
            LoaderUtils.loadDocTheroyList(docTheoryList,docTheoryLoader);
            return new Pair<>(docTheory,module.getInformationForScoring());
        }

        public final List<CausealJson> ConvertRelationsToJson(Pair<DocTheory, Mappings> preprocessedData) throws IOException {
            final DocTheory docTheory = preprocessedData.getFirst();
            final Mappings mappings = new InstanceIdentifierFilter().makeFiltered(preprocessedData.getSecond());
//        List<Pair<ImmutableList,String>> existingInstances = new ArrayList<>();
            Set<Pair<InstanceIdentifier,String>> existingInstances = new HashSet<>();
            for(String relationName : extractors.keySet()){
                final PatternScoreTable patternScoreTable = extractors.get(relationName).getPatternScores();
                for(LearnitPattern learnitPattern: patternScoreTable.getFrozen()){
                    if(patternScoreTable.getScore(learnitPattern).isGood()){
                        for(InstanceIdentifier instanceIdentifier:mappings.getInstancesForPattern(learnitPattern)){
                            existingInstances.add(new Pair<>(instanceIdentifier,relationName));
                        }
                    }
                }
            }
            List<CausealJson> ret = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();
            for(Pair<InstanceIdentifier,String> instance : existingInstances){
                // getFirst for de-duplicated sentId. getSecond for relationType
                final InstanceIdentifier instanceIdentifier = instance.getFirst();
                final String docId = instanceIdentifier.getDocid();
                final int sentId = instanceIdentifier.getSentid();
//            int slot0Start = instanceIdentifier.getSlot0Start();
//            int slot0End = instanceIdentifier.getSlot0End();
//            int slot1Start = instanceIdentifier.getSlot1Start();
//            int slot1End = instanceIdentifier.getSlot1End();
                final MatchInfo.LanguageMatchInfo languageMatchInfo = instanceIdentifier.reconstructMatchInfo(this.target).getPrimaryLanguageMatch();
                final Spanning span1 = languageMatchInfo.getSlot0().get();
                final Spanning span2 = languageMatchInfo.getSlot1().get();
                final String span1Text = span1.span().tokenizedText().utf16CodeUnits();
                final String span2Text = span2.span().tokenizedText().utf16CodeUnits();
                int slot0Start = span1.span().startCharOffset().asInt();
                int slot0End = span1.span().endCharOffset().asInt();
                int slot1Start = span2.span().startCharOffset().asInt();
                int slot1End = span2.span().endCharOffset().asInt();
                // Maybe, we should do some filtering here?
                CausealJson causealJson = new CausealJson(
                        docId,
                        ImmutableList.of(ImmutableList.of(slot0Start,slot0End)),
                        span1Text,
                        ImmutableList.of(ImmutableList.of(slot1Start,slot1End)),
                        span2Text,
                        instance.getSecond(),
//                    ImmutableList.copyOf(new HashSet<>(mappings.getInstance2Pattern().getPatterns(instanceIdentifier)))
                        ImmutableList.copyOf(new HashSet<>()),
                        languageMatchInfo.getSentTheory().span().tokenizedText().utf16CodeUnits()
                );
                ret.add(causealJson);
            }
            return ret;
        }
    }
    @SuppressWarnings("serial")
    @MultipartConfig(location="/tmp/upload", fileSizeThreshold=1024*1024, maxFileSize=1024*1024*16)
    @WebServlet(urlPatterns={"/"}, name="upload")
    public static class UploadServlet extends HttpServlet
    {
        final SingleDocumentDecoder singleDocumentDecoder;
        UploadServlet(SingleDocumentDecoder singleDocumentDecoder){
            this.singleDocumentDecoder = singleDocumentDecoder;
        }
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
        {
            resp.setContentType("application/json;charset=utf-8");
            String serifxmlStr = convertStreamToString(req.getPart("serifxml_str").getInputStream());
            ObjectMapper mapper = StorageUtils.getMapperWithoutTyping();
            System.out.println("Received SerifXML String");
            List<CausealJson> result = null;
            try {
                result = this.singleDocumentDecoder.ConvertRelationsToJson(this.singleDocumentDecoder.LoadDocument(serifxmlStr));
            } catch (ExecutionException | InterruptedException e) {
                throw new IOException(e);
            }
            mapper.writeValue(resp.getWriter(), result);
        }
    }
    public static void main(String[] args) throws Exception {
        String params = args[0];
        String relation = args[1];
        String extractorPath = args[2];
        int port = Integer.parseInt(args[3]);
        LearnItConfig.loadParams(new File(params));
        Target target = TargetFactory.fromNamedString(relation);
        SingleDocumentDecoder singleDocumentDecoder = new SingleDocumentDecoder(target,extractorPath);
        System.out.println("starting server...");
        Server server = new Server(port);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        ServletHolder fileUploadServletHolder = new ServletHolder(new UploadServlet(singleDocumentDecoder));
        fileUploadServletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement("data/tmp"));
        context.addServlet(fileUploadServletHolder, "/learnit");
        server.setHandler(context);
        server.start();
        server.join();
    }
}
