# Android Update Analysis [![Build Status](https://travis-ci.com/ualberta-smr/Android-Update-Analysis.svg?branch=master)](https://travis-ci.com/ualberta-smr/Android-Update-Analysis)
A project for comparing changes between Android's proprietary updates and other independently modified versions of it, such as the OS distributed by phone vendors or community-based variants.

The master branch contains the latest changes. You can check out the [code used for our MSR '18](https://github.com/ualberta-smr/Android-Update-Analysis/tree/msr-2018-submission) submission.


## How to run
Running the code from scratch involves multiple steps.

### 1. Mutual repositories
You should first fetch the list of mutual repositories between corresponding Android and custom system versions. This can be achieved by running the [repo.py](python/repo.py) script. This script requires [repos_config.xml](python/repos_config.xml) as in input. This file includes the list of Android-based systems you want to study ("projects"), and the URL to the manifest file that includes the list of required repositories for each version. A sample file is provided in the repo which includes only 1 project, LieangeOS, and the latest 8 versions of it. You can add additional projects to this file by adding a new `<project>` tag.

This script will create a `csv` file for each pair of Android and "project" version, containing a list of repositories and their AOSP and "project" URLs. These files will be used as the input of the Java code which performs the analysis.

Please use Python 3.x for running this script.


### 2. The analysis
The main analysis is done by the Java code available in [java](java) directory, using the `csv` files generated by [repo.py](python/repo.py). In order to do so, you should copy the `csv` files to `java/input/csv/project_name/`. project_name is the name you want to assign to this subject system.

After copying the `csv` input files, you can start the analysis by running the main method in `ca.ualberta.mehran.androidevolution.repositories.RepositoryAutomation`. You should pass the path to SourcererCC as the first argument to the main method.

#### Prerequisites
- **TXL:** Install [TXL](www.txl.ca) system-wide. It's a programming language that is used by SourcererCC.
- **SourcererCC:** A copy of SourcererCC is [included in the repo](sourcerercc). Make sure to run `make clean` and `make` in `sourcerercc/parser/java/txl` directory. Also, delete the file `sourcerercc/input/dataset/blocks.file`, if it exists. You need to pass the ABSOLUTE path to SourcererCC as the first argument to the main function in `ca.ualberta.mehran.androidevolution.repositories.RepositoryAutomation`, in order to start the analysis.
- **git:** You need to have git installed on your machine.
- **Other dependencies:** This project uses [Spoon](http://spoon.gforge.inria.fr/), [RefactoringMiner](https://github.com/tsantalis/RefactoringMiner), and [ChangeDistiller](http://www.ifi.uzh.ch/en/seal/research/tools/changeDistiller.html). All these libraries and their dependencies are included in the [lib](java/lib) directory. Make sure to include it in your Java classpath before running the code.

#### System requirements
- You need to run the Java code with JRE 1.8.
- The code will download a large number of repositories, so you need to have available storage space in your system. The exact required disk space depends on the number of projects and versions you analyze. In our experience with LineageOS, covering 8 versions, it took about 57 GBs.
- We have tested this tool on macOS and Linux. We are uncertain about its compatibility with Windows, because it uses `java.lang.Runtime.exec()` for running `git` and `java -jar` commands, which could not work as expected on Windows machines.

#### `csv` input files
This section further explains the structure of `csv` input files generated by [repo.py](python/repo.py) and required by the Java code. You do not need to know this if you want to simply run the program. 
This program needs the version names and URLs of the Android subsystems that should be analyzed as `csv` files. All this information are passed to the program via `csv` files. The program searches its current directory for all CSV files and processes them.

Each line in the CSV file could be either a combination of three versions, a subsystem or a comment:
1. Version line begins with `versions:` and follow by three version names: Android old version, Android new version and the LineageOS version that is based on the old version of Android. These names correspond to tag names in Android repositories and branch names in LineageOS.
2. Subsystem lines consist of three parts. The first part is the name of subsystem, the second part is the URL address of the Android repository of that subsystem, and the third part is URL address of the CM repository of the subsystem.
3. Comment lines can either start with `#`, `!` or `/`.

A `csv` input file example:
```
versions:android-4.2_r1,android-4.3_r1,cm-10.1
versions:android-4.3_r1,android-4.4_r1,cm-10.2
versions:android-4.4_r1,android-5.0.0_r1,cm-11.0
versions:android-5.0.0_r1,android-5.1.0_r1,cm-12.0
versions:android-5.1.0_r1,android-6.0.0_r1,cm-12.1
versions:android-6.0.0_r1,android-7.0.0_r1,cm-13.0
versions:android-7.0.0_r1,android-7.1.0_r1,cm-14.0
packages_apps_Gallery2,https://android.googlesource.com/platform/packages/apps/Gallery2,https://review.lineageos.org/LineageOS/android_packages_apps_Gallery2
development,https://android.googlesource.com/platform/development,https://review.lineageos.org/LineageOS/android_development
packages_apps_KeyChain,https://android.googlesource.com/platform/packages/apps/KeyChain,https://review.lineageos.org/LineageOS/android_packages_apps_KeyChain
#packages_providers_UserDictionaryProvider,https://android.googlesource.com/platform/packages/providers/UserDictionaryProvider,https://review.lineageos.org/LineageOS/android_packages_providers_UserDictionaryProvider
```

#### Output
For each subsystem and each tuple of versions, a `csv` file will be generated in the output folder, in the current path.

Each `csv` output file includes information regarding the three versions of a subsystem. These three versions are the old Android version (Android Old or _AO_), the new Android version that is based on _AO_ (Android New or _AN_), and a independently modified variant of the subsystem that is based on _AO_ (Modified or _MO_).

The first line includes the number of method is _AO_, _AN_ and _MO_. The last line includes the number of new methods in _AN_, _MO_ and methods with same signature and different body that are added to both _AN_ and _MO_; the number in parenthesis is the number of identical new methods between _MO_ and _AN_ (same signature and body). The remaining lines between the first and last line constitute the results table for methods in _AO_, and their status in _AN_ and _MO_. They layout of the table follows the the changesets table (Table 1) in the paper.

There might be numbers written in parenthesis for the intersections with the same change type in _AN_ and _MO_. Those are the number of method that had an identical type of change, and were purged from the total number of methods in that category. The number before parenthesis does not include the number in the parenthesis, so the numbers in parenthesis can be simply ignored in most cases.

### 3. Results
The results we used in our paper were obtained by running the Java code for LineageOS with the provided [repos_config.xml](python/repos_config.xml) file. They are available in the [results](results) directory. The Java code took 14 hours and 25 minutes to execute, on a machine with 128 GB of memory and an AMD Ryzen Threadripper 1950X 16-Core processor.

### 4. Process results & draw plots
[process_results_charts.py](python/process_results_charts.py) can be used for processing `csv` output files to generate overall stats and create suitable files for plots.

## How to Cite
If you are using this project in your research, please cite the following paper:
```
@inproceedings{AndroidUpdateProblem,
 author = {Mahmoudi, Mehran and Nadi, Sarah},
 title = {The Android Update Problem: An Empirical Study},
 booktitle = {Proceedings of the 15th International Conference on Mining Software Repositories},
 series = {MSR '18},
 year = {2018},
 isbn = {978-1-4503-5716-6},
 location = {Gothenburg, Sweden},
 pages = {220--230},
 numpages = {11},
 url = {http://doi.acm.org/10.1145/3196398.3196434},
 doi = {10.1145/3196398.3196434},
 acmid = {3196434},
 publisher = {ACM},
 address = {New York, NY, USA},
 keywords = {Android, merge conflicts, software evolution, software merging},
} 
```

## Publications
- Mehran Mahmoudi and Sarah Nadi, "[The Android Update Problem:
An Empirical Study](https://dl.acm.org/citation.cfm?id=3196434)", _15th International Conference on
Mining Software Repositories, May 28–29, 2018, Gothenburg, Sweden_

## License
This project is available under [GNU General Public License v3.0](LICENSE). Third party libraries used are licensed as follows:
 * ChangeDistiller: [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0)
 * Spoon: [CeCILL-C license - French equivalent to LGPL](https://github.com/INRIA/spoon/blob/master/LICENSE.txt)
 * SourcererCC: [GNU General Public License v3.0](https://github.com/Mondego/SourcererCC/blob/master/LICENSE)

## Team
- [Mehran Mahmoudi](https://webapps.cs.ualberta.ca/profile/?who=66938), University of Alberta (Main contact)
- [Sarah Nadi](http://www.sarahnadi.org), University of Alberta
