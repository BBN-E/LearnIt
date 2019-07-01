from collections import defaultdict
import codecs
import numpy
import scipy
import pickle

# currently assumes the embeddings_file to be a pickle file in a specific format: [word_list,embeddings_list], where all embeddings are of type numpy.ndarray
def read_embeddings(embeddings_file_path):
    [word_list,embeddings_list] = [[],[]]
    try:
        with open(embeddings_file_path,'rb') as embeddings_file:
            [word_list,embeddings_list] = pickle.load(embeddings_file)
    except ValueError as err:
        raise err
    embeddings_file.close()
    if isinstance(embeddings_list,numpy.ndarray) and embeddings_list.size==0:
        raise ValueError('embeddings_list cannot be empty')
    if not isinstance(embeddings_list[0],numpy.ndarray):
        raise ValueError('embeddings_list must be a list of numpy.ndarray elements')
    embeddings = defaultdict(lambda : embeddings_list[0]-embeddings_list[0]) #default value is a zero-vector
    for i in range(len(word_list)):
        word = word_list[i]
        embeddings[word]=embeddings_list[i]
    return embeddings

def write_embeddings(embeddings,file_path):
    with open(file_path,'wb') as fp:
        pickle.dump(embeddings,fp)
    return 

def compute_average_vector(words,embeddings):
    vectors = [embeddings[word] for word in words]
    average_vector = sum(vectors)/len(vectors)
    return average_vector

def is_zero_vector(v):
    return numpy.count_nonzero(v)==0

def get_vector_similarity(vec1,vec2,metric='cosine'):
    if numpy.nonzero(vec1)[0].size==0 or numpy.nonzero(vec2)[0].size==0: #if either vector is a zero-vector
        return 0.0
    if metric=='euclidean':
        return 1.0/(1.0 + numpy.linalg.norm(vec1-vec2)) # use euclidean distance as distance metric (similarity = 1/(1+d))
    return numpy.dot(vec1,vec2)/(numpy.linalg.norm(vec1)*numpy.linalg.norm(vec2))

