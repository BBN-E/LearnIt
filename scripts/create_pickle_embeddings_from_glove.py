import sys, os, codecs
from vector_utils import write_embeddings
import numpy as np

def main():
    glove_embeddings_txt = sys.argv[1]
    output_embeddings_file = sys.argv[2]
    word_list = []
    vector_list = []

    print 'Reading glove embeddings file...'
    glove_file = codecs.open(glove_embeddings_txt,'r','utf-8')
    lines_read = 0
    lines = glove_file.readlines() 
    total_lines = len(lines)
    for line in lines:
        lines_read+=1
        if lines_read%1000==0:
            print 'Read '+str(lines_read)+' of '+str(total_lines)
        tokens = line.split()
        word = tokens[0]
        vector = [float(t) for t in tokens[1:]]
        vector = np.array(vector)
        word_list.append(word)
        vector_list.append(vector)

    print 'Writing embeddings...'
    write_embeddings([word_list,vector_list],output_embeddings_file)
                
    print 'Done! Embeddings written to '+output_embeddings_file

if __name__ == '__main__':
    main()
