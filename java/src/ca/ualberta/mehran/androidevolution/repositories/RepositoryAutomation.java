package ca.ualberta.mehran.androidevolution.repositories;


import ca.ualberta.mehran.androidevolution.mapping.EvolutionAnalyser;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static ca.ualberta.mehran.androidevolution.Utils.log;
import static ca.ualberta.mehran.androidevolution.Utils.runSystemCommand;

public class RepositoryAutomation {


    private static final String SOURCERERCC_PATH = "/home/mehran/sourcerercc";


    private static final String OUTPUT_PATH = "output";
    private static final String VERSION_LINE_PREFIX = "versions:";

    public static void main(String[] args) {

        String sourcererCCPath = SOURCERERCC_PATH;

        if (args != null && args.length > 0) {
            sourcererCCPath = args[0];
        }

        new RepositoryAutomation().run(sourcererCCPath);
    }

    public void run(String sourcererCCPath) {

        // All CSV files in the current path
        File[] versionAndSubsystemsFiles = getVersionAndSubsystemsFiles();

        new File(OUTPUT_PATH).mkdir();

        for (File versionAndSubsystemsFile : versionAndSubsystemsFiles) {
            if (!isValidVersionsAndSubsystemFile(versionAndSubsystemsFile)) continue;

            List<SubSystem> subsystems = new ArrayList<>();
            List<String> versions = new ArrayList<>();
            readVersionsAndSubsystemsFile(versionAndSubsystemsFile, subsystems, versions);

            EvolutionAnalyser evolutionAnalyser = new EvolutionAnalyser();

            for (SubSystem subsystem : subsystems) {
                File subsystemDir = new File(OUTPUT_PATH, subsystem.name);
                subsystemDir.mkdir();

                String androidRawFolderName = "android_raw";
                String CMRawFolderName = "cm_raw";
                gitClone(subsystem.androidRepositoryURL, subsystemDir.getAbsolutePath(), androidRawFolderName);
                gitClone(subsystem.CMRepositoryURL, subsystemDir.getAbsolutePath(), CMRawFolderName);

                File androidRawFolder = new File(subsystemDir, androidRawFolderName);
                File CMRawFolder = new File(subsystemDir, CMRawFolderName);

                for (String comparisonPath : subsystem.comparisonPaths) {
                    for (int i = 0; i < versions.size(); i++) {
                        String androidBaseVersion = versions.get(i).split(",")[0];
                        String androidNewVersion = versions.get(i).split(",")[1];
                        String CMVersion = versions.get(i).split(",")[2];

                        String analysisPrefix = comparisonPath.equals("src") ? subsystem.name : comparisonPath.replace("/", "_");
                        String analysisName = analysisPrefix + "_" + androidBaseVersion + "_" + androidNewVersion + "_" + CMVersion;
                        log("Doing " + analysisName);

                        ComparisionFolder androidOldNew = new ComparisionFolder(subsystemDir.getAbsolutePath(), androidBaseVersion, androidNewVersion);
                        ComparisionFolder androidOldCM = new ComparisionFolder(subsystemDir.getAbsolutePath(), androidBaseVersion, CMVersion);

                        if (!gitChangeBranch(androidRawFolder.getAbsolutePath(), androidBaseVersion)) continue;
                        File androidSrcFolder = new File(androidRawFolder, comparisonPath);
                        if (!androidSrcFolder.exists()) {
                            log("No " + comparisonPath + " folder for " + androidRawFolder.getAbsolutePath());
                            continue;
                        }
                        copyFolder(androidRawFolder.getAbsolutePath(), comparisonPath, androidOldNew.getOldVersionPath() + "/");
                        copyFolder(androidRawFolder.getAbsolutePath(), comparisonPath, androidOldCM.getOldVersionPath() + "/");

                        if (!gitChangeBranch(androidRawFolder.getAbsolutePath(), androidNewVersion)) continue;
                        androidSrcFolder = new File(androidRawFolder, comparisonPath);
                        if (!androidSrcFolder.exists()) {
                            log("No " + comparisonPath + " folder for " + androidRawFolder.getAbsolutePath());
                            continue;
                        }
                        copyFolder(androidRawFolder.getAbsolutePath(), comparisonPath, androidOldNew.getNewVersionPath() + "/");

                        if (!gitChangeBranch(CMRawFolder.getAbsolutePath(), CMVersion)) continue;
                        File CMSrcFolder = new File(CMRawFolder, comparisonPath);
                        if (!CMSrcFolder.exists()) {
                            log("No " + comparisonPath + " folder for " + CMSrcFolder.getAbsolutePath());
                            continue;
                        }
                        copyFolder(CMRawFolder.getAbsolutePath(), comparisonPath, androidOldCM.getNewVersionPath() + "/");

                        try {
                            evolutionAnalyser.run(analysisName, androidOldNew.getPath(), androidOldNew.getOldVersionPath(), androidOldNew.getNewVersionPath(),
                                    androidOldCM.getPath(), androidOldCM.getOldVersionPath(), androidOldCM.getNewVersionPath(), sourcererCCPath, OUTPUT_PATH);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
//                        removeFolder(new File(androidOldNew.getPath()));
//                        removeFolder(new File(androidOldCM.getPath()));
                    }
                }
            }
        }

    }

    private void readVersionsAndSubsystemsFile(File versionAndSubsystemsFile, List<SubSystem> subsystems, List<String> versions) {
        try {
            Scanner input = new Scanner(versionAndSubsystemsFile);
            while (input.hasNextLine()) {
                String line = input.nextLine().trim();
                if (line.equals("") || line.startsWith("!") || line.startsWith("#") || line.startsWith("/")) {
                    continue;
                } else if (line.toLowerCase().startsWith(VERSION_LINE_PREFIX)) {
                    versions.add(line.substring(VERSION_LINE_PREFIX.length()));
                } else if (line.split(",").length >= 3) {
                    String[] cells = line.split(",");
                    if (cells.length >= 3) {
                        SubSystem subsystem = new SubSystem(cells[0], cells[1], cells[2]);
                        if (cells.length > 3) {
                            subsystem.comparisonPaths.clear();
                            for (int j = 3; j < cells.length; j++) {
                                subsystem.addExtraComparisonPath(cells[j]);
                            }
                        }
                        subsystems.add(subsystem);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isValidVersionsAndSubsystemFile(File versionAndSubsystemsFile) {
        try {
            Scanner input = new Scanner(versionAndSubsystemsFile);
            while (input.hasNextLine()) {
                String line = input.nextLine().trim();
                if (line.equals("") || line.startsWith("!") || line.startsWith("#") || line.startsWith("/")) {
                    continue;
                }
                if (line.toLowerCase().startsWith(VERSION_LINE_PREFIX)) {
                    continue;
                }
                if (line.split(",").length >= 3) {
                    continue;
                }
                return false;
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private File[] getVersionAndSubsystemsFiles() {
        File file = new File(".");
        return file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".csv");
            }
        });
    }

    private void removeFolder(File file) {
        try {
            FileUtils.deleteDirectory(file);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void copyFolder(String srcPath, String fileName, String dest) {
        File dirSrc = new File(srcPath, fileName);
        File dirDest = new File(dest);
//        removeFolder(dirDest);
        try {
            FileUtils.copyDirectory(dirSrc, dirDest);
        } catch (Throwable e) {
            e.printStackTrace();
        }
//        runSystemCommand(srcPath, false, "cp", "-r", fileName, dest);
    }

    private void gitClone(String url, String path, String folderName) {
        runSystemCommand(path, true, "git", "clone", url, folderName);
    }

    private boolean gitChangeBranch(String path, String branchName) {
        String result = runSystemCommand(path, false, "git", "checkout", branchName);
        return !result.toLowerCase().contains("did not match any");
    }

//    private String[] readFile(String path) {
//        try {
//            File file = new File(path);
//            Scanner input = new Scanner(file);
//            List<String> lines = new ArrayList<>();
//            while (input.hasNextLine()) {
//                lines.add(input.nextLine());
//            }
//            String[] result = new String[lines.size()];
//            for (int i = 0; i < result.length; i++) {
//                result[i] = lines.get(i);
//            }
//            input.close();
//            return result;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return new String[]{};
//    }

    private class ComparisionFolder {
        String oldVersionName;
        String newVersionName;
        String rootPath;

        public ComparisionFolder(String rootPath, String oldVersionName, String newVersionName) {
            this.oldVersionName = oldVersionName;
            this.newVersionName = newVersionName;
            this.rootPath = rootPath;
            File folder = new File(getPath());
            if (!folder.exists()) {
                folder.mkdir();
            }
            File oldVersionFolder = new File(getOldVersionPath());
            if (!oldVersionFolder.exists()) {
                oldVersionFolder.mkdir();
            }
            File newVersionFolder = new File(getNewVersionPath());
            if (!newVersionFolder.exists()) {
                newVersionFolder.mkdir();
            }
        }


        public String getName() {
            return oldVersionName + "_" + newVersionName;
        }

        public String getPath() {
            return new File(rootPath, getName()).getAbsolutePath();
        }

        public String getOldVersionPath() {
            return new File(getPath(), "old").getAbsolutePath();
        }

        public String getNewVersionPath() {
            return new File(getPath(), "new").getAbsolutePath();
        }
    }

    private class SubSystem {
        String name;
        String androidRepositoryURL;
        String CMRepositoryURL;
        ArrayList<String> comparisonPaths;

        public SubSystem(String name, String androidRepositoryURL, String CMRepositoryURL) {
            this.name = name;
            this.androidRepositoryURL = androidRepositoryURL;
            this.CMRepositoryURL = CMRepositoryURL;
            comparisonPaths = new ArrayList<>();
            comparisonPaths.add("src");
        }

        public void addExtraComparisonPath(String path) {
            comparisonPaths.add(path);
        }

        public boolean hasExtraComparisonPath() {
            return comparisonPaths != null && comparisonPaths.size() > 0;
        }
    }
}
