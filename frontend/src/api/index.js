import axios from 'axios';
import constants from '@/constants.js';

export default{
    getOntology:function(){
        return axios({
            baseURL: constants.baseURL,
            url: "/ontology/current_tree",
            method: "GET",
          });
    },
    addOntologyNode:function(parent,newRootId,description,isUnaryTarget,isEventTarget){
        return axios({
            baseURL:constants.baseURL,
            url:"/ontology/add_target",
            method:"POST",
            params:{
                parentNodeId:parent,
                id:newRootId,
                description:description,
                isUnaryTarget:isUnaryTarget,
                isEventTarget:isEventTarget
            }
        });
    },
    addTargetAndScoreTable:function(targetName,description,slot0EntityTypes,slot1EntityTypes,symmetric,isUnaryTarget,isEventTarget){
        return axios({
            baseURL:constants.baseURL,
            url:"/init/add_target",
            method:"POST",
            params:{
                name:targetName,
                description:description,
                slot0EntityTypes:slot0EntityTypes,
                slot1EntityTypes:slot1EntityTypes,
                symmetric:symmetric,
                isUnaryTarget:isUnaryTarget,
                isEventTarget:isEventTarget
            }
        });
    },
    loadTargetAndScoreTable:function(targetName){
        return axios({
            baseURL:constants.baseURL,
            url:"/init/get_extractor",
            method:"POST",
            params:{relation: targetName}
        });
    },
    getPatternsByKeyword:function(targetName,keyword,amount){
        return axios({
            baseURL:constants.baseURL,
            url:"/init/get_patterns_by_keyword",
            method:"POST",
            params:{target: targetName,keyword:keyword,amount:amount}
        });
    },
    getSeedsBySlot:function(targetName,slot0,slot1,amount){
        return axios({
            baseURL:constants.baseURL,
            url:"/init/get_seeds_by_slots",
            method:"POST",
            params:{target:targetName,slot0:slot0,slot1:slot1,amount:amount}
        });
    },
    getPatternInstances:function(targetName,patternStr,amount,fromOther){
        return axios({
            baseURL:constants.baseURL,
            url:"/init/get_pattern_instances",
            method:"POST",
            params:{target:targetName,pattern:patternStr,amount:amount,fromOther:fromOther}
        });
    },
    getSeedInstances:function(targetName,seedJson,amount,fromOther){
        return axios({
            baseURL:constants.baseURL,
            url:"/init/get_seed_instances",
            method:"POST",
            params:{target:targetName,seed:seedJson,amount:amount,fromOther:fromOther}
        });
    },
    proposeSeeds:function(targetName){
        return axios({
            baseURL:constants.baseURL,
            url:"/init/propose_seeds_new",
            method:"POST",
            params:{target:targetName}
        });
    },
    proposeLearnitPatterns:function(targetName){
        return axios({
            baseURL:constants.baseURL,
            url:"/init/propose_patterns_new",
            method:"POST",
            params:{target:targetName}
        });
    },
    getSimilarSeeds:function(targetName){
        return axios({
            baseURL:constants.baseURL,
            url:"/init/similar_seeds",
            method:"POST",
            params:{target:targetName}
        });
    },
    getSimilarLearnitPatterns:function(targetName){
        return axios({
            baseURL:constants.baseURL,
            url:"/init/similar_patterns",
            method:"POST",
            params:{target:targetName}
        });
    },
    markSeed:function(targetName,seedObj,isFrozen,isGood){
        if(isFrozen){
            return axios({
                baseURL:constants.baseURL,
                url:"/init/add_seeds",
                method:"POST",
                params:{target:targetName, seeds: [seedObj], quality:isGood?"good":"bad"}
            });
        }
        else{
            return axios({
                baseURL:constants.baseURL,
                url:"/init/remove_seed",
                method:"POST",
                params:{target:targetName, seed: seedObj}
            })
        }
    },
    markPattern:function(targetName,patternStr,isFrozen,isGood){
        if(isFrozen){
            return axios({
                baseURL:constants.baseURL,
                url:"/init/add_pattern",
                method:"POST",
                params:{target:targetName, pattern: patternStr, quality:isGood?"good":"bad"}
            });
        }
        else{
            return axios({
                baseURL:constants.baseURL,
                url:"/init/remove_pattern",
                method:"POST",
                params:{target:targetName, pattern: patternStr}
            });
        }
    },
    saveProgress:function(){
        return axios({
            baseURL:constants.baseURL,
            url:"/init/save_progress",
            method:"POST",
        });
    },
    clearUnknown:function(targetName){
        return axios({
            baseURL:constants.baseURL,
            url:"/init/clear_unknown",
            method:"POST",
            params:{target:targetName}
        });
    },
    clearAll:function(targetName){
        return axios({
            baseURL:constants.baseURL,
            url:"/init/clear_all",
            method:"POST",
            params:{target:targetName}
        });
    },
    markInstance:function(targetName,instanceIdentifier,quality){
        return axios({
            baseURL:constants.baseURL,
            url:"/init/add_instance",
            method:"POST",
            params:{target:targetName,instance:instanceIdentifier,quality}
        });
    },
    unMarkInstance:function(targetName,instanceIdentifier){
        return axios({
            baseURL:constants.baseURL,
            url:"/init/remove_instance",
            method:"POST",
            params:{target:targetName,instance:instanceIdentifier}
        });
    },
    markInstanceFromOther:function(ontoloyTypeName,instanceIdentifier){
        return axios({
            baseURL:constants.baseURL,
            url:"/init/mark_instance_from_other",
            method:"POST",
            params:{target:ontoloyTypeName,instance:instanceIdentifier}
        })
    },
    markInstanceByPatternFromOther:function(ontoloyTypeName,patternStr){
        return axios({
            baseURL:constants.baseURL,
            url:"/init/mark_pattern_from_other",
            method:"POST",
            params:{target:ontoloyTypeName,pattern:patternStr}
        });
    },
    markInstanceBySeedFromOther:function(ontoloyTypeName,seedJson){
        return axios({
            baseURL:constants.baseURL,
            url:"/init/mark_seed_from_other",
            method:"POST",
            params:{target:ontoloyTypeName,seed:seedJson}
        });
    },
}