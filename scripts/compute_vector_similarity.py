import sys, os, gzip, codecs
from vector_utils import read_embeddings, is_zero_vector
from annoy_model import AnnoyModel

from collections import defaultdict
from enum import Enum

class ArtifactType(Enum):
    Seeds = 'seeds'
    Patterns = 'patterns'

def write_similarity_output(in_tsv_file,out_tsv_gz_file,artifact_type,embeddings,annoy_model,similarity_threshold):
    in_file = codecs.open(in_tsv_file,'r','utf-8')
    out_file = gzip.open(out_tsv_gz_file,'wb')
    count = 0
    for line in in_file:
        count+=1
        if count%1000==0:
            print "Found NNs for "+str(count)+" artifacts so far..."
        tokens = line[:-1].split('\t')[:-1] #avoid reading the count token
        if len(tokens)<1:
            continue
        artifact = tokens[0] # fine for a pattern file
        if artifact_type == ArtifactType.Seeds.value:
           if len(tokens)<2:
               continue
           artifact = (tokens[0],tokens[1])
        vector = embeddings[artifact]
        if is_zero_vector(vector):
            continue
        similarity_list = annoy_model.get_nearest_neighbors_for_vec(vector,100)
        similarity_list = [v for v in similarity_list if v[1]>=similarity_threshold]
        vector_tsv_list = [ ('\t'.join([v[0][0],v[0][1],str(v[1])]) if artifact_type==ArtifactType.Seeds.value else '\t'.join([v[0],str(v[1])]) ) for v in similarity_list]
        output_line = '\t'.join(vector_tsv_list)
        main_artifact = artifact[0]+'\t'+artifact[1] if artifact_type==ArtifactType.Seeds.value else artifact
        output_line = main_artifact+'\t'+output_line
        out_file.write(output_line.encode('utf-8')+'\n')
    in_file.close()
    out_file.close()
    return



global artifact_type
global similarity_threshold

def main():
    global artifact_type
    global similarity_threshold
    embeddings_file = sys.argv[1]
    original_artifact_lists_dir = sys.argv[2]
    output_dir = sys.argv[3]
    output_lists_dir = sys.argv[4]
    # with annoy model, we are using 100 as the hardcoded number of NN items to return (see the call to get_nearest_neighbors_for_vec method)
    # similarity_threshold is applied as additional filter on the returned NNs
    similarity_threshold = float(sys.argv[5])
    artifact_type = sys.argv[6]
    if artifact_type not in [atype.value for atype in ArtifactType]:
        raise ValueError('artifact_type must be one of '+str([atype.value for atype in ArtifactType]))

    print 'Reading the embeddings file...'
    embeddings = read_embeddings(embeddings_file)
    print 'Read '+str(len(embeddings))+' embeddings...'
    print "Building annoy model:"
    annoy_model = AnnoyModel.load_annoy_model(embeddings)

    print 'Iterating over original '+artifact_type+' files...'
    for f_name in os.listdir(original_artifact_lists_dir):
        list_file_path = os.path.join(original_artifact_lists_dir,f_name)
        print 'Reading list file for: '+list_file_path
        list_file = open(list_file_path,'r')
        batch_name = f_name
        output_list_file = open(os.path.join(output_lists_dir,batch_name),'w')
        for artifact_file_path in list_file.readlines():
            artifact_file_path = artifact_file_path[:-1]
            similarity_file_path = os.path.join(output_dir,batch_name+".tsv.gz")
            print '\tWriting similarity output for '+artifact_type+' in '+artifact_file_path+' to file '+similarity_file_path
            write_similarity_output(artifact_file_path,similarity_file_path,artifact_type,embeddings,annoy_model,similarity_threshold)
            print '\t...done writing above file.'
            output_list_file.write(similarity_file_path+"\n")
        output_list_file.close()
        print 'Written similarity list file: '+output_list_file.name
                
    print 'Done!'

if __name__ == '__main__':
    main()
