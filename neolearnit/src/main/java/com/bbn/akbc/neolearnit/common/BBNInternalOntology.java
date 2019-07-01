package com.bbn.akbc.neolearnit.common;

import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class BBNInternalOntology {
    public static Multimap<String, String> buildTriggerExampleToOntologyIdMapping(String jsonFile) throws Exception {
        // @hqiu. Need refactor
        Multimap<String, String> ret = HashMultimap.create();
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(jsonFile));

        JSONObject jsonObject = (JSONObject) obj;

        for (Object eventObject : jsonObject.keySet()) {
            String eventType = (String) eventObject;

            System.out.println("event\t" + eventType);

            JSONObject map1 = (JSONObject) jsonObject.get(eventObject);

            if (map1.containsKey("exemplars")) {
                ArrayList<Object> triggers = (ArrayList<Object>) map1.get("exemplars");

                for (final Object trigger : triggers) {
                    JSONObject triggerJson = (JSONObject) trigger;
                    if (triggerJson.containsKey("trigger")) {
                        JSONObject map = (JSONObject) triggerJson.get("trigger");
                        if (map.containsKey("text")) {
                            String text = (String) map.get("text");
                            String[] tokens = text.trim().split(" ");
                            if (tokens.length == 1) {
                                String token = tokens[0].trim().toLowerCase();
                                ret.put(token, eventType);
                            }
                        }
                    }
                }
            }
        }
        return ret;
    }

    public static void DFSNodeNameToSlashJointNodePathMapParsing(BBNInternalOntologyNode root, Stack<BBNInternalOntologyNode> currentStack, Multimap<String, String> NodeNameToSlashJointMap) {
        currentStack.push(root);
        NodeNameToSlashJointMap.put(root.originalKey, "/" + currentStack.stream().map(i -> i.originalKey).collect(Collectors.joining("/")));
        for (BBNInternalOntologyNode child : root.children) {
            DFSNodeNameToSlashJointNodePathMapParsing(child, currentStack, NodeNameToSlashJointMap);
        }
        currentStack.pop();
    }


    public static void bindTriggerExampleToOntologyTree(BBNInternalOntologyNode root, String jsonFile) throws Exception {
        Map<String, BBNInternalOntologyNode> nodeIdToNodeMap = root.getPropToNodeMap(new BBNInternalOntologyNode.PropStringGetter("_id"));
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(jsonFile));

        JSONObject jsonObject = (JSONObject) obj;

        for (Object eventObject : jsonObject.keySet()) {
            String eventType = (String) eventObject;

            System.out.println("event\t" + eventType);

            JSONObject map1 = (JSONObject) jsonObject.get(eventObject);

            if (map1.containsKey("exemplars")) {
                ArrayList<Object> triggers = (ArrayList<Object>) map1.get("exemplars");
                // Dont know if this is desired. But by default, the examplars contain keyword NA
                nodeIdToNodeMap.get(eventType)._examples.clear();
                for (final Object trigger : triggers) {
                    JSONObject triggerJson = (JSONObject) trigger;
                    if (triggerJson.containsKey("trigger")) {
                        JSONObject map = (JSONObject) triggerJson.get("trigger");
                        if (map.containsKey("text")) {
                            String text = (String) map.get("text");
                            String[] tokens = text.trim().split(" ");
                            if (tokens.length == 1) {
                                String token = tokens[0].trim().toLowerCase();
                                BBNInternalOntologyNode bbnInternalOntologyNode = nodeIdToNodeMap.get(eventType);
                                bbnInternalOntologyNode._examples.add(token);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        BBNInternalOntologyNode ontologyRoot = BBNInternalOntology.BBNInternalOntologyNode.fromInternalOntologyFile(new File("/home/hqiu/ld100/CauseEx-pipeline-WM/CauseEx/ontology/internal_ontology/hume/event_ontology.yaml"));
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        System.out.println(objectMapper.writeValueAsString(ontologyRoot.convertToInternalOntologyYaml()));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BBNInternalOntologyNode {
        @JsonProperty
        public List<BBNInternalOntologyNode> children;
        @JsonProperty
        public String originalKey;


        public BBNInternalOntologyNode parent;

        @JsonProperty
        public String _id;
        @JsonProperty
        public List<String> _source;
        @JsonProperty
        public String _description;
        @JsonProperty
        public List<String> _examples;
        @JsonProperty
        public String _alternative_id;

        public BBNInternalOntologyNode() {
            this.children = new ArrayList<>();
            this.parent = null;
            this._source = new ArrayList<>();
            this._examples = new ArrayList<>();
            this._alternative_id = null;
        }

        public static BBNInternalOntologyNode fromInternalOntologyFile(File yamlFile) throws Exception {
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            List<Map<String, List<Map<String, Object>>>> parseRoot = objectMapper.readValue(yamlFile, List.class);
            return BBNInternalOntologyNode.fromYamlRoot(parseRoot);
        }

        public static BBNInternalOntologyNode fromYamlRoot(List<Map<String, List<Map<String, Object>>>> root) {
            List<BBNInternalOntologyNode> buf = new ArrayList<>();
            for (Map<String, List<Map<String, Object>>> yamlOntologyNode : root) {
                for (String originalKey : yamlOntologyNode.keySet()) {
                    BBNInternalOntologyNode ontologyNode = new BBNInternalOntologyNode();
                    ontologyNode.originalKey = originalKey;
                    for (Map<String, Object> entryMapPair : yamlOntologyNode.get(originalKey)) {
                        for (String key : entryMapPair.keySet()) {
                            Object value = entryMapPair.get(key);
                            if (key.startsWith("_")) {
                                if (key.equals("_id")) {
                                    ontologyNode._id = (String) value;
                                } else if (key.equals("_source")) {
                                    for (Object sourceObj : (List<Object>) value) {
                                        String sourceVal = (String) sourceObj;
                                        ontologyNode._source.add(sourceVal);
                                    }
                                } else if (key.equals("_description")) {
                                    ontologyNode._description = (String) value;
                                } else if (key.equals("_examples")) {
                                    for (Object exampleObj : (List<Object>) value) {
                                        String exampleVal = (String) exampleObj;
                                        if (!exampleVal.toLowerCase().equals("na")) {
                                            ontologyNode._examples.add(exampleVal);
                                        }
                                    }
                                } else if (key.equals("_alternative_id")) {
                                    ontologyNode._alternative_id = (String) value;
                                } else {
                                    throw new RuntimeException("The field " + key + " is reserved for internal usage, which you didn't parse it");
                                }
                            } else {
                                Map<String, List<Map<String, Object>>> buf1 = new HashMap<>();
                                buf1.put(key, (List<Map<String, Object>>) value);
                                List<Map<String, List<Map<String, Object>>>> buf2 = new ArrayList<>();
                                buf2.add(buf1);
                                BBNInternalOntologyNode childOntologyRoot = fromYamlRoot(buf2);
                                childOntologyRoot.parent = ontologyNode;
                                ontologyNode.children.add(childOntologyRoot);
                            }
                        }

                    }
                    buf.add(ontologyNode);
                }
            }

            return buf.get(0);
        }


        private void addRequiredFields(List<Map<String, Object>> retMap) {
            Map<String, Object> buf;

            buf = new HashMap<>();
            buf.put("_id", this._id);
            retMap.add(buf);

            buf = new HashMap<>();
            buf.put("_source", this._source);
            retMap.add(buf);

            buf = new HashMap<>();
            buf.put("_description", this._description);
            retMap.add(buf);

            buf = new HashMap<>();
            buf.put("_examples", this._examples);
            retMap.add(buf);

            buf = new HashMap<>();
            buf.put("_alternative_id", this._alternative_id);
            retMap.add(buf);
        }

        public List<Map<String, Object>> convertToInternalOntologyYaml() {
            List<Map<String, Object>> yamlWriterBuf = new ArrayList<>();
            List<Map<String, Object>> buf2 = new ArrayList<>();
            this.addRequiredFields(buf2);
            for (BBNInternalOntologyNode child : this.children) {
                buf2.add(child.convertToInternalOntologyYaml().get(0));
            }
            Map<String, Object> buf = new HashMap<>();
            buf.put(this.originalKey, buf2);
            yamlWriterBuf.add(buf);
            return yamlWriterBuf;
        }

        public void convertToInternalOntologyYamlFile(File yamlOntologyFile) throws Exception{
            BufferedWriter bf = new BufferedWriter(new FileWriter(yamlOntologyFile));
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            objectMapper.writeValue(bf,this.convertToInternalOntologyYaml());
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof BBNInternalOntologyNode)) return false;
            BBNInternalOntologyNode that = (BBNInternalOntologyNode) o;
            return that._id.equals(this._id);
        }

        @Override
        public int hashCode() {
            return this._id.hashCode();
        }

        @Override
        public String toString(){
            ObjectMapper objectMapper = StorageUtils.getMapperWithoutTyping();
            try{
                return objectMapper.writeValueAsString(this);
            }
            catch (Exception e){
                return "{}";
            }
        }

        private static void dfsWorkerForMap(BBNInternalOntologyNode node, PropStringGetter propStringGetter, Map<String, BBNInternalOntologyNode> map) throws IllegalAccessException {
            if (map.containsKey(propStringGetter.getter(node))) {
                throw new RuntimeException("The key you chosen is not unique on the whole ontology tree");
            }
            map.put(propStringGetter.getter(node), node);
            for (BBNInternalOntologyNode child : node.children) {
                dfsWorkerForMap(child, propStringGetter, map);
            }
        }

        private static void dfsWorkerMultiMap(BBNInternalOntologyNode node, PropStringGetter propStringGetter, Multimap<String, BBNInternalOntologyNode> map) throws IllegalAccessException {
            map.put(propStringGetter.getter(node), node);
            for (BBNInternalOntologyNode child : node.children) {
                dfsWorkerMultiMap(child, propStringGetter, map);
            }
        }

        public Map<String, BBNInternalOntologyNode> getPropToNodeMap(PropStringGetter strGetter) throws IllegalAccessException {
            Map<String, BBNInternalOntologyNode> ret = new HashMap<>();
            dfsWorkerForMap(this, strGetter, ret);
            return ret;
        }

        public Multimap<String, BBNInternalOntologyNode> getPropToNodeMultiMap(PropStringGetter stringGetter) throws IllegalAccessException {
            Multimap<String, BBNInternalOntologyNode> ret = ArrayListMultimap.create();
            dfsWorkerMultiMap(this, stringGetter, ret);
            return ret;
        }

        public interface PropGetter {
            String getter(BBNInternalOntologyNode node) throws IllegalAccessException;
        }

        public static class PropStringGetter implements PropGetter {
            final String fieldName;

            public PropStringGetter(final String fieldName) {
                this.fieldName = fieldName;
            }

            @Override
            public String getter(BBNInternalOntologyNode node) throws IllegalAccessException {
                try {
                    Field f = node.getClass().getDeclaredField(fieldName);
                    f.setAccessible(true);
                    return (String) f.get(node);
                } catch (NoSuchFieldException e) {
                    return "";
                }
            }

        }
    }

}
