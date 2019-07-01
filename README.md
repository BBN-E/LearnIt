# LearnIt

LearnIt is a Machine Reading customization tool develop by [BBN](https://www.raytheon.com/ourcompany/bbn) with support from
 DARPA [World Modelers](https://www.darpa.mil/program/world-modelers)
and [Causal Exploration](https://www.darpa.mil/program/causal-exploration) programs. 

LearnIt is a tool for customizing Machine Readers (a.k.a., Information Extraction algorithms) 
with human in the loop. It enables users to build event and event-event relation extractors
with a small amount of efforts.
 
Once the extractors are built, they can be applied to any corpora to extract events and event-event relations. 
This version includes a HTML visualizer to visualize these extractions. The extractors can also be plugged into BBN's
[Hume](https://github.com/BBN-E/Hume) Machine Reading system. 

## Contents

- Running LearnIt
    -   [Build LearnIt Mappings](#build-learnit-mappings)
    -   [Build extractors](#build-extractors)
    -   [Apply extractors](#apply-extractors)

# Running LearnIt

LearnIt is a `maven` based `Java8` project. You'll need to install `maven` and `openjdk8` to get started. You'll also need extra jars by emailing us.

First, build LearnIt by

```bash
cd [LEARNIT_REPO_ROOT]
cp -r [LEARNIT_EXP_ROOT] data
mvn install:install-file -Dfile=data/libs/common-core-open-8.0.0.jar -DgroupId=com.bbn.bue -DartifactId=common-core-open -Dversion=8.0.0 -Dpackaging=jar
mvn install:install-file -Dfile=data/libs/nlp-core-open-8.0.0.jar -DgroupId=com.bbn.nlp -DartifactId=nlp-core-open -Dversion=8.0.0 -Dpackaging=jar

mvn install:install-file -Dfile=data/libs/serif-8.10.3-SNAPSHOT-pg.jar -DgroupId=com.bbn.serif -DartifactId=serif -Dversion=8.10.3-SNAPSHOT -Dpackaging=jar
mvn install:install-file -Dfile=data/libs/common-core-4.1.0-pg.jar -DgroupId=com.bbn.bue -DartifactId=common-core -Dversion=4.1.0 -Dpackaging=jar
mvn install:install-file -Dfile=data/libs/nlp-core-4.1.0-pg.jar -DgroupId=com.bbn.nlp -DartifactId=nlp-core -Dversion=4.1.0 -Dpackaging=jar
mvn install:install-file -Dfile=data/libs/learn-core-6.7.0-pg.jar -DgroupId=com.bbn.bue -DartifactId=learn-core -Dversion=6.7.0 -Dpackaging=jar

mvn clean install -am
```

Second, make a system parameter file:

```bash
cd [LEARNIT_REPO_ROOT]
cp params/learnit/user.params.example params/learnit/user.params
```

And then edit `params/learnit/user.params` by changing `learnit_root` to your LearnIt directory.

## Build LearnIt Mappings

A LearnIt Mappings is a collection of bidirectional maps, in which instances (event or event-event relation candidates)
and their features (lingustic observations such as context and part-of-speech tags) are stored for efficient access. 

To get started with a new corpus, you'll need to build a LearnIt mappings for this corpus. 
The input corpus currently is in SERIF XML format (You can request a [World Modelers](https://www.darpa.mil/program/world-modelers) `M12` corpus by emailing us. 

Then make a param file on each corpus.We also prepare the parameter file sets for `M12` corpus. 
Please change file `params/learnit/domains/wm_m12_demo.param`,  line `learnit_data_dir` to `%learnit_root%/data/corpus/wm_m12_demo`.

Create a Mappings by:

```bash
find $PWD/data/corpus/wm_m12_demo/serifxml/*.xml > data/corpus/wm_m12_demo/source_lists/00000.list
JAVA_OPTS="-Xmx18G" ./neolearnit/target/appassembler/bin/InstanceExtractor params/learnit/runs/wm_m12_demo.params all data/corpus/wm_m12_demo/source_lists/00000.list data/corpus/wm_m12_demo/source_mappings/00000.sjson
```

A Mappings file `00000.sjson` will be generated.

## Build extractors

`InstanceExtractor` is in charge of auto populate all `Seeds` and `LearnitPattern` from the corpus. 
They're in neutral status. User have to add them into specific types. 
(For event, `types` are `Trend`,  `Percipitation`, `Famine` etc.. 
For event-event relation, `types` are `Cause-Effect` etc..). So let's bring up an UI. Please execute:

```bash
./neolearnit/target/appassembler/bin/OnDemandReMain params/learnit/runs/wm_m12_demo.params data/corpus/wm_m12_demo/source_mappings/00000.sjson 5022
```

And then open your browser [http://127.0.0.1:5022](http://127.0.0.1:5022).

You can add a new event type or an new event-event relation type by clicking `+` on the upper panel. 
You only need to fill in `name` in the pop-up window.

In the working panes, you can label good `Seeds` or `LearnitPattern` for each `types`. 
For events, `Seeds` means potential words that may indicate there's an event. 
`LearnitPattern` is a linguistic pattern that indicates the existence of an event. 
For event-event relations, `Seeds` means a pair of word that indicates there's a relation. For example, `(attack,death)` is an example for the target relation `Cause-Effect`. 
Similar to events, `LearnitPatterns` is a linguistic pattern, e.g. `{0} caused {1}`,  that indicates there is a relation. 

For a target type, you can select a positive `Seed` or `LearnitPattern` by clicking on `+`,
 and select a negative `Seed` or `LearnItPattern` by clicking on `-`. 
Also, there's a `Propose` button which is used for helping you find more `Seeds` or `LearnitPattern` candidates from mappings 
automatically. After you click `Purpose` under `LearnitPattern`, LearnIt will use the `Seeds` that marked 
as positive, to purpose to you additionally `LearnitPatterns` that appears in sentences matched by the `Seeds`.

Your extractors will be saved at `inputs/extractors/YOUR_TARGET_NAME/*.json`. 
Each name contains a timestamp so you could find the newest one. 

Please click `Save Progress` at up right corner before you shut down the backend server.

## Apply extractors

### Visualize extractions decoded with your extractors from a corpus

Once you finish building event and event-event-relation extractors, 
you can apply them on any corpus. Suppose we want to see results on the original corpus, you can do

```bash
./neolearnit/target/appassembler/bin/TargetAndScoreTableGoodSeedAndPatternLabeler params/learnit/runs/wm_m12_demo.params data/corpus/wm_m12_demo/source_mappings/00000.sjson data/corpus/wm_m12_demo/labeled_00000.sjson
./neolearnit/target/appassembler/bin/BinaryEventRelationHTMLDecoder params/learnit/runs/wm_m12_demo.params data/corpus/wm_m12_demo/labeled_00000.sjson data/corpus/wm_m12_demo/labeled_00000.html
```

Then, open `labeled_00000.html` to see results. 
Only sentences that been captured by an event-event relation extractor will appear here. 

### Apply LearnIt to extract event-event relations

Currently, we support outputting a JSON file when applying an event-event relation extractor to a corpus. 

First, you'll need to make a `corpus_2.params` and generate a mappings file `corpus_2.sjson`, by following 
 instructions in [Build LearnIt Mappings](#build-learnit-mappings)

You also need a folder contains a set of event-event relation extractors. For example, we included `event_event_relation_extractors`. 

```bash
./neolearnit/target/appassembler/bin/EventEventRelationPatternDecoder corpus_2.params corpus_2.sjson causal_json_output.json all 0 na na data/event_event_relation_extractors
```

The causal realtions extracted will appear in `causal_json_output.json`.