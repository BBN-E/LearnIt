package com.bbn.akbc.neolearnit.serializers.observations;

import java.util.*;

public class EERGraph {

    public static class Node{
        public String nodeId;
        public int cnt=0;
        public Set<String> examples = new HashSet<>();
        public String nodeName;
        public String nodeType;
        public void increaseCnt(int cnt){
            this.cnt += cnt;
        }
        public void putExample(String example,int maxExamplesPerElem){
            if(this.examples.size()<maxExamplesPerElem){
                this.examples.add(example);
            }
        }

        public Node(String nodeId){
            this.nodeId = nodeId;
        }

        public Map<String,Object> toDict(){
            Map<String,Object> ret = new HashMap<>();
            ret.put("node_name",this.nodeName);
            ret.put("node_type",this.nodeType);
            ret.put("cnt",this.cnt);
            ret.put("node_id",this.nodeId);
            ret.put("examples",new ArrayList<>(this.examples));
            return ret;
        }
    }

    public static class Edge{

        public Node leftNode;
        public Node rightNode;

        public String edgeName;
        public String edgeType;
        public int cnt = 0;
        public Set<String> examples = new HashSet<>();

        public void putExample(String example,int maxExamplesPerElem){
            if(this.examples.size()<maxExamplesPerElem){
                this.examples.add(example);
            }
        }

        public Edge(Node leftNode, Node rightNode, String edgeName, String edgeType){
            this.leftNode = leftNode;
            this.rightNode = rightNode;
            this.edgeName = edgeName;
            this.edgeType = edgeType;

        }
        public void increaseCnt(int cnt){
            this.cnt += cnt;
        }
        public Map<String,Object> toDict(){
            Map<String,Object> ret = new HashMap<>();
            ret.put("left_node_id",this.leftNode.nodeId);
            ret.put("right_node_id",this.rightNode.nodeId);
            ret.put("edge_name",this.edgeName);
            ret.put("edge_type",this.edgeType);
            ret.put("cnt",this.cnt);
            ret.put("examples",new ArrayList<>(this.examples));
            return ret;
        }
    }
}
