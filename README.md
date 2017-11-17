# Android Update Analysis
A project for comparing changes between Android's proprietary updates and CyanogenMod (an AOSP-based OS) updates.

## How to run
Running the code from scratch invloved multiple steps.

### 1. Mutual repositories
You should first fetch the list of mutual repositories (or subsystems) between corresponding Android and CyanogenMod versions. This can be achieved by running the [repo.py](python/repo.py) script. This script will create a csv file for each pair of Android and CyanogenMod version, containing a list of repositories and their AOSP and CyanogenMod URLs. These files will be used as the input of the Java Project.

The AOSP versions and their corresponding CyanogenMod versions are hardcoded in this script. This information was gathered from various posts from CyanogenMod's blog. Their website is not accesible anymore, hence we used the Wayback Machine:
- [CM10.1](https://web.archive.org/web/20161224214249/https://www.cyanogenmod.org/blog/cyanogenmod-10-1-m2-release)
- [CM10.2](https://web.archive.org/web/20161224211536/https://www.cyanogenmod.org/blog/cyanogenmod-10-2-0-release)
- [CM11.0](https://web.archive.org/web/20161224204349/https://www.cyanogenmod.org/blog/cm-10-2-1-maintenance-release)
- [CM12.0](https://web.archive.org/web/20161224202329/https://www.cyanogenmod.org/blog/the-l-is-for-lollipop)
- [CM12.1](https://web.archive.org/web/20161224201849/https://www.cyanogenmod.org/blog/android-security-bulletin-october-5th-update)
- [CM13.0](https://web.archive.org/web/20161224201848/https://www.cyanogenmod.org/blog/cm-13-0-release-1)
- [CM14.0](https://web.archive.org/web/20161224202317/https://www.cyanogenmod.org/blog/cm14-is-landing)

You can find the csv files we used for our ICSE 2018 paper submission [here](https://github.com/ualberta-se/icse-2018-mehran-results/blob/master/subsystem_names).

### 2. The Java script
Using the csv files produced in the previous section and the path to SourcererCC you can run the Java script. In order to do so, you should copy the csv files in the same path as the Java file, and pass the path to the SourcerCC as an argument. You should run the main function in `ca.ualberta.mehran.androidevolution.repositories.RepositoryAutomation`

#### SourcererCC
This project requires [SourcererCC](https://github.com/Mondego/SourcererCC) to run. Simply pass the path to SourcererCC as the first argument to the main function in `RepositoryAutomation` class.

When installing SourcererCC, make sure TXL is installed system-wide and run `make clean` and `make` in `parsesr/java/txl` directory. Also, make sure you delete the file `input/dataset/blocks.file`.

#### Input
This program needs the version names and URLs of the Android subsystems that should be analysed as csv files. All this information are passed to the program via csv files. The program searches its current directory for all CSV files and processes them.

Each line in the CSV file could be either a combination of three versions, a subsystem or a comment:
1. Version line begins with `versions:` and follow by three version names: Android old version, Android new version and the CyanogenMod version that is based on the old version of Android. These names correspond to tag names in Android repositories and branch names in CyanogenMod.
2. Subsystem lines consist of three parts. The first part is the name of subsystem, the second part is the URL address of the Android repository of that subsystem, and the third part is URL address of the CM repository of the subsystem.
3. Comment lines can either start with `#`, `!` or `/`.

A CSV input file example:
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
For each subsystem and each tuple of versions, a csv file will be generated in the output folder, in the current path.
#### Format
Each csv file includes information regarding the three versions (AO, AN and CM) of a subsystem. The line includes the number of method is AO, AN and CM. The last line includes the number of new methods in AN, CM and methods with same signature and different body that are added to both AN and CM; the numebr in paranthesis is the number of identical new methods between CM and AN (same signature and body). The remaining lines between the first and last line constitue the results table for methods in AO, and their status in AN and CM. They layout of the table follows the following format:

|         | Identical | Refactored | ArgumentChanged | Body-only | Deleted | SUM |
|:-------:| :-------: |:--------------:| :---------:| :--------------:| :------:|:--: |
Identical |||||||
Refactored | |  | |  |  | |
ArgumentChanged | |  |  |  | | |
Body-only | |  | |  |  | |
Deleted | |  |  |  |  | |

There might be numbers written in paranthesis for the intersections with the same change type in AN and CM. Those are the number of method that had an identical type of change, and were purged from the total number of methods in that category. The number before paranthesis does not include the number in the paranthesis, so the numbers in paranthesis can be simply ignored in most cases.

### 3. Purge result tables
Some result tables belong to non-Java repositories and should be purged. In order to do so, you can use the [purge_results.py](python/purge_results.py) file to do so. It looks for csv files in results folder in the current path, and deletes those which have zero methods in either AO, AN or CM.

### 4. Draw plots
[process_results_charts.py](python/process_results_charts.py) can be used for aggregating csv files and creating csv files sutibale for drawing plots. Like the previous script, [process_results_charts.py](python/process_results_charts.py) expects the table csv files to be in results folder in current path. 

## Team
- [Mehran Mahmoudi](https://webapps.cs.ualberta.ca/profile/) (Main contact)
- [Sarah Nadi](http://www.sarahnadi.org)
