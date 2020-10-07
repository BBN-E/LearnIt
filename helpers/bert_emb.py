
import numpy as np
import logging

logger = logging.getLogger(__name__)

class BertEmbCache(object):
    def __init__(self, doc_id_to_bert_npz_path):
        self.doc_id_to_bert_npz_path = doc_id_to_bert_npz_path
        self.doc_id_to_bert = dict()

    def get_bert_emb(self, docid, sent_id, token_id, should_cached=lambda x:True):
        d = dict()
        if docid not in self.doc_id_to_bert.keys():
            logger.info("Loading {}".format(docid))
            with np.load(self.doc_id_to_bert_npz_path[docid],
                         allow_pickle=True) as fp2:
                embeddings = fp2['embeddings']
                token_map = fp2['token_map']
                d['embeddings'] = embeddings
                d['token_map'] = token_map
                if should_cached(docid) is True:
                    self.doc_id_to_bert[docid] = d
        else:
            d = self.doc_id_to_bert[docid]
        token_map = d['token_map']
        embeddings = d['embeddings']
        head_token_idx_in_bert = token_map[sent_id][token_id]
        return embeddings[sent_id][head_token_idx_in_bert]
