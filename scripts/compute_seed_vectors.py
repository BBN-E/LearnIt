import sys, codecs
from vector_utils import read_embeddings
from vector_utils import compute_average_vector
from vector_utils import write_embeddings
from numpy import asarray


def main():
    print 'Reading seed list file...'
    seed_files_list = open(sys.argv[1])
    print 'Reading embeddings...'
    embeddings = read_embeddings(sys.argv[2])
    output_path = sys.argv[3]

    seed_list = []
    seed_vectors = []
 
    for line in seed_files_list:
        print 'Reading seed file '+line[:-1]
        seed_file = codecs.open(line[:-1],"r","utf-8")
        print '\t...computing seed vectors'
        for seed in seed_file:
            word_pair= seed[:-1].split("\t")[:-1]
            if len(word_pair)!=2:
                continue
            word1 = word_pair[0].lower()
            word2 = word_pair[1].lower()
            seed_vector = compute_average_vector([word1,word2],embeddings)
            #print word1+" "+word2+" "+str(seed_vector)
            seed_list.append((word1,word2,))
            seed_vectors.append(seed_vector)
        seed_file.close()

    print 'Serializing seed vectors (length='+str(len(seed_list))+')...'
    write_embeddings([seed_list,asarray(seed_vectors)],output_path)

    print 'Done!'
	
if __name__ == '__main__':
    main()
