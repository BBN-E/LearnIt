package com.bbn.akbc.neolearnit.bootstrapping;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class YamlOntology {

    private Multimap<String, String> concept2path = HashMultimap.create();

    YamlOntology(String strFileYamlOntology) {
        System.out.printf("-- loading from %s --%n", strFileYamlOntology);
        try {
            Yaml yaml = new Yaml();

            InputStream in = new FileInputStream(strFileYamlOntology);

            Iterable<Object> itr = yaml.loadAll(in);
            for (Object o : itr) {
                fillInConcept2path(o, "");
            }

            for (String concept : concept2path.keySet()) {
                for (String p : concept2path.get(concept)) {
                    System.out.println(concept + "\t" + p);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Collection<String> getPathFromConcept(String concept) {
        if(concept2path.containsKey(concept))
            return concept2path.get(concept);
        else
            return Collections.emptyList();
    }

    void fillInConcept2path(Object object, String prefix) {
        if(object instanceof Map) {
            Map<String, Object> concept2object = (Map<String, Object>) object;

            for (String concept : concept2object.keySet()) {
                if(concept.startsWith("_"))
                    continue;

                String path = prefix + "/" + concept;

                concept2path.put(concept, path);

                Object o = concept2object.get(concept);
                if(o instanceof String) {
                    String leafName = (String) o;

                    if(leafName.startsWith("_"))
                        continue;

                    concept2path.put(leafName, path + "/" + leafName);
                }
                else {
                    fillInConcept2path(o, path);
                }
            }
        }
        else if(object instanceof ArrayList) {
            ArrayList<Object> objects = (ArrayList<Object>)object;
            for(Object o : objects) {
                if(o instanceof String) {
                    String leafName = (String) o;

                    if(leafName.startsWith("_"))
                        continue;

                    concept2path.put(leafName, prefix + "/" + leafName);
                }
                else {
                    fillInConcept2path(o, prefix);
                }
            }
        }
    }

    public static void main(String[] args) {
        String strFileYamlOntology = "/nfs/ld100/u10/bmin/repo_clean_for_exp_causeex/CauseEx/ontology/internal_ontology/causeex/event_ontology.yaml";
        YamlOntology yamlOntology = new YamlOntology(strFileYamlOntology);
    }
}
