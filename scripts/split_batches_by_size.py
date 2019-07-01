import os, sys

root = sys.argv[1]
#cutoff = int(sys.argv[2])*1024*1024
num_batches = int(sys.argv[2])
out_dir = sys.argv[3]
limit_cutoff = len(sys.argv) > 4 and int(sys.argv[4]) == 1

total_size = 0
print "Finding average size for %d batches..."%num_batches
for f in os.listdir(root):
    total_size += os.path.getsize(os.path.join(root,f))
cutoff = total_size/num_batches
if limit_cutoff and cutoff > 83886080L: #80MB
    print "Batch cutoff too big at %dM"%(cutoff/1024/1024)
    cutoff = 83886080L
print "Batch cutoff set to %dM"%(cutoff/1024/1024)

batches = []
curr_batch = []
curr_size = 0

print "Collecting batches..."
for f in os.listdir(root):
    size = os.path.getsize(os.path.join(root,f))
    curr_batch.append(os.path.join(root,f))
    curr_size += size
    if curr_size >= cutoff:
        batches.append(curr_batch)
        curr_batch = []
        curr_size = 0L
if curr_batch:
    batches.append(curr_batch)
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
