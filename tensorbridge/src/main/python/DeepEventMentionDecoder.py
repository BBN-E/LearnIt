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

from sklearn.externals import joblib
from model.nn_model import Model
from batcher import Batcher


class DeepEventMentionDecoder:
    def __init__(self, paramsFile):
        params = wbq_params.read(paramsFile)

        print("params: " + str(params))

        tf_model_dir=params['TensorFlowEventMentionExtractionModelDir']
        tf_model_dict=params['TensorFlowEventMentionExtractionModelDict']

        self.threshold = float(params['threshold'])

        print "Loading dictionaries..."
        dicts = joblib.load(tf_model_dict)

        self.label2id = dicts["label2id"]
        self.id2label = dicts["id2label"]
        self.word2id = dicts["word2id"]
        self.feature2id = dicts["feature2id"]
        self.id2vec = dicts["id2vec"]

        print "Loading the model..."
        self.model = Model.load(os.path.join(tf_model_dir,"tf_session"), self.label2id)

    def preprocess_dataset(self, example, label2id, word2id, feature2id):
        num_of_samples = 1 # decode one by one

        num_of_labels = len(label2id.values())
        storage = []
        data = np.zeros((num_of_samples, 4 + 70 + num_of_labels), "int32")
        s_start_pointer = 0
        num = 0

        start = int(example["mention_idx_start"])
        end = int(example["mention_idx_end"])
        features = example["features"]
        words = example["sentence"].split()
        labels = example["label"]

        length = len(words)

        # one-hot representation
        labels_code = [0 for i in range(num_of_labels)]
        for label in labels:
            if label in label2id:
                labels_code[label2id[label]] = 1

        words_code = [word2id[word] if word in word2id else word2id["unknown_word_case"] for word in words]
        features_code = [feature2id[feature] if feature in feature2id else feature2id["unknown_feature_case"] for
                         feature in features]
        storage += words_code
        data[num, 0] = s_start_pointer  # s_start
        data[num, 1] = s_start_pointer + length  # s_end
        data[num, 2] = s_start_pointer + start  # e_start
        data[num, 3] = s_start_pointer + end  # e_end
        data[num, 4:4 + len(features_code)] = np.array(features_code)
        data[num, 74:] = labels_code
        s_start_pointer += length
        num += 1
        if num % 100000 == 0:
            print num

        storage = np.array(storage, "int32")
        dataset = {"storage":storage,"data":data}
        return dataset

    def decode(self, serializedInstance):
        label=serializedInstance['label']

        print ("serializedInstance sentence : " + serializedInstance["sentence"])

        dataset = self.preprocess_dataset(serializedInstance, self.label2id, self.word2id, self.feature2id)

        batcher = Batcher(dataset["storage"],dataset["data"],dataset["data"].shape[0],10,self.id2vec)

        context_data, mention_representation_data, target_data, feature_data = batcher.next()

        scores = self.model.predict(context_data, mention_representation_data, feature_data)

        score = scores[0]
        label_id, label_score = max(enumerate(list(score)), key=lambda x: x[1])

        if label_score>=self.threshold:
            predicted_label = self.id2label[label_id]
        else:
            predicted_label = "OTHER"

        print ("predicted_label: " + predicted_label + ", label_id: " + str(label_id) + ", score: " + str(label_score))

        return { 'label': predicted_label, 'confidence' : str(label_score) }
