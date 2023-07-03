
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
You need to have **Java JRE** or **JDK** installed (**openJDK** is fine), at least version 17. And the client's host must have allowance to connect to the server, which is determined by our firewall settings. If you are unsure, if your client is allowed to connect, contact us at support@qbic.zendesk.com.

### Configuration

Postman uses picocli file arguments. Therefore, a config file has to be passed with the '@' prefix to its path:    
Example:
```bash
java -jar postman.jar list -u <user> <sample> @path/to/config.txt 
```
The structure of the configuration file is:
```text
[-cliOption] [value]
```
For example: To set the ApplicationServerURL to another URL we have to use: <code>-as [URL]</code>

To use our openBIS URL we write the following lines in the config file:  
(Anything beginning with '#' is a comment)   
```
# Config file for postman-cli
# Replace the values defined after the respective CLI parameters (e.g. -u)
# with your value of choice (e.g. qbcab01 as value for -u)

--suffix .txt,.fastq,.fastq.gz
-u qbc001
--password:env MY_PASSWORD
```
A default file is provided here: [default-config](https://github.com/qbicsoftware/postman-cli/blob/development/config.txt).

## How to use

### Options
Just execute the following command to get an overview over the available options. 
```bash 
java -jar postman-cli.jar -h

Usage: postman-cli [-hV] COMMAND

Description:
A software client for downloading data from QBiC.

Options:
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.

Commands:
  download  Download data from QBiC.
  list      lists all the datasets found for the given identifiers

Optional: specify a config file by running postman with '@/path/to/config.txt'.
A detailed documentation can be found at
https://github.com/qbicsoftware/postman-cli#readme.
``` 
### How to specify your log directory
By default postman will create log files in a directory `./logs/` in your working directory.
You can specify where logs are written by setting the system property `log.path` to the desired directory.

### How to provide your credentials
After gaining access by applying through support@qbic.zendesk.com, you can log in using your credentials.
The username is provided by us or if you have a uni-tuebingen account by the university of tuebingen.

**Provide your username:**

Use the option `-u / --user` to provide us with your username.

**Provide your password:**

Please never send your password over email. We will never ask you for it!<br>
When using the application, you can either:
1. enter your password interactively `--password`
2. enter the name of a system property containing your password `--password:prop my.awesome.property`
```bash
java -jar -Dmy.awesome.property=ABCDEFG postman.jar -u qbc001a --password:prop my.awesome.property
```
3. enter the name of an environment variable containing your password `--password:env MY_PASSWORD`
```bash
MY_PASSWORD=ABCDEFG java -jar postman.jar -u qbc001a --password:env MY_PASSWORD
```
### How to provide QBiC identifiers
To specify which data you want to list or download, you need to provide us with QBiC identifiers. 
A QBiC project identifier begins with `Q` followed by four characters (`QTEST`). QBiC sample identifiers contain their project identifier.
You can provide identifiers either using the command line directly:
```bash
java -jar postman.jar QSTTSABCD01 QSTTSABCD02 QSTTSABCDE4 NGSQSTTS0012 "QTEST*"
```
Please note: When you use the `*` character to search for all files in a project, escape your identifier using quotation marks.

**Provide identifiers using a file:**

You can provide identifiers using a file containing the identifiers. Lines starting with `#` are ignored.
```text
# all files for the project
QTEST*
# all files associated to these samples
QSTTS001AB
QSTTS002BC
```
```bash
java -jar postman.jar -f myids.txt
```

### How to filter files by suffix
Both the `download` and the `list` command allow you to filter files by their suffix. The suffixes provided are not case-sensitive. 
`.TXT` and `.txt` will have the same effect.
Multiple suffixes can be provided separated by a comma. A suffix does not have to be the file ending but can be any substring from the end of a files name.

If you only want to download `fastq` and `fastq.gz` files you can run postman with 
```bash
java -jar postman.jar -s .fastq,.fastq.gz
```

## `list`
```txt
Usage: postman-cli list [-hV] [--exact-filesize] [--with-checksum]
                        [--without-header] -u=<user> [-s=<suffix>[,
                        <suffix>...]]... (--password:env=<environment-variable>
                        | --password:prop=<system-property> | --password)
                        (-f=<filePath> | SAMPLE_IDENTIFIER...)

Description:
lists all the datasets found for the given identifiers

Parameters:
      SAMPLE_IDENTIFIER...   one or more QBiC sample identifiers

Options:
      --password:env=<environment-variable>
                             provide the name of an environment variable to
                               read in your password from
      --password:prop=<system-property>
                             provide the name of a system property to read in
                               your password from
      --password             please provide your password
  -u, --user=<user>          openBIS user name
  -f, --file=<filePath>      a file with line-separated list of QBiC sample ids
  -s, --suffix=<suffix>[,<suffix>...]
                             only include files ending with one of these
                               (case-insensitive) suffixes
      --with-checksum        list the crc32 checksum for each file
      --exact-filesize       use exact byte count instead of unit suffixes:
                               Byte, Kilobyte, Megabyte, Gigabyte, Terabyte and
                               Petabyte
      --without-header       remove the header line from the output
  -h, --help                 Show this help message and exit.
  -V, --version              Print version information and exit.

Optional: specify a config file by running postman with '@/path/to/config.txt'.
A detailed documentation can be found at
https://github.com/qbicsoftware/postman-cli#readme.
```
The `list` command comes with some special options. You can change how your output looks by 
1. listing the exact number of bytes for each file `--exact-filesize`
2. removing the header from the tabular output `--without-header`
3. listing the crc32 checksum for every file `--with-checksum`

## `download`

```txt
Usage: postman-cli download [-hV] [-o=<outputPath>] -u=<user> [-s=<suffix>[,
                            <suffix>...]]... (--password:
                            env=<environment-variable> | --password:
                            prop=<system-property> | --password) (-f=<filePath>
                            | SAMPLE_IDENTIFIER...)

Description:
Download data from QBiC.

Parameters:
      SAMPLE_IDENTIFIER...   one or more QBiC sample identifiers

Options:
      --password:env=<environment-variable>
                             provide the name of an environment variable to
                               read in your password from
      --password:prop=<system-property>
                             provide the name of a system property to read in
                               your password from
      --password             please provide your password
  -u, --user=<user>          openBIS user name
  -f, --file=<filePath>      a file with line-separated list of QBiC sample ids
  -s, --suffix=<suffix>[,<suffix>...]
                             only include files ending with one of these
                               (case-insensitive) suffixes
  -o, --output-dir=<outputPath>
                             specify where to write the downloaded data
  -h, --help                 Show this help message and exit.
  -V, --version              Print version information and exit.

Optional: specify a config file by running postman with '@/path/to/config.txt'.
A detailed documentation can be found at
https://github.com/qbicsoftware/postman-cli#readme.
```
The `download` command allows you to download data from our storage to your machine. 

Use the `--output-dir` option to specify a location on your client the location will be interpreted relative to your working directory.

##### File integrity check
Postman computes the CRC32 checksum for all input streams using the native Java utility class [CRC32](https://docs.oracle.com/javase/8/docs/api/java/util/zip/CRC32.html). Postman favors [`CheckedInputStream`](https://docs.oracle.com/javase/7/docs/api/java/util/zip/CheckedInputStream.html)
over the traditional InputStream, and promotes the CRC checksum computation.

Computed CRC32 checksums are compared with CRC32 checksum stored on our servers. 
When the checksum does not match, then the download failed. Each download is attempted multiple times. 
When all attempts fail, the mismatching checksum is recorded in your log directory in the `checksum-mismatch.log` file.

The `checksum-mismatch.log` file contains one line for each file that was not downloaded.
```
// values are tab separated
<expected checksum> <computed checksum> <absolute file path>
```

In addition, Postman writes the CRC32 checksum in an additional file `<file-name-of-checked-file>.crc32` and stores it together with the according file.

#### Advanced Options
##### `posmtan`
* `-Dlog.path`: provide the log directory
* `-Dlog.level`: provide the log level to use for logging
* `--source-sample-type <sample-type>`: specify which sample type to consider as source sample type.
* `--server-timeout <millis>`: the server timeout in milli seconds

##### `download`
* `--download-attempts <download-attempts>` provide the maximal amount attempted downloads
* `--buffer-size <buffer-size>` provide a custom buffer size. Please only specify values that are a multiple of `1024`.
