import sys,codecs, re
from vector_utils import read_embeddings
from vector_utils import write_embeddings
from numpy import asarray

'''
This script merely extracts learned embeddings from an embeddings file for existing patterns.
If you want to use word embeddings to compute pattern embeddings, use the script 'compute_pattern_vectors.py'.
'''

def main():
    print 'Reading pattern list file...'
    pattern_files_list = open(sys.argv[1])
    print 'Reading embeddings...'
    embeddings = read_embeddings(sys.argv[2])
    output_path = sys.argv[3]

    pattern_list = []
 
    for line in pattern_files_list.readlines():
        print 'Reading pattern file '+line[:-1]
        pattern_file = codecs.open(line[:-1],"r","utf-8")
        print '\t...extracting pattern vectors'
        for pattern in pattern_file:
            pattern_list.append(pattern.split('\t')[0])
    
    regex = re.compile("[\\[\\]:_\\s]+") #stuff to be removed from pattern string for normalization: remove all special characters and multiple spaces

    def normalize_pattern_string(pattern): 
        # normalize pattern string to the format that exists in the embeddings file (basically the transformation used for gigaword patterns with TransE model)
        pattern = pattern.replace(' = ','=')
        pattern = regex.sub(' ',pattern).strip();
        return pattern

    pattern_vectors = [embeddings[normalize_pattern_string(p)] for p in pattern_list]

    print 'Serializing pattern vectors (length='+str(len(pattern_vectors))+')...'
    write_embeddings([pattern_list,asarray(pattern_vectors)],output_path)

    print 'Done!'
	
if __name__ == '__main__':
    main()
