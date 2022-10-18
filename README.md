
<div align="center">

# qPostman

<i>A Java command line tool to download datasets from QBiC.</i>


[![Build Maven Package](https://github.com/qbicsoftware/postman-cli/actions/workflows/build-package.yml/badge.svg)](https://github.com/qbicsoftware/postman-cli/actions/workflows/build-package.yml)
[![Run Maven Tests](https://github.com/qbicsoftware/postman-cli/actions/workflows/run-tests.yml/badge.svg)](https://github.com/qbicsoftware/postman-cli/actions/workflows/run-tests.yml)
[![CodeQL](https://github.com/qbicsoftware/postman-cli/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/qbicsoftware/postman-cli/actions/workflows/codeql-analysis.yml)
[![release](https://img.shields.io/github/v/release/qbicsoftware/postman-cli?include_prereleases)](https://github.com/qbicsoftware/postman-cli/releases)

[![license](https://img.shields.io/github/license/qbicsoftware/postman-cli)](https://github.com/qbicsoftware/postman-cli/blob/master/LICENSE)
![language](https://img.shields.io/badge/language-java-blue.svg)

</div>

A client software written in Java for dataset downloads from QBiC's data management system openBIS (https://wiki-bsse.ethz.ch/display/bis/Home).

We are making use of the V3 API of openBIS (https://wiki-bsse.ethz.ch/display/openBISDoc1605/openBIS+V3+API) in order to interact with the data management system from command line, in order to provide a quick data retreaval on server or cluster resources, where the download via the qPortal is impractical.

## How to run

### Download
You can download the compiled and executable Java binaries as JAR of postman from the GitHub release page: https://github.com/qbicsoftware/postman-cli/releases.

If you want to build from source, checkout the commit of your choice and execute `mvn clean package`. We only recommend this if you are familiar with Java build systems though, as we cannot give you support here. In the normal case, the binary of a stable release is sufficient.

### Requirements
You need to have **Java JRE** or **JDK** installed (**openJDK** is fine), at least version 1.8 or 11. And the client's host must have allowance to connect to the server, which is determined by our firewall settings. If you are unsure, if your client is allowed to connect, contact us at support@qbic.zendesk.com.

### Configuration

Postman uses picocli file arguments. Therefore a config file has to be passed with the '@' prefix to its path:    
Example:
```bash
java -jar postman.jar -u <user> <sample> @path/to/config.txt 
```
The structure of the configuration file is:       <code>[-cliOption] [value]</code>   
For example: To set the ApplicationServerURL to another URL we have to use:

```properties
-as [URL]   

# Therefore to use our openbis URL we write the following line in the config file (Anything beginning with '#' is a comment):    
# Set the AS_URL (ApplicationServerURL) to the value defined below   
-as https://qbis.qbic.uni-tuebingen.de/openbis/openbis

# The following config file options are currently supported:    
# AS_URL (Application Server URL)       
-as [URL]
# DSS_URL (DataStore Server URL)     
-dss [URL]
```
A default file is provided here: [default-config](https://github.com/qbicsoftware/postman-cli/blob/development/config.txt). If no config file is provided postman uses the default values set in the PostmanCommandLineOptions class.

If no config file or commandline option is provided, Postman will resort to the defaults set here: [Defaults](https://github.com/qbicsoftware/postman-cli/blob/development/src/main/java/life/qbic/io/commandline/PostmanCommandLineOptions.java).    
Hence, the default AS is set to: `https://qbis.qbic.uni-tuebingen.de/openbis/openbis`  
and the DSS defaults to: `https://qbis.qbic.uni-tuebingen.de/datastore_server`

## How to use

### Options
Just execute postman with `java -jar postman-cli.jar` or `java -jar postman.jar -h` to get an overview of the options:
```bash

~$ java -jar postman.jar                    
Usage: <main class> [-h] [-b=<bufferMultiplier>] [-f=<filePath>]
                    [-t=<datasetType>] -u=<user> [SAMPLE_ID]...
      [SAMPLE_ID]...          one or more QBiC sample ids
  @/path/to/config.txt        config file which specifies the AS and DSS url
  -as, --as_url=<url>         AS URL 
  -dss,--dss_url=<url>        DSS URL 
  -u,  --user=<user>          openBIS user name 
  -f,  --file=<filePath>      a file with line-separated list of QBiC sample ids
  -o,  --out-dir=<outputPath> provide an existing path to change the output directory 
  -t,  --type=<datasetType>   filter for a given openBIS dataset type
  -s,  --type=<suffix>        filter for a given openBIS file suffix
  -r,  --type=<regex>         filter for a given openBIS file regex    
  -p,  --env-password=<passwordEnvVariable> 
                              provide the name of an environment variable to read in
                               the password from
  -b,  --buffer-size=<bufferMultiplier>
                              a integer muliple of 1024 bytes (default). Only
                                change this if you know what you are doing.
  -h, --help                  display a help message
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

### Download all data of a project

If you want to download all datasets for a given project id, you can use the wildcard symbol `*`:

```bash
~$ java -jar postman.jar -u <your_qbic_username> <QBiC Project ID>*
```

### Filter for file suffix

For example filter for fastq files only:

```
java -jar postman.jar -s fastq.gz -u <userid> QMFKD003AG  
```

### Filter for openBIS dataset type (recommended for advanced users)

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

### Filter for file regex

You can filter for files by a provided regex, using the `-r` option:    
Example: -r .jobscript.FastQC.*

Please note that depending on your favorite shell, you may need quote your regex. 
### Provide a path to an output directory#
If no path is provided, the requested datasets will be downloaded into the current working directory.
To change that, you can provide a path to a directory where you want your datasets to be downloaded to.
The given path already needs to exist, otherwise there will be no change to the output directory.

To use this feature, provide the output path with the -o option.

For example:

```bash
java -jar postman.jar -o /home/datasets -u <userid> <QBiC Sample ID>
java -jar postman.jar -o C:\Users\user\datasets -u <userid> <QBiC Sample ID>  
```

### Provide a file with several QBiC IDs
In order to download datasets from several samples at once, you can provide a manifest file consisting of multiple, line-separated, QBiC IDs.
Hand it to postman with the `-f` option.

For example:

```bash
java -jar postman.jar -s fastq.gz -f myids.txt -u <userid>  
```

with `myids.txt` like:

```
QTEST001AE
QTEST002BD
...
```

postman will automatically iterate over the IDs and try to download them.

### File integrity check
Postman computes the CRC32 checksum for all input streams using the native Java utility class [CRC32](https://docs.oracle.com/javase/8/docs/api/java/util/zip/CRC32.html). Postman favors [`CheckedInputStream`](https://docs.oracle.com/javase/7/docs/api/java/util/zip/CheckedInputStream.html)
over the traditional InputStream, and promotes the CRC checksum computation.

The expected CRC32 checksums are derived via the openBIS API and compared with the computed ones after the download.

Postman writes two additional summary files for that in the `logs` folder of the current working directory: `summary_valid_files.txt` and `summary_invalid_files.txt`.  
They contain the computed and expected checksum as hex string plus the file path of the recorded file:

```
// values are tab separated
<expected checksum> <computed checksum> <absolute file path>
```

In addition, Postman writes the CRC32 checksum in an additional file `<file-name-of-checked-file>.crc32` and stores it in the working directory.

