# Changelog
## Hotfix 0.5.3 (2021-12-20)

* Bump `org.apache.logging.log4j:log4j-core:2.16.0` -> `2.17.0` (CVE-2021-45105)

## Hotfix 0.5.2 (2021-12-15)

* Bump `life.qbic:cli-parent-pom:2.2.0` -> `2.3.0`
* Bump `life.qbic:core-utils-lib:1.0.0` -> `1.6.0`
* Bump `org.apache.logging.log4j:log4j-api:2.15.0` -> `2.16.0` (CVE-2021-45046)
* Bump `org.apache.logging.log4j:log4j-core: 2.15.0` -> `2.16.0` (CVE-2021-45046)
* Bump `org.codehaus.groovy:groovy:2.5.1` -> `2.5.13`

## Hotfix 0.5.1 (2021-12-13)

* Fix severity issue CVE-2021-44228 (org.apache.logging.log4j 2.11.0 -> 2.15.0)

## Release 0.5.0 (2021-12-07)

* Introduce feature to parse the password from environment variable
* Fix issue with datasets being downloaded three times for sample codes containing an asterix

## Hotfix 0.4.7 (2021-04-12)

* Write CRC32 files again next to the downloaded data-sets


## Hotfix 0.4.6 (2021-03-26)

* qPostman now shows more user-friendly error messages when the
  authentication or server connection fails.

## Hotfix 0.4.5 (2021-03-25)

* Default dss route is now using port 443 and not 444 anymore. Port 444
  is a system reserved port that is blocked on many systems. With a
  proper proxy setup now, there is no need to go over port 444 anymore.

## Hotfix 0.4.4

* Download attempts are now configured, to try two additional times to
  download a dataset, if the attempt fails. This addresses random
  time-out exception events that have been reported by a user.

## Hotfix 0.4.3

* Fix #65 and #47

## Hotfix 0.4.2

* Fix wrong display of found files for download
* Fix wrong exit status. Postman reported to have failed, although all
  files have been downloaded successfully.

## Hotfix 0.4.1

* Fix the issue of not finding datasets when filtering by suffix

## Release 0.4.0

* Calculate CRC32 checksums for every downloaded file and compares it against the sum stored in openBIS
* Provide summary report files for valid and broken files
* Remove duplicate data set file ids

