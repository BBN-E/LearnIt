import sys, os
import itertools
import math
import time
import random

import wbq_params
import tensorflow as tf
from tensorflow.core.framework import summary_pb2
from tensorflow.python.client import timeline
from tensorflow.python import debug as tf_debug
import ijson
import numpy as np
import cPickle

from data_loader import DataLoader
from model.multi_model import MultiModel


class DeepEventEventRelationMentionDecoder:
    def __init__(self, paramsFile):
        params = wbq_params.read(paramsFile)

        print("params: " + str(params))

        self.tf_model_prefix=params['TensorFlowEventEventRelationMentionExtractionModelPrefix']
        self.tf_model_relation2id=params['TensorFlowEventEventRelationMentionExtractionModelRelation2id']
        self.tf_model_word2vec=params['TensorFlowEventEventRelationMentionExtractionModelWord2vec']

        self.max_length=120
        self.batch_size=160
        self.word_embedding_dim=300
        self.encoder="pcnn"
        self.selector="att"

        self.test_data_loader = DataLoader(self.tf_model_word2vec,
                                      self.tf_model_relation2id,
                                      mode=DataLoader.MODE_INSTANCE,
                                      shuffle=False,
                                      max_length=self.max_length,
                                      batch_size=self.batch_size)

        self.test_data_loader.create_dataset([])

        self.model = MultiModel(None, self.test_data_loader,
                           max_length=self.max_length,
                           batch_size=self.batch_size,
                           word_embedding_dim=self.word_embedding_dim,
                           encoder=self.encoder,
                           selector=self.selector)

        self.model.load_best_model(self.tf_model_prefix)

        self.model.load_id2rel(self.tf_model_relation2id)


    def decode(self, serializedInstance):

        # we have one and only one instance each time this function is called
        # decoding_json = [serializedInstance]
        decoding_json = [serializedInstance for _ in range(0, 160)]

        self.test_data_loader.create_dataset(decoding_json)

        predicted_rels,predicted_prob = self.model.predict(self.test_data_loader)

        # we have one and only one instance in this list
        label = predicted_rels[0]
        if label=="NA":
            label="OTHER"
        confidence = predicted_prob[0]

        print("PREDICTION:\t" + label + "\t" + str(confidence) + "\tserializedInstance: " + str(serializedInstance))

        return { 'label': label, 'confidence' : str(confidence) }
