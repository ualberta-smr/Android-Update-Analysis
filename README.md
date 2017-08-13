# mehran-android-evolution
A Java project for comparing changes between Android's proprietary updates and independently modified versions of it.

## How to run
You should run the main function in `ca.ualberta.mehran.androidevolution.repositories.RepositoryAutomation`
### SourcererCC
This project requires [SourcererCC](https://github.com/Mondego/SourcererCC) to run. Simply pass the path to SourcererCC as the first argument to the main function in `RepositoryAutomation` class.

When installing SourcererCC, make sure TXL is installed system-wide and run `make clean` and `make` in `parsesr/java/txl` directory. Also, make sure you delete the file `input/dataset/blocks.file`.

### Input
This program also needs the version names and URLs of the Android subsystems that should be analysed. All this information are passed to the program via a CSV file. The program searches its current directory for all CSV files and processes them.

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

### Output
For each subsystem and each tuple of versions, a CSV file will be generated in the output folder, in the current path.
#### Format
The first line of the output consist of three numbers: Number of methods in Android old, Android new and CyanogenMod.
The last line is the number of new methods in Android new, CyanogenMod, and mutual new methods.
The lines within follow this format:


|         | Identical | Refactored | ArgumentChanged | Body-only | Deleted | SUM |
|:-------:| :-------: |:--------------:| :---------:| :--------------:| :------:|:--: |
Identical |||||||
Refactored | |  | |  |  | |
ArgumentChanged | |  |  |  | | |
Body-only | |  | |  |  | |
Deleted | |  |  |  |  | |
