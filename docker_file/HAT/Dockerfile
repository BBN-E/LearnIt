FROM centos:7

RUN yum -y update
RUN yum -y -q reinstall glibc-common
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8
RUN yum -y install epel-release
RUN yum -y install perl git nodejs npm
RUN yum -y install perl-CPAN perl-devel perl-Digest-MD5 java-1.8.0-openjdk
RUN yum -y groupinstall Development\ Tools
RUN yum -y install curl bzip2 \
    && curl -sSL https://repo.continuum.io/miniconda/Miniconda3-latest-Linux-x86_64.sh -o /tmp/miniconda.sh \
    && bash /tmp/miniconda.sh -bfp /usr/local/ \
    && rm -rf /tmp/miniconda.sh \
    && conda install -y python=3 \
    && conda update conda \
    && conda clean --all --yes \
    && rpm -e --nodeps curl bzip2 \
    && yum clean all
RUN ln -s /usr/local /anaconda-py3 && rm -rf /home && ln -s /hat_rootfs/home /home && ln -s /hat_rootfs/hat_data /hat_data && ln -s /hat_rootfs/hat_runtime /hat_runtime && ln -s /hat_rootfs/SVN_PROJECT_ROOT_LOCAL /SVN_PROJECT_ROOT_LOCAL
ENV SVN_PROJECT_ROOT /SVN_PROJECT_ROOT_LOCAL

COPY learnit /Hume_release
RUN rm -rf /Hume_release/HAT/backend/config_default.json && ln -s /hat_data/config_default.json /Hume_release/HAT/backend/config_default.json

ENV SYSPATH $PATH
RUN /usr/local/bin/conda create -y --name customized_event python=3.6.3
ENV PATH /usr/local/envs/customized_event/bin:$SYSPATH
ENV CONDA_DEFAULT_ENV customized_event
ENV CONDA_PREFIX /usr/local/envs/customized_event
RUN /usr/local/envs/customized_event/bin/pip install -r /Hume_release/HAT/backend/requirements.txt
ENV PATH $SYSPATH

ENV SYSPATH $PATH
RUN /usr/local/bin/conda create -y --name py3-tf1 python=3.6.3 tensorflow=1.12.0 keras=2.2.4 future spacy
ENV PATH /usr/local/envs/py3-tf1/bin:$SYSPATH
ENV CONDA_DEFAULT_ENV py3-tf1
ENV CONDA_PREFIX /usr/local/envs/py3-tf1
RUN python -m spacy download en
ENV PATH $SYSPATH


WORKDIR /Hume_release/HAT/frontend
RUN npm install && npm run build
WORKDIR /


