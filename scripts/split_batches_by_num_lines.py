import os, sys, codecs

root = sys.argv[1]
#cutoff = int(sys.argv[2])*1024*1024
num_batches = int(sys.argv[2])
out_dir = sys.argv[3]
limit_cutoff = len(sys.argv) > 4 and int(sys.argv[4]) == 1

total_size = 0
print "Finding average number of lines for %d batches..."%num_batches
for f in os.listdir(root):
    fp = codecs.open(os.path.join(root,f),"r","utf-8") 
    total_size += len(fp.readlines())
    fp.close()
cutoff = total_size/num_batches
#if limit_cutoff and cutoff > 83886080L: #80MB
    #print "Batch cutoff too big at %dM"%(cutoff/1024/1024)
    #cutoff = 83886080L
print "Batch cutoff set to %d lines"%(cutoff)

batches = []
curr_batch = []
curr_size = 0

print "Collecting batches..."
for f in os.listdir(root):
    fp = codecs.open(os.path.join(root,f),"r","utf-8") 
    num_lines = len(fp.readlines())
    fp.close()
    curr_batch.append(os.path.join(root,f))
    curr_size += num_lines
    if curr_size >= cutoff:
        batches.append(curr_batch)
        curr_batch = []
        curr_size = 0L
print "Created %d batches"%len(batches)

print "Writing out batches to %s..."%out_dir
if not os.path.exists(out_dir):
    os.mkdir(out_dir)
batch_num = 0
for batch in batches:
    out = file(os.path.join(out_dir,'batch_%d'%batch_num),'w')
    for f in batch:
        out.write(f+'\n')
    out.close()
    batch_num += 1
print "Done!"
