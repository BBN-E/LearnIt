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


class DeepRelationMentionDecoder:
    def __init__(self, paramsFile):
        params = wbq_params.read(paramsFile)
        # self.model_prefix=params['modelFile']

    def decode(self, serializedInstance):
        label=serializedInstance['label']

        orderedListTokenData=serializedInstance['orderedListTokenData']
        sent=""
        for i in range(len(orderedListTokenData)):
            sent+=orderedListTokenData[i]['tokenString']+" "
        print("label="+str(label)+" , " + "sent={" + sent + "}")

        return { 'label': 'MY_TF_RELATION', 'confidence' : 0.71 }
