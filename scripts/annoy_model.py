import os
import sys

from annoy import AnnoyIndex
import numpy as np


class AnnoyModel:

    def __init__(self, word_dict, index, metric):
        self.word_dict = word_dict
        self.reverse_word_dict = {y: x for x, y in word_dict.items()}
        self.index = index
        # following is currently unused; plus it seems like 'angular' metric always returns a distance of 0.0
        self.metric = metric

    @staticmethod
    def load_annoy_model(embeddings_dict, metric='euclidean'):
        #int_to_word = dict()
        w_index = 0
        word_to_int = dict()
        dim = len(embeddings_dict.values()[0])
        index = AnnoyIndex(dim, metric=metric)
        for w in embeddings_dict.keys():
            # if w in int_to_word.values():
                # raise ValueError("Found {} present twice in the embeddings file".format(w))
            # w_index = len(int_to_word)
            # int_to_word[w_index] = w
            w_index+=1
            word_to_int[w] = w_index
            vec = embeddings_dict[w]
            index.add_item(w_index, vec)
            # if len(int_to_word) % 10000 == 0:
               # print("Loaded {} embeddings".format(len(int_to_word)))
            if w_index % 10000 == 0:
               print("Loaded {} embeddings".format(w_index))
        # this is an arbitrary parameter for the number of trees created (currently sticking to 100); 
        # higher the number better the precision, but also more the memory used
        index.build(100)
        print("Loaded {} embeddings".format(w_index))
        return AnnoyModel(word_to_int, index, metric)

    def __getitem__(self, item):
        if isinstance(item, int):
            return self.index.get_item_vector(item)
        elif isinstance(item, str):
            return self.index.get_item_vector(self.word_dict[item])
        else:
            raise ValueError("Unknwon type for {}, cannot retrieve a vector".format(item))

    def __contains__(self, item):
        if isinstance(item, int):
            return item in self.reverse_word_dict
        elif isinstance(item, str):
            return item in self.word_dict
        else:
            raise ValueError("Unknwon type for {}, cannot determine if we know about it".format(item))

    def get_nearest_neighbors_for_vec(self, vec, n_neighbors,include_distances=True):
        neighbor_words, distances = self.index.get_nns_by_vector(vec, n_neighbors, include_distances=include_distances)
        words = [self.reverse_word_dict[x] for x in neighbor_words]
        ret_list = [(w,1.0/(1.0 + distances[i])) for i,w in enumerate(words)]
        return ret_list


def main():
    embeddings_file = sys.argv[1]
    print("Loading embeddings")
    embeddings_dict = read_embeddings(embeddings_file)
    model = AnnoyModel.load_annoy_model(embeddings_dict, 'angular')
    


if __name__ == '__main__':
    main()
