[![Build Status](https://travis-ci.com/qbicsoftware/postman-cli.svg?branch=development)](https://travis-ci.com/qbicsoftware/postman-cli)[![Code Coverage](https://codecov.io/gh/qbicsoftware/postman-cli/branch/development/graph/badge.svg)](https://codecov.io/gh/qbicsoftware/postman-cli)

# postman

**Current stable version: 0.1.3 (2. July 2018)**

A client software written in Java for dataset downloads from QBiC's data management system openBIS (https://wiki-bsse.ethz.ch/display/bis/Home).

We are making use of the V3 API of openBIS (https://wiki-bsse.ethz.ch/display/openBISDoc1605/openBIS+V3+API) in order to interact with the data management system from command line, in order to provide a quick data retreaval on server or cluster resources, where the download via the qPortal is impractical.

## Download
You can download postman from the GitHub release page: https://github.com/qbicsoftware/postman-cli/releases .

## Requirements
You need to have **Java JRE** or **JDK** installed (**openJDK** is fine), at least version 1.8 or 9. And the client's host must have allowance to connect to the server, which is determined by our firewall settings. If you are unsure, if your client is allowed to connect, contact us at support@qbic.zendesk.com.

## Usage
### Options
Just execute postman with `java -jar postman-cli.jar` or `java -jar postman.jar -h` to get an overview of the options:
```bash

~$ java -jar postman.jar                    
Usage: <main class> [-h] [-b=<bufferMultiplier>] [-f=<filePath>]
                    [-t=<datasetType>] -u=<user> [SAMPLE_ID]...
      [SAMPLE_ID]...          one or more QBiC sample ids
  -b, --buffer-size=<bufferMultiplier>
                              a integer muliple of 1024 bytes (default). Only
                                change this if you know what you are doing.
  -f, --file=<filePath>       a file with line-separated list of QBiC sample ids
  -h, --help                  display a help message
  -t, --type=<datasetType>    filter for a given openBIS dataset type
  -s, --type=<suffix>         filter for a given openBIS file suffix
  -r, --type=<regex>          filter for a given openBIS file regex
  -u, --user=<user>           openBIS user name                          
```
### Provide a QBiC ID
The simplest scenario is, that you want to download a dataset/datasets from a sample. Just provide the QBiC ID for that sample and your username (same as the one you use for the qPortal):
```bash
~$ java -jar postman.jar -u <your_qbic_username> <QBiC Sample ID>
```
postman will prompt you for your password, which is the password from your QBiC user account.

After you have provided your password and authenticate successfully, postman tries to download all datasets that are registered for that given sample ID and downloads them to the current working directory:

```bash
[bbbfs01@u-003-ncmu03 ~]$ java -jar postman.jar -u bbbfs01 QMFKD003AG                                                                                          
Provide password for user 'bbbfs01':                                                                                                                           
                                                                                                                                                               
12:32:02.038 [main] INFO  life.qbic.App - OpenBis login returned with 0                                                                                        
12:32:02.043 [main] INFO  life.qbic.App - Connection to openBIS was successful.                                                                                
12:32:02.043 [main] INFO  life.qbic.App - 1 provided openBIS identifiers have been found: [QMFKD003AG]                                                         
12:32:02.044 [main] INFO  life.qbic.App - Downloading files for provided identifier QMFKD003AG                                                                 
12:32:02.278 [main] INFO  life.qbic.App - Number of data sets found: 2                                                                                         
12:32:02.279 [main] INFO  life.qbic.App - Initialize download ...                                                                                              
QMFKD003AG_SRR099967_1.filt.fastq.gz                                 [###                                                            ]    0.38/7.94   Gb       
```

### Filter for dataset type

You can filter for dataset types, using the `-t` option and one of the following openBIS dataset types we are currently using:

```bash
ARR
AUDIT
CEL
CSV
EXPERIMENTAL_DESIGN
EXPRESSION_MATRIX
FASTQ
FEATUREXML
GZ
IDXML
JPG
MAT
MZML
PDF
PNG
Q_BMI_IMAGING_DATA
Q_DOCUMENT
Q_EXT_MS_QUALITYCONTROL_RESULTS
Q_EXT_NGS_QUALITYCONTROL_RESULTS
Q_FASTA_DATA
Q_HT_QPCR_DATA
Q_MA_AGILENT_DATA
Q_MA_CHIP_IMAGE
Q_MA_RAW_DATA
Q_MS_MZML_DATA
Q_MS_RAW_DATA
Q_MTB_ARCHIVE
Q_NGS_HLATYPING_DATA
Q_NGS_IMMUNE_MONITORING_DATA
Q_NGS_IONTORRENT_DATA
Q_NGS_MAPPING_DATA
Q_NGS_MTB_DATA
Q_NGS_RAW_DATA
Q_NGS_READ_MATCH_ARCHIVE
Q_NGS_VARIANT_CALLING_DATA
Q_PEPTIDE_DATA
Q_PROJECT_DATA
Q_TEST
Q_VACCINE_CONSTRUCT_DATA
Q_WF_EDDA_BENCHMARK_LOGS
Q_WF_EDDA_BENCHMARK_RESULTS
Q_WF_MA_QUALITYCONTROL_LOGS
Q_WF_MA_QUALITYCONTROL_RESULTS
Q_WF_MS_INDIVIDUALIZED_PROTEOME_LOGS
Q_WF_MS_INDIVIDUALIZED_PROTEOME_RESULTS
Q_WF_MS_LIGANDOMICS_ID_LOGS
Q_WF_MS_LIGANDOMICS_ID_RESULTS
Q_WF_MS_LIGANDOMICS_QC_LOGS
Q_WF_MS_LIGANDOMICS_QC_RESULTS
Q_WF_MS_MAXQUANT_LOGS
Q_WF_MS_MAXQUANT_ORIGINAL_OUT
Q_WF_MS_MAXQUANT_RESULTS
Q_WF_MS_PEAKPICKING_LOGS
Q_WF_MS_PEPTIDEID_LOGS
Q_WF_MS_PEPTIDEID_RESULTS
Q_WF_MS_QUALITYCONTROL_LOGS
Q_WF_MS_QUALITYCONTROL_RESULTS
Q_WF_NGS_16S_TAXONOMIC_PROFILING_LOGS
Q_WF_NGS_EPITOPE_PREDICTION_LOGS
Q_WF_NGS_EPITOPE_PREDICTION_RESULTS
Q_WF_NGS_HLATYPING_LOGS
Q_WF_NGS_HLATYPING_RESULTS
Q_WF_NGS_MAPPING_LOGS
Q_WF_NGS_MAPPING_RESULTS
Q_WF_NGS_QUALITYCONTROL_LOGS
Q_WF_NGS_QUALITYCONTROL_RESULTS
Q_WF_NGS_RNAEXPRESSIONANALYSIS_LOGS
Q_WF_NGS_RNAEXPRESSIONANALYSIS_RESULTS
Q_WF_NGS_SHRNA_COUNTING_LOGS
Q_WF_NGS_SHRNA_COUNTING_RESULTS
Q_WF_NGS_VARIANT_ANNOTATION_LOGS
Q_WF_NGS_VARIANT_ANNOTATION_RESULTS
Q_WF_NGS_VARIANT_CALLING_LOGS
Q_WF_NGS_VARIANT_CALLING_RESULTS
RAW
SHA256SUM
TAR
UNKNOWN
VCF
```

### Filter for file suffix

You can filter for file suffixes, using the `-s` option:    
Example: -s .pdf

### Filter for file regex

You can filter for files by a provided regex, using the `-r` option:    
Example: -r .jobscript.FastQC.*

Please note that depending on your favorite shell, you may need quote your regex. 

### Provide a file with several QBiC IDs
In order to download datasets from several samples at once, you can provide a simple text file with multiple, line-separated, QBiC IDs and hand it to postman with the `-f` option.

postman will automatically iterate over the IDs and try to download them.


### Performance issues
We discovered, that a default buffer size of 1024 bytes seems not always to get all out of the performance that is possible for the dataset download. Therefore, we allow you to enter a multipler Integer value that increases the buffer size. For example a multipler of 2 will result in 2x1024 = 2048 bytes and so on.

Just use the `-b` option for that. The default buffer size remains 1024 bytes, if you don't specify this value.


