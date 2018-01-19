package ca.ualberta.mehran.androidevolution.repositories;


import ca.ualberta.mehran.androidevolution.mapping.EvolutionAnalyser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

import static ca.ualberta.mehran.androidevolution.Utils.runSystemCommand;

public class RepositoryAutomation {


    private static final String SOURCERERCC_PATH = "/Users/mehran/Android API/SourcererCC";

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

            List<Subsystem> subsystems = new ArrayList<>();

            for (File inputCsvFile : inputCsvFiles) {
                if (!isValidInputCsvFile(inputCsvFile)) continue;

                List<PairedRepository> pairedRepositories = new ArrayList<>();
                List<ComparisonVersions> versions = new ArrayList<>();
                readInputCsvFile(inputCsvFile, pairedRepositories, versions);

                for (ComparisonVersions comparisonVersions : versions) {
                    for (PairedRepository pairedRepository : pairedRepositories) {
                        String repoPath = new File(REPOS_PATH, pairedRepository.name).getAbsolutePath();
                        File aospRepoPath = new File(repoPath, "aosp");
                        File proprietaryRepoPath = new File(repoPath, projectName);
                        aospRepoPath.mkdirs();
                        proprietaryRepoPath.mkdirs();

                        checkoutRepository(pairedRepository, aospRepoPath, proprietaryRepoPath);
                        List<Subsystem> repoSubsystems = getSubsystemsInRepository(pairedRepository.name, aospRepoPath, proprietaryRepoPath, comparisonVersions);
                        if (repoSubsystems == null) continue;
                        subsystems.addAll(repoSubsystems);
                    }
                }
            }
            prepareForAnalysis(projectName, subsystems, sourcererCCPath);
        }


//        // All CSV files in the current path
//        File[] versionAndRepositoryFiles = getVersionAndRepositoryFiles();
//
//        new File(OUTPUT_PATH).mkdir();
//
//        for (File versionAndRepositoryFile : versionAndRepositoryFiles) {
//            if (!isValidInputCsvFile(versionAndRepositoryFile)) continue;
//
//            List<PairedRepository> repositories = new ArrayList<>();
//            List<String> versions = new ArrayList<>();
//            readInputCsvFile(versionAndRepositoryFile, repositories, versions);
//
//            EvolutionAnalyser evolutionAnalyser = new EvolutionAnalyser();
//
//            for (PairedRepository repository : repositories) {
//                File repositoryDir = new File(OUTPUT_PATH, repository.name);
//                repositoryDir.mkdir();
//
//                String androidRawFolderName = "android_raw";
//                String proprietaryRawFolderName = "proprietary_raw";
//                gitClone(repository.androidRepositoryURL, repositoryDir.getAbsolutePath(), androidRawFolderName);
//                gitClone(repository.proprietaryRepositoryURL, repositoryDir.getAbsolutePath(), proprietaryRawFolderName);
//
//                File androidRawFolder = new File(repositoryDir, androidRawFolderName);
//                File proprietaryRawFolder = new File(repositoryDir, proprietaryRawFolderName);
//
//                for (String comparisonPath : repository.subsystemPaths) {
//                    for (int i = 0; i < versions.size(); i++) {
//                        String androidBaseVersion = versions.get(i).split(",")[0];
//                        String androidNewVersion = versions.get(i).split(",")[1];
//                        String proprietaryVersion = versions.get(i).split(",")[2];
//
//                        String analysisPrefix = comparisonPath.equals("src") ? repository.name : comparisonPath.replace("/", "_");
//                        String analysisName = analysisPrefix + "_" + androidBaseVersion + "_" + androidNewVersion + "_" + proprietaryVersion;
//                        log("Doing " + analysisName);
//
//                        ComparisionFolder androidOldNew = new ComparisionFolder(repositoryDir.getAbsolutePath(), androidBaseVersion, androidNewVersion);
//                        ComparisionFolder androidOldProprietary = new ComparisionFolder(repositoryDir.getAbsolutePath(), androidBaseVersion, proprietaryVersion);
//
//                        if (!gitChangeBranch(androidRawFolder.getAbsolutePath(), androidBaseVersion)) continue;
//                        File androidSrcFolder = new File(androidRawFolder, comparisonPath);
//                        if (!androidSrcFolder.exists()) {
//                            log("No " + comparisonPath + " folder for " + androidRawFolder.getAbsolutePath());
//                            continue;
//                        }
//                        copyFolder(androidRawFolder.getAbsolutePath(), comparisonPath, androidOldNew.getOldVersionPath() + "/");
//                        copyFolder(androidRawFolder.getAbsolutePath(), comparisonPath, androidOldProprietary.getOldVersionPath() + "/");
//
//                        if (!gitChangeBranch(androidRawFolder.getAbsolutePath(), androidNewVersion)) continue;
//                        androidSrcFolder = new File(androidRawFolder, comparisonPath);
//                        if (!androidSrcFolder.exists()) {
//                            log("No " + comparisonPath + " folder for " + androidRawFolder.getAbsolutePath());
//                            continue;
//                        }
//                        copyFolder(androidRawFolder.getAbsolutePath(), comparisonPath, androidOldNew.getNewVersionPath() + "/");
//
//                        if (!gitChangeBranch(proprietaryRawFolder.getAbsolutePath(), proprietaryVersion)) continue;
//                        File proprietarySrcFolder = new File(proprietaryRawFolder, comparisonPath);
//                        if (!proprietarySrcFolder.exists()) {
//                            log("No " + comparisonPath + " folder for " + proprietarySrcFolder.getAbsolutePath());
//                            continue;
//                        }
//                        copyFolder(proprietaryRawFolder.getAbsolutePath(), comparisonPath, androidOldProprietary.getNewVersionPath() + "/");
//
//                        try {
//                            evolutionAnalyser.run(analysisName, androidOldNew.getPath(), androidOldNew.getOldVersionPath(), androidOldNew.getNewVersionPath(),
//                                    androidOldProprietary.getPath(), androidOldProprietary.getOldVersionPath(), androidOldProprietary.getNewVersionPath(), sourcererCCPath, OUTPUT_PATH);
//                        } catch (Throwable e) {
//                            e.printStackTrace();
//                        }
////                        removeFolder(new File(androidOldNew.getPath()));
////                        removeFolder(new File(androidOldProprietary.getPath()));
//                    }
//                }
//            }
//        }

    }

    private void prepareForAnalysis(String projectName, List<Subsystem> subsystems, String sourcererCCPath) {

        EvolutionAnalyser evolutionAnalyser = new EvolutionAnalyser();
        String outputPath = new File(OUTPUT_PATH, projectName).getAbsolutePath();

        for (Subsystem subsystem : subsystems) {
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

            evolutionAnalyser.run(analysisName, comparisionFolderAoAn.getPath(),
                    comparisionFolderAoAn.getOldVersionPath(), comparisionFolderAoAn.getNewVersionPath(),
                    comparisionFolderAoProprietary.getPath(), comparisionFolderAoProprietary.getOldVersionPath(),
                    comparisionFolderAoProprietary.getNewVersionPath(), sourcererCCPath, outputPath);
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
//                        if (cells.length > 3) {
//                            pairedRepository.subsystemPaths.clear();
//                            for (int j = 3; j < cells.length; j++) {
//                                pairedRepository.addSubsystemPath(cells[j]);
//                            }
//                        }
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

                // TODO: Filter out tests and examples
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
        Collection<File> xmlFiles = FileUtils.listFiles(path, new IOFileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().equalsIgnoreCase("AndroidManifest.xml");
            }

            @Override
            public boolean accept(File file, String s) {
                return s.equalsIgnoreCase("AndroidManifest.xml");
            }
        }, null);
//        if (xmlFiles != null) {
//            File[] results = new File[xmlFiles.size()];
//            Iterator<File> iterator = xmlFiles.iterator();
//            for (int i = 0; i < xmlFiles.size() && iterator.hasNext(); i++) {
//                results[i] = iterator.next();
//            }
//            return results;
//        }
        Set<String> result = new HashSet<>();
        for (File xmlFile : xmlFiles) {
            String absolutePath = xmlFile.getAbsolutePath();
            result.add(absolutePath.substring(path.getAbsolutePath().length(), absolutePath.length()));
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
    }

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

    private class PairedRepository {
        String name;
        String androidRepositoryURL;
        String proprietaryRepositoryURL;
//        ArrayList<String> subsystemPaths;

        public PairedRepository(String name, String androidRepositoryURL, String proprietaryRepositoryURL) {
            this.name = name;
            this.androidRepositoryURL = androidRepositoryURL;
            this.proprietaryRepositoryURL = proprietaryRepositoryURL;
//            subsystemPaths = new ArrayList<>();
        }

//        public void addSubsystemPath(String path) {
//            subsystemPaths.add(path);
//        }
    }

    private class ComparisonVersions {
        String androidOldVersion;
        String androidNewVersion;
        String proprietaryVersion;

        public ComparisonVersions(String inputCsvVersionLine) {
            if (inputCsvVersionLine.startsWith(VERSION_LINE_PREFIX))
                inputCsvVersionLine = inputCsvVersionLine.substring(VERSION_LINE_PREFIX.length());
            String[] versions = inputCsvVersionLine.split(",");
            this.androidOldVersion = versions[0];
            this.androidNewVersion = versions[1];
            this.proprietaryVersion = versions[2];
        }
    }
}
