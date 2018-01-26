package ca.ualberta.mehran.androidevolution.repositories;


import ca.ualberta.mehran.androidevolution.Utils;
import ca.ualberta.mehran.androidevolution.mapping.EvolutionAnalyser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;

import static ca.ualberta.mehran.androidevolution.Utils.log;
import static ca.ualberta.mehran.androidevolution.Utils.runSystemCommand;

public class RepositoryAutomation {


    private static final String SOURCERERCC_PATH = "/home/mehran/sourcerercc";

    private static final String OUTPUT_PATH = "output";
    private static final String CSV_INPUT_PATH = "input/csv";
    private static final String REPOS_PATH = "input/repos";

    private static final String VERSION_LINE_PREFIX = "versions:";

    public static void main(String[] args) {

        String sourcererCCPath = SOURCERERCC_PATH;

        if (args != null && args.length > 0) {
            sourcererCCPath = args[0];
        }

        new RepositoryAutomation().run(sourcererCCPath);
    }

    public void run(String sourcererCCPath) {

        // Project input CSV files should be copied to CSV_INPUT_PATH/PROJECT_NAME.
        for (File projectInputCSVsDir : getProjectInputCSVsPath()) {
            String projectName = projectInputCSVsDir.getName();
            File[] inputCsvFiles = getProjectInputCsvFiles(projectInputCSVsDir);

            List<Subsystem> allSubsystems = new ArrayList<>();
            StringBuilder projectStats = new StringBuilder();
            projectStats.append("Comparison versions,Number of subsystems\n");

            for (File inputCsvFile : inputCsvFiles) {
                if (!isValidInputCsvFile(inputCsvFile)) continue;

                List<PairedRepository> pairedRepositories = new ArrayList<>();
                List<ComparisonVersions> versions = new ArrayList<>();
                readInputCsvFile(inputCsvFile, pairedRepositories, versions);

                for (ComparisonVersions comparisonVersion : versions) {
                    List<Subsystem> comparisonVersionSubsystems = new ArrayList<>();
                    for (PairedRepository pairedRepository : pairedRepositories) {
                        log("Initializing " + pairedRepository + "...");
                        String repoPath = new File(REPOS_PATH, pairedRepository.name).getAbsolutePath();
                        File aospRepoPath = new File(repoPath, "aosp");
                        File proprietaryRepoPath = new File(repoPath, projectName);
                        aospRepoPath.mkdirs();
                        proprietaryRepoPath.mkdirs();

                        checkoutRepository(pairedRepository, aospRepoPath, proprietaryRepoPath);
                        List<Subsystem> repoSubsystems = getSubsystemsInRepository(pairedRepository.name, aospRepoPath, proprietaryRepoPath, comparisonVersion);
                        if (repoSubsystems == null) continue;
                        comparisonVersionSubsystems.addAll(repoSubsystems);
                    }
                    projectStats.append(comparisonVersion + "," + comparisonVersionSubsystems.size() + "\n");
                    allSubsystems.addAll(comparisonVersionSubsystems);
                }
            }
            Utils.writeToFile(OUTPUT_PATH + "/subsystems_" + projectName + ".txt", projectStats.toString());
            prepareForAnalysis(projectName, allSubsystems, sourcererCCPath);
        }

    }

    private void prepareForAnalysis(String projectName, List<Subsystem> subsystems, String sourcererCCPath) {

        EvolutionAnalyser evolutionAnalyser = new EvolutionAnalyser();
        String outputPath = new File(OUTPUT_PATH, projectName).getAbsolutePath();

        for (Subsystem subsystem : subsystems) {
            log("Preparing " + subsystem + "...");
            // TODO: Pass the repo's path via subsystem. This is hacky.
            String repoPath = new File(subsystem.aospRepoPath).getParentFile().getAbsolutePath();
            String analysisName = subsystem.name + "_" +
                    subsystem.comparisonVersions.androidOldVersion + "_" +
                    subsystem.comparisonVersions.androidNewVersion + "_" +
                    projectName + "_" + subsystem.comparisonVersions.proprietaryVersion;
            File comparisonFolderParent = new File(repoPath, analysisName);
            comparisonFolderParent.mkdir();
            ComparisionFolder comparisionFolderAoAn = new ComparisionFolder(comparisonFolderParent.getAbsolutePath(),
                    subsystem.comparisonVersions.androidOldVersion, subsystem.comparisonVersions.androidNewVersion);
            ComparisionFolder comparisionFolderAoProprietary = new ComparisionFolder(comparisonFolderParent.getAbsolutePath(),
                    subsystem.comparisonVersions.androidOldVersion, subsystem.comparisonVersions.proprietaryVersion);

            if (!gitChangeBranch(subsystem.aospRepoPath, subsystem.comparisonVersions.androidOldVersion)) continue;
            copyFolder(new File(subsystem.aospRepoPath, subsystem.relativePath).getAbsolutePath(), comparisionFolderAoAn.getOldVersionPath());
            copyFolder(new File(subsystem.aospRepoPath, subsystem.relativePath).getAbsolutePath(), comparisionFolderAoProprietary.getOldVersionPath());

            if (!gitChangeBranch(subsystem.aospRepoPath, subsystem.comparisonVersions.androidNewVersion)) continue;
            copyFolder(new File(subsystem.aospRepoPath, subsystem.relativePath).getAbsolutePath(), comparisionFolderAoAn.getNewVersionPath());

            if (!gitChangeBranch(subsystem.proprietaryRepoPath, subsystem.comparisonVersions.proprietaryVersion))
                continue;
            copyFolder(new File(subsystem.proprietaryRepoPath, subsystem.relativePath).getAbsolutePath(), comparisionFolderAoProprietary.getNewVersionPath());

            try {
                evolutionAnalyser.run(analysisName, comparisionFolderAoAn.getPath(),
                        comparisionFolderAoAn.getOldVersionPath(), comparisionFolderAoAn.getNewVersionPath(),
                        comparisionFolderAoProprietary.getPath(), comparisionFolderAoProprietary.getOldVersionPath(),
                        comparisionFolderAoProprietary.getNewVersionPath(), sourcererCCPath, outputPath);
            } catch (Throwable e) {
                log("An exception occurred while analyzing " + analysisName + ": " + e.getMessage());
                for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                    log(stackTraceElement.toString());
                }
                e.printStackTrace();
            }
        }
    }

    private void readInputCsvFile(File inputCsvFile, List<PairedRepository> pairedRepositories, List<ComparisonVersions> versions) {
        try {
            Scanner input = new Scanner(inputCsvFile);
            while (input.hasNextLine()) {
                String line = input.nextLine().trim();
                if (line.equals("") || line.startsWith("!") || line.startsWith("#") || line.startsWith("/")) {
                    continue;
                } else if (line.toLowerCase().startsWith(VERSION_LINE_PREFIX)) {
                    versions.add(new ComparisonVersions(line));
                } else if (line.split(",").length >= 3) {
                    String[] cells = line.split(",");
                    if (cells.length >= 3) {
                        PairedRepository pairedRepository = new PairedRepository(cells[0], cells[1], cells[2]);
                        pairedRepositories.add(pairedRepository);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isValidInputCsvFile(File inputCsvFile) {
        try {
            Scanner input = new Scanner(inputCsvFile);
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

    private File[] getProjectInputCSVsPath() {
        File csvInputs = new File(CSV_INPUT_PATH);
        if (!csvInputs.exists() || !csvInputs.isDirectory()) {
            throw new RuntimeException(CSV_INPUT_PATH + " doesn't exist");
        }
        return csvInputs.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
    }

    private void checkoutRepository(PairedRepository pairedRepository, File aospRepoPath, File proprietaryRepoPath) {
        gitClone(pairedRepository.androidRepositoryURL, aospRepoPath.getParentFile().getAbsolutePath(), aospRepoPath.getName());
        gitClone(pairedRepository.proprietaryRepositoryURL, proprietaryRepoPath.getParentFile().getAbsolutePath(), proprietaryRepoPath.getName());
    }

    private File[] getProjectInputCsvFiles(File projectInputCsvPath) {
        return projectInputCsvPath.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".csv");
            }
        });
    }

    private List<Subsystem> getSubsystemsInRepository(String repoName, File aospRepoPath, File proprietaryRepoPath, ComparisonVersions comparisonVersions) {
        if (!gitChangeBranch(aospRepoPath.getAbsolutePath(), comparisonVersions.androidOldVersion)) return null;
        Collection<String> manifestsInAospOld = getAndroidManifestFiles(aospRepoPath);
        if (!gitChangeBranch(aospRepoPath.getAbsolutePath(), comparisonVersions.androidNewVersion)) return null;
        Collection<String> manifestsInAospNew = getAndroidManifestFiles(aospRepoPath);
        if (!gitChangeBranch(proprietaryRepoPath.getAbsolutePath(), comparisonVersions.proprietaryVersion)) return null;
        Collection<String> manifestsInProprietary = getAndroidManifestFiles(proprietaryRepoPath);

        // Return the intersection of the tree, omit those with test and example
        List<Subsystem> result = new ArrayList<>();
        for (String aospOldManifest : manifestsInAospOld) {
            if (manifestsInAospNew.contains(aospOldManifest) && manifestsInProprietary.contains(aospOldManifest)) {
                File aospOldManifestFile = new File(aospRepoPath, aospOldManifest);
                String subsystemName = aospOldManifestFile.getParentFile().getName();
                if (subsystemName.equalsIgnoreCase("aosp")) subsystemName = repoName;
                String subsystemRelativePath = aospOldManifestFile.getParentFile().getAbsolutePath().substring(aospRepoPath.getAbsolutePath().length());

                if (subsystemRelativePath.contains("/test") ||
                        subsystemRelativePath.contains("Test") ||
                        subsystemRelativePath.toLowerCase().contains("example")) {
                    continue;
                }
                // Look for src folder
                subsystemRelativePath += "/src";
                File srcFodlerAosp = new File(aospRepoPath, subsystemRelativePath);
                if (!srcFodlerAosp.exists()) continue;
                // TODO: What about aosp new?
                File srcFodlerProprietary = new File(proprietaryRepoPath, subsystemRelativePath);
                if (!srcFodlerProprietary.exists()) continue;

                result.add(new Subsystem(subsystemName, subsystemRelativePath, aospRepoPath.getAbsolutePath(), proprietaryRepoPath.getAbsolutePath(), comparisonVersions));
            }
        }
        return result;
    }

    private Collection<String> getAndroidManifestFiles(File path) {
        Set<String> result = new HashSet<>();
        try {
            Files.walk(Paths.get(path.getAbsolutePath()))
                    .filter(pathToFile -> pathToFile.endsWith("AndroidManifest.xml"))
                    .forEach(t -> result.add(t.toString().substring(path.getAbsolutePath().length())));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void removeFolder(File file) {
        try {
            FileUtils.deleteDirectory(file);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void copyFolder(String srcPath, String dest) {
        File dirSrc = new File(srcPath);
        File dirDest = new File(dest);
        try {
            FileUtils.copyDirectory(dirSrc, dirDest);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void gitClone(String url, String path, String folderName) {
        runSystemCommand(path, true, "git", "clone", url, folderName);
    }

    private boolean gitChangeBranch(String path, String branchName) {
        String result = runSystemCommand(path, false, "git", "checkout", branchName);
        return !result.toLowerCase().contains("did not match any");
    }

    private class Subsystem {
        String name;
        String relativePath;
        String aospRepoPath;
        String proprietaryRepoPath;
        ComparisonVersions comparisonVersions;

        public Subsystem(String name, String relativePath, String aospRepoPath, String proprietaryRepoPath, ComparisonVersions comparisonVersions) {
            this.name = name;
            this.relativePath = relativePath;
            this.aospRepoPath = aospRepoPath;
            this.proprietaryRepoPath = proprietaryRepoPath;
            this.comparisonVersions = comparisonVersions;
        }

        @Override
        public String toString() {
            return name + " (" + relativePath + ") - " + comparisonVersions;
        }
    }

    private class ComparisionFolder {
        String oldVersionName;
        String newVersionName;
        String rootPath;

        ComparisionFolder(String rootPath, String oldVersionName, String newVersionName) {
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

        String getOldVersionPath() {
            return new File(getPath(), "old").getAbsolutePath();
        }

        String getNewVersionPath() {
            return new File(getPath(), "new").getAbsolutePath();
        }
    }

    private class PairedRepository {
        String name;
        String androidRepositoryURL;
        String proprietaryRepositoryURL;

        PairedRepository(String name, String androidRepositoryURL, String proprietaryRepositoryURL) {
            this.name = name;
            this.androidRepositoryURL = androidRepositoryURL;
            this.proprietaryRepositoryURL = proprietaryRepositoryURL;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private class ComparisonVersions {
        String androidOldVersion;
        String androidNewVersion;
        String proprietaryVersion;

        ComparisonVersions(String inputCsvVersionLine) {
            if (inputCsvVersionLine.startsWith(VERSION_LINE_PREFIX))
                inputCsvVersionLine = inputCsvVersionLine.substring(VERSION_LINE_PREFIX.length());
            String[] versions = inputCsvVersionLine.split(",");
            this.androidOldVersion = versions[0];
            this.androidNewVersion = versions[1];
            this.proprietaryVersion = versions[2];
        }

        @Override
        public String toString() {
            return androidOldVersion + "_" + androidNewVersion + "_" + proprietaryVersion;
        }
    }
}
