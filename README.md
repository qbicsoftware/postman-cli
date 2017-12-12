[![Build Status](https://qbic-intranet.am10.uni-tuebingen.de/jenkins/job/qPostMan-development/badge/icon)](https://qbic-intranet.am10.uni-tuebingen.de/jenkins/job/qPostMan-development/)

# qPostMan
A client software written in Java for dataset downloads from QBiC's data management system openBIS (https://wiki-bsse.ethz.ch/display/bis/Home).

We are making use of the V3 API of openBIS (https://wiki-bsse.ethz.ch/display/openBISDoc1605/openBIS+V3+API) in order to interact with the data management system from command line, in order to provide a quick data retreaval on server or cluster resources, where the download via the qPortal is impractical.

## Download
You can download qPostMan from our repositoy with i.e. `wget`:
```bash
~$ wget https://qbic-repo.am10.uni-tuebingen.de/repository/maven-releases/life/qbic/qpostman/0.1.2.1/qpostman-0.1.2.1-jar-with-dependencies.jar
~$ wget https://qbic-repo.am10.uni-tuebingen.de/repository/maven-releases/life/qbic/qpostman/0.1.2.1/qpostman-0.1.2.1-jar-with-dependencies.jar.md5
```
Please compare the md5 checksum after the download.

## Requirements
You need to have **Java JRE** or **JDK** installed (**openJDK** is fine), at least version 1.8 or 9. And the client's host must have allowance to connect to the server, which is determined by our firewall settings.

## Usage
### Options
Just execute qPostMan with `java -jar qpostman.jar` or `java -jar qpostman.jar -h` to get an overview of the options:
```bash

~$ java -jar qpostman.jar                    
usage: qPostMan [-b <arg>] [-f <arg>] [-h] [-i <arg>] [-u <arg>]                      
 -b,--buffer-size <arg>   A integer muliple of 1024 bytes (default). Only             
                          change this if you know what you are doing.                 
 -f,--file <arg>          File containing openBis sample IDs (one per                 
                          line) [mutually exclusive]                                  
 -h,--help                Print this help                                             
 -i,--identifier <arg>    openBis sample ID [mutually exclusive]                      
 -u,--user-name <arg>     openBIS user name                                           
```
### Provide a QBiC ID
The simplest scenario is, that you want to download a dataset/datasets from a sample. Just provide the QBiC ID for that sample and your username (same as the one you use for the qPortal):
```bash
~$ java -jar qpostman.jar -i <QBiC ID> -u <your_qbic_username>
```
qPostMan will prompt you for your password, which is the password from your QBiC user account.

After you have provided your password and authenticate successfully, qPostMan tries to download all datasets that are registered for that given sample ID and downloads them to the current working directory:

```bash
[bbbfs01@u-003-ncmu03 ~]$ qpostman-0.1.2.1 -i QMFKD003AG -u bbbfs01                                                                                            
Provide password for user 'bbbfs01':                                                                                                                           
                                                                                                                                                               
12:32:02.038 [main] INFO  life.qbic.App - OpenBis login returned with 0                                                                                        
12:32:02.043 [main] INFO  life.qbic.App - Connection to openBIS was successful.                                                                                
12:32:02.043 [main] INFO  life.qbic.App - 1 provided openBIS identifiers have been found: [QMFKD003AG]                                                         
12:32:02.044 [main] INFO  life.qbic.App - Downloading files for provided identifier QMFKD003AG                                                                 
12:32:02.278 [main] INFO  life.qbic.App - Number of data sets found: 2                                                                                         
12:32:02.279 [main] INFO  life.qbic.App - Initialize download ...                                                                                              
QMFKD003AG_SRR099967_1.filt.fastq.gz                                 [###                                                            ]    0.38/7.94   Gb       
```


