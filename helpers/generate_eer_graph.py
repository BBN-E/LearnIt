import os,sys,json,subprocess,shutil,shlex

current_script_path = __file__
project_root = os.path.realpath(os.path.join(current_script_path, os.path.pardir))
sys.path.append(project_root)


class RunjobsPathFactory:
    def __init__(self,project_root):
        self.project_root = project_root

    @property
    def ckpts(self):
        return os.path.realpath(os.path.join(os.path.realpath(os.environ.get('RAID')),"ckpts",self.project_root))

    @property
    def etemplates(self):
        return os.path.realpath(os.path.join(os.path.realpath(os.environ.get('RAID')),"etemplates",self.project_root))

    @property
    def expts(self):
        return os.path.realpath(os.path.join(os.path.realpath(os.environ.get('RAID')),"expts",self.project_root))

    @property
    def logfiles(self):
        return os.path.realpath(os.path.join(os.path.realpath(os.environ.get('RAID')),"logfiles",self.project_root))


def read_learnit_par(learnit_root,learnit_par):
    java_class_path = os.path.realpath(os.path.join(learnit_root,'neolearnit/target/neolearnit-2.0-SNAPSHOT-jar-with-dependencies.jar'))
    l = set(os.environ.get("CLASSPATH","").split(":"))
    l.discard("")
    l.add(java_class_path)
    os.environ['CLASSPATH'] = ":".join(l)
    from jnius import autoclass
    LearnItConfig = autoclass('com.bbn.akbc.neolearnit.common.LearnItConfig')
    LearnItConfig.loadParams(autoclass('java.io.File')(learnit_par))
    return LearnItConfig

class LearnItGraphGeneratorWrapper(object):

    def clear_runjobs_dir(self):
        corpus_name = self.learnit_config.get("corpus_name")
        expt_name = "{}_decoding".format(corpus_name)
        shutil.rmtree(os.path.join(self.runjobs_path_factory.ckpts,expt_name),ignore_errors=True)
        shutil.rmtree(os.path.join(self.runjobs_path_factory.etemplates,expt_name),ignore_errors=True)
        shutil.rmtree(os.path.join(self.runjobs_path_factory.expts,expt_name),ignore_errors=True)
        shutil.rmtree(os.path.join(self.runjobs_path_factory.logfiles,expt_name),ignore_errors=True)

    def __init__(self,learnit_root,learnit_par):
        self.learnit_root = learnit_root
        self.learnit_par = learnit_par
        self.runjobs_path_factory = RunjobsPathFactory(os.path.basename(self.learnit_root))
        self.learnit_config = read_learnit_par(learnit_root,learnit_par)

    def create_file_list(self):
        source_list_dir = self.learnit_config.get("source_lists")
        docs = set()
        for file in os.listdir(source_list_dir):
            with open(os.path.join(source_list_dir,file)) as fp:
                for i in fp:
                    i = i.strip()
                    docs.add(i)
        corpus_name = self.learnit_config.get("corpus_name")
        expt_name = "{}_decoding".format(corpus_name)
        selected_doc_list_path = os.path.join(self.runjobs_path_factory.expts,expt_name)
        os.makedirs(selected_doc_list_path,exist_ok=True)
        with open(os.path.join(selected_doc_list_path,"selected.list"),'w') as wfp:
            for i in docs:
                wfp.write("{}\n".format(i))
        return os.path.join(selected_doc_list_path,"selected.list"),len(docs)

    def run_runjobs(self):
        doc_list_path,doc_list_length = self.create_file_list()
        RUNJOB_ENVIRON = os.environ.copy()
        RUNJOB_ENVIRON['PWD'] = os.path.join(self.learnit_root)
        run_serif_ins = subprocess.run(shlex.split("/opt/perl-5.20.0-x86_64/bin/perl sequences/learnit_decoding.pl --params {} --filelist {} --number_of_batches {} -local".format(self.learnit_par,doc_list_path,50)),cwd=RUNJOB_ENVIRON['PWD'],env=RUNJOB_ENVIRON,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        out,err = run_serif_ins.stdout,run_serif_ins.stderr
        return run_serif_ins

def main():
    learnit_root = os.path.realpath(os.path.join(project_root,os.pardir))
    learnit_par = os.path.join(learnit_root,'params','learnit','runs','wm_dart_101519_bootstrap.params')
    learnit_graph_generator = LearnItGraphGeneratorWrapper(learnit_root,learnit_par)
    learnit_graph_generator.clear_runjobs_dir()
    learnit_graph_generator.run_runjobs()

if __name__ == "__main__":
    main()