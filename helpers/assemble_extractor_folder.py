import os,sys,json,datetime,shutil

def main(input_extractor_folder,learnit_root,output_folder):
    shutil.rmtree(output_folder,ignore_errors=True)
    target_dir = os.path.join(output_folder,'inputs','targets','json')
    os.makedirs(target_dir,exist_ok=True)
    extractor_dir = os.path.join(output_folder,'inputs','extractors')
    os.makedirs(extractor_dir,exist_ok=True)
    d = datetime.datetime(2020,3,10,0,15,15)
    for f in os.listdir(input_extractor_folder):
        p = os.path.join(input_extractor_folder,f)
        extractor_type_dir = os.path.join(extractor_dir,f[:-len(".json")])
        os.makedirs(extractor_type_dir)
        shutil.copy(p,os.path.join(extractor_type_dir+"/{}".format("{}_{}.json".format(f[:-len(".json")],d.strftime("%Y%m%d%H%M%S")))))
        shutil.copy(os.path.join(learnit_root,'inputs','targets','json',"{}.json".format(f[:-len(".json")])),target_dir+"/")

if __name__ == "__main__":
    binary_extractor = ""
    learnit_root = ""
    output_folder = ""
    main(binary_extractor, learnit_root, output_folder)