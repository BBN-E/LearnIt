import sys,codecs,re
from vector_utils import read_embeddings
from vector_utils import compute_average_vector
from vector_utils import write_embeddings
from numpy import asarray

'''
This script computes pattern embeddings using word embeddings (e.g. Glove embeddings) after normalizing the pattern to tokens.
If you want to use embeddings learned for the full for of patterns (like embeddings learned for gigaword patterns with TransE model), use the script extract_pattern_vectors.py
'''

def main():
    print 'Reading pattern list file...'
    pattern_files_list = open(sys.argv[1])
    print 'Reading embeddings...'
    embeddings = read_embeddings(sys.argv[2]) 
    output_path = sys.argv[3]

    pattern_list = []
    pattern_vectors = []
 
    regex = re.compile("[\\[\\]:_\\s<>=]+") #stuff to be removed from pattern string for normalization: remove all special characters and multiple spaces

    for line in pattern_files_list.readlines():
        print 'Reading pattern file '+line[:-1]
        pattern_file = codecs.open(line[:-1],"r","utf-8")
        print '\t...extracting pattern vectors'

        def normalize_pattern_string(pattern):
            if '{0}' not in pattern and '{1}' not in pattern and '{' in pattern: # this should be a combination pattern
                pattern = pattern.replace('{','').replace('}','')
            pattern = regex.sub(' ',pattern).strip();
            return pattern

        for pattern in pattern_file.readlines():
            if len(pattern.split('\t'))!=2:
                continue
            pattern = pattern.split('\t')[0]
            pattern_list.append(pattern) #add original pattern
            pattern_tokens = normalize_pattern_string(pattern).split()
            pattern_vector = compute_average_vector(pattern_tokens,embeddings)
            pattern_vectors.append(pattern_vector)

    print 'Serializing pattern vectors...'
    write_embeddings([pattern_list,asarray(pattern_vectors)],output_path)

    print 'Done!'
	
if __name__ == '__main__':
    main()
