[![Build Status](https://qbic-intranet.am10.uni-tuebingen.de/jenkins/job/qPostMan-development/badge/icon)](https://qbic-intranet.am10.uni-tuebingen.de/jenkins/job/qPostMan-development/)

# qPostMan
A client software written in Java for dataset downloads from QBiC's data management system openBIS (https://wiki-bsse.ethz.ch/display/bis/Home).

We are making use of the V3 API of openBIS (https://wiki-bsse.ethz.ch/display/openBISDoc1605/openBIS+V3+API) in order to interact with the data management system from command line, in order to provide a quick data retreaval on server or cluster resources, where the download via the qPortal is impractical.

## Usage

```bash

~$ java -jar qpostman.jar                                                                                                       usage: qPostMan [-f <arg>] [-h] [-i <arg>] [-u <arg>]                                   
 -f,--file <arg>         File containing openBis sample IDs (one per line)              
                         [mutually exclusive]                                           
 -h,--help               Print this help                                                
 -i,--identifier <arg>   openBis sample ID [mutually exclusive]                         
 -u,--user-name <arg>    openBIS user name                                              

```



