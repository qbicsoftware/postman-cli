
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

We are making use of the V3 API of openBIS (https://wiki-bsse.ethz.ch/display/openBISDoc1605/openBIS+V3+API) in order to interact with the data management system from command line, in order to provide a quick data retrieval on server or cluster resources, where the download via the qPortal is impractical.

## How to run

### Download
You can download the compiled and executable Java binaries as JAR of postman from the GitHub release page: https://github.com/qbicsoftware/postman-cli/releases.

If you want to build from source, checkout the commit of your choice and execute `mvn clean package`. We only recommend this if you are familiar with Java build systems though, as we cannot give you support here. In the normal case, the binary of a stable release is sufficient.

### Requirements
You need to have **Java JRE** or **JDK** installed (**openJDK** is fine), at least version 1.8 or 11. And the client's host must have allowance to connect to the server, which is determined by our firewall settings. If you are unsure, if your client is allowed to connect, contact us at support@qbic.zendesk.com.

### Configuration

Postman uses picocli file arguments. Therefore, a config file has to be passed with the '@' prefix to its path:    
Example:
```bash
java -jar postman.jar -u <user> <sample> @path/to/config.txt 
```
The structure of the configuration file is:       <code>[-cliOption] [value]</code>   
For example: To set the ApplicationServerURL to another URL we have to use: <code>-as [URL]</code>

To use our openBIS URL we write the following lines in the config file:  
(Anything beginning with '#' is a comment)   
```

# Set the AS_URL (ApplicationServerURL) to the value defined below   
-as https://qbis.qbic.uni-tuebingen.de/openbis/openbis

# The following config file options are currently supported:    
# AS_URL (Application Server URL)       
-as <URL>
# DSS_URL (DataStore Server URL)     
-dss <URL>[,<URL>...]

```
A default file is provided here: [default-config](https://github.com/qbicsoftware/postman-cli/blob/development/config.txt). If no config file is provided, postman uses the default values set in the PostmanCommandLineOptions class.

If no config file or commandline option is provided, Postman will resort to the defaults set here: [Defaults](https://github.com/qbicsoftware/postman-cli/blob/development/src/main/java/life/qbic/io/commandline/PostmanCommandLineOptions.java).    
Hence, the default AS is set to: `https://qbis.qbic.uni-tuebingen.de/openbis/openbis`  
and the DSS defaults to: `https://qbis.qbic.uni-tuebingen.de/datastore_server` and `https://qbis.qbic.uni-tuebingen.de/datastore_server2`

## How to use

### Options
Just execute postman with `java -jar postman-cli.jar` or `java -jar postman.jar -h` to get an overview of the options available for all subcommands:
```bash

~$ java -jar postman.jar     
Usage: postman-cli [-h] [-as=<as_url>] [-dss=<dss_url>] [-f=<filePath>]
                   [-p=<passwordEnvVariable>] -u=<user> [-s=<suffix>[,
                   <suffix>...]]... [SAMPLE_ID...] [COMMAND]

Description:
A client software for dataset downloads from QBiC\'s data management system openBIS.

Parameters:
      [SAMPLE_ID...]      one or more QBiC sample ids

Options:
  -u, --user=<user>                         openBIS user name
  -p, --env-password=<passwordEnvVariable>  provide the name of an environment variable to read
                                              in the password from
  -as, -as_url=<as_url>                     ApplicationServer URL
  -dss, --dss_url=<url>[,<url>...]          DataStoreServer URLs. Specifies the 
                                              data store servers where data can be found.
  -f, --file=<filePath>                     a file with line-separated list of QBiC sample ids
  -s, --suffix=<suffix>[,<suffix>...]       only include files ending with one of these suffixes
  -h, --help                                display a help message and exit

Commands:
  download  Download data from OpenBis
  list      List all available datasets for the given identifiers with additional metadata 

Optional: specify a config file by running postman with '@/path/to/config.txt'.

```
To get only the options for one of the two subcommands, execute postman with `java -jar postman.jar download -h` or `java -jar postman.jar list -h`.
#### Provide a file with several QBiC IDs
In order to download datasets from several samples at once, you can provide a manifest file consisting of multiple, line-separated, QBiC IDs.
Hand it to postman with the `-f` option.

For example:

```bash
java -jar postman.jar download -s fastq.gz -f myids.txt -u <userid>  
```

with `myids.txt` like:

```
QTEST001AE
QTEST002BD
...
```

postman will automatically iterate over the IDs.


#### Filter for file suffix

For example filter for fastq.gz files only:

```
java -jar postman.jar download -s fastq.gz -u <userid> QMFKD003AG  
java -jar postman.jar list -s fastq.gz -u <userid> QMFKD003AG  
```

For example filter for fastq.gz and fastq files only:
```
java -jar postman.jar download -s fastq,fastq.gz -u <userid> QMFKD003AG  
java -jar postman.jar list -s fastq,fastq.gz -u <userid> QMFKD003AG  
```

#### Provide your password with an environment variable
If you do not want to provide your password manually every time, you can use the `-p` option instead.
Set a new environment variable with your password as a value. Then, you can execute postman with `-p <VARIABLE_NAME>` and postman will automatically read in your password from the environment variable.

###### Setting environment variable

**Windows**  
To set your environment variable for the current cmd session, you can use this command:
```bash  
~$ set VARIABLE_NAME=password 
```
Make sure to not put a space before and after the "=" sign.  
This command will not define a permanent environment variable. It will only be accessible during the current cmd session.
If you want to assign it permanently, you have to add it via the advanced settings of the Control Panel.   

**MacOs/Linux**  
  To set your environment variable for the current cmd session, you can use this command:
```bash  
~$ export VARIABLE_NAME=password 
```
Make sure to not put a space before and after the "=" sign.  
This command will not define a permanent environment variable. It will only be accessible during the current cmd session.
If you want to assign it permanently, you have to add the export command to your bash shells startup script.

### Subcommands 
There are two available subcommands to use: `download` and `list`.  
It is **always required** to specify one of these subcommands.

### `list`
Provide this subcommand if you only want to get information about the given samples. **Nothing will be downloaded**.

For each Sample ID, all available datasets and the files they contain will be listed as output on the terminal. 
For all files size and name are provided. Additionally, registration date, size and source of each dataset are displayed.

The easiest way to access the information about a sample is to execute postman with the subcommand `list` together with the QBiC ID for that sample and your username (same as the one you use for the qPortal):
```bash
~$ java -jar postman.jar list -u <your_qbic_username> <QBiC Sample ID>
```
Postman will prompt you for your password, which is the password from your QBiC user account.
After a successful authentication, a possible result can look like this:
```bash
[bbbfs01@u-003-ncmu03 ~]$ java -jar postman.jar list -u bbbfs01 NGSQSTTS016A8 NGSQSTTS019AW                                                                                          
Provide password for user 'bbbfs01':     

Number of datasets found for identifier NGSQSTTS016A8 : 1
# Dataset          NGSQSTTS016A8 (20211215154407692-131872)
# Source           QSTTS016A8
# Registration     2021-12-15T02:44:07Z
# Size             2.10MB
1.05MB  testfile1
1.05MB  testfile2

Number of datasets found for identifier NGSQSTTS019AW : 1
# Dataset          NGSQSTTS019AW (20211215154408961-131875)
# Source           QSTTS019AW
# Registration     2021-12-15T02:44:09Z
# Size             1.05MB
1.05MB  testfile


```
##### Dataset vs. File
The structure of samples can sometimes seem a little confusing. To clarify the difference of a dataset and a file, you can take a look at the example result above.
- A sample can contain several datasets. 
- All of these datasets can contain several files.  
&rarr; a dataset is a collection of files and does not contain any data itself.  
&rarr; the files are the ones that contain data, depending on the file format.

Another thing to think of is that samples as well as datasets can be empty.

### `Download`
The other available subcommand is `download`.
With this command, existing files will be downloaded for the provided sample IDs.

The simplest scenario is, that you want to download a dataset/datasets from a sample. Just provide the QBiC ID for that sample and your username (same as the one you use for the qPortal):
```bash
~$ java -jar postman.jar download -u <your_qbic_username> <QBiC Sample ID>
```
Postman will prompt you for your password, which is the password from your QBiC user account.

After you have provided your password and authenticate successfully, postman tries to download all datasets that are registered for that given sample ID and downloads them to the current working directory:

```bash
[bbbfs01@u-003-ncmu03 ~]$ java -jar postman.jar download -u bbbfs01 QMFKD003AG                                                                                          
Provide password for user 'bbbfs01':                                                                                                                           
                                                                                                                                                               
12:32:02.038 [main] INFO  life.qbic.App - OpenBis login returned with 0                                                                                        
12:32:02.043 [main] INFO  life.qbic.App - Connection to openBIS was successful.                                                                                
12:32:02.043 [main] INFO  life.qbic.App - 1 provided openBIS identifiers have been found: [QMFKD003AG]                                                         
12:32:02.044 [main] INFO  life.qbic.App - Downloading files for provided identifier QMFKD003AG                                                                 
12:32:02.278 [main] INFO  life.qbic.App - Number of data sets found: 2                                                                                         
12:32:02.279 [main] INFO  life.qbic.App - Initialize download ...                                                                                              
QMFKD003AG_SRR099967_1.filt.fastq.gz                                 [###                                                            ]    0.38/7.94   Gb       
```

##### Options
When using the subcommand `download`, there are further options available:
```
 -c, --conserve             Conserve the file path structure during download
 -b, --buffer-size=<int>    Dataset download performance can be improved by increasing this value with a multiple of 1024 (default). 
                            Only change this if you know what you are doing.
 -o,--output-dir=<path>     Provide an already existing path where you want your data to be downloaded to                
```
###### Download all data of a project

If you want to download all datasets for a given project id, you can use the wildcard symbol `*` and have to specify the project code inside of quotation marks:
```bash
~$ java -jar postman.jar -u <your_qbic_username> "<QBiC Project ID>*"
```

##### File integrity check
Postman computes the CRC32 checksum for all input streams using the native Java utility class [CRC32](https://docs.oracle.com/javase/8/docs/api/java/util/zip/CRC32.html). Postman favors [`CheckedInputStream`](https://docs.oracle.com/javase/7/docs/api/java/util/zip/CheckedInputStream.html)
over the traditional InputStream, and promotes the CRC checksum computation.

The expected CRC32 checksums are derived via the openBIS API and compared with the computed ones after the download.

Postman writes two additional summary files for that in the `logs` folder of the current working directory: `summary_valid_files.txt` and `summary_invalid_files.txt`.  
They contain the computed and expected checksum as hex string plus the file path of the recorded file:

```
// values are tab separated
<expected checksum> <computed checksum> <absolute file path>
```

In addition, Postman writes the CRC32 checksum in an additional file `<file-name-of-checked-file>.crc32` and stores it together with the according file.
