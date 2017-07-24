package ca.ualberta.mehran.androidevolution.repositories;


import ca.ualberta.mehran.androidevolution.mapping.EvolutionAnalyser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static ca.ualberta.mehran.androidevolution.Utils.log;
import static ca.ualberta.mehran.androidevolution.Utils.runSystemCommand;

public class RepositoryAutomation {


    private static final String SOURCERERCC_PATH = "/Users/mehran/Android API/SourcererCC";

    private static final String VERSIONS_FILE = "versions.csv";
    private static final String SUBSYSTEMS_FILE = "subsystems.csv";

    public static void main(String[] args) {

        String sourcererCCPath = SOURCERERCC_PATH;

        if (args != null && args.length > 0) {
            sourcererCCPath = args[0];
        }

        new RepositoryAutomation().run(sourcererCCPath);
    }

    public void run(String sourcererCCPath) {
        String[] versions = readFile(VERSIONS_FILE);
        List<SubSystem> subsystems = getSubsystems(SUBSYSTEMS_FILE);

        EvolutionAnalyser evolutionAnalyser = new EvolutionAnalyser();

        for (SubSystem subsystem : subsystems) {
            File subsystemDir = new File(subsystem.name);
            subsystemDir.mkdir();

            String androidRawFolderName = "android_raw";
            String CMRawFolderName = "cm_raw";
            gitClone(subsystem.androidRepositoryURL, subsystemDir.getAbsolutePath(), androidRawFolderName);
            gitClone(subsystem.CMRepositoryURL, subsystemDir.getAbsolutePath(), CMRawFolderName);

            File androidRawFolder = new File(subsystemDir, androidRawFolderName);
            File CMRawFolder = new File(subsystemDir, CMRawFolderName);


            for (int i = 0; i < versions.length - 1; i++) {
                String androidBaseVersion = versions[i].split(",")[0];
                String androidNewVersion = versions[i + 1].split(",")[0];
                String CMVersion = versions[i].split(",")[1];

                String analysisName = subsystem.name + "_" + androidBaseVersion + "_" + androidNewVersion + "_" + CMVersion;
                log("Doing " + analysisName);

                ComparisionFolder androidOldNew = new ComparisionFolder(subsystemDir.getAbsolutePath(), androidBaseVersion, androidNewVersion);
                ComparisionFolder androidOldCM = new ComparisionFolder(subsystemDir.getAbsolutePath(), androidBaseVersion, CMVersion);

                if (!gitChangeBranch(androidRawFolder.getAbsolutePath(), androidBaseVersion)) continue;
                File androidSrcFolder = new File(androidRawFolder, "src");
                if (!androidSrcFolder.exists()) {
                    log("No src folder for " + androidRawFolder.getAbsolutePath());
                    continue;
                }
                copyFolder(androidRawFolder.getAbsolutePath(), "src", androidOldNew.getOldVersionPath() + "/");
                copyFolder(androidRawFolder.getAbsolutePath(), "src", androidOldCM.getOldVersionPath() + "/");

                if (!gitChangeBranch(androidRawFolder.getAbsolutePath(), androidNewVersion)) continue;
                androidSrcFolder = new File(androidRawFolder, "src");
                if (!androidSrcFolder.exists()) {
                    log("No src folder for " + androidRawFolder.getAbsolutePath());
                    continue;
                }
                copyFolder(androidRawFolder.getAbsolutePath(), "src", androidOldNew.getNewVersionPath() + "/");

                if (!gitChangeBranch(CMRawFolder.getAbsolutePath(), CMVersion)) continue;
                File CMSrcFolder = new File(CMRawFolder, "src");
                if (!CMSrcFolder.exists()) {
                    log("No src folder for " + CMSrcFolder.getAbsolutePath());
                    continue;
                }
                copyFolder(CMRawFolder.getAbsolutePath(), "src", androidOldCM.getNewVersionPath() + "/");

                try {
                    evolutionAnalyser.run(analysisName, androidOldNew.getPath(), androidOldNew.getOldVersionPath(), androidOldNew.getNewVersionPath(),
                            androidOldCM.getPath(), androidOldCM.getOldVersionPath(), androidOldCM.getNewVersionPath(), sourcererCCPath);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }


    }

    private void copyFolder(String srcPath, String fileName, String dest) {
        runSystemCommand(srcPath, false, "cp", "-r", fileName, dest);
    }

    private void gitClone(String url, String path, String folderName) {
        runSystemCommand(path, true, "git", "clone", url, folderName);
    }

    private boolean gitChangeBranch(String path, String branchName) {
        String result = runSystemCommand(path, false, "git", "checkout", branchName);
        return !result.toLowerCase().contains("did not match any");
    }


    private List<SubSystem> getSubsystems(String subsystemFilePath) {
        String[] subsystemsLines = readFile(subsystemFilePath);
        List<SubSystem> subsystems = new ArrayList<>();
        for (int i = 0; i < subsystemsLines.length; i++) {
            if (subsystemsLines[i].startsWith("#") || subsystemsLines[i].startsWith("/") || subsystemsLines[i].startsWith("!")) {
                continue;
            }
            String[] cells = subsystemsLines[i].split(",");
            if (cells.length != 3) continue;
            subsystems.add(new SubSystem(cells[0], cells[1], cells[2]));
        }
        return subsystems;
    }

    private String[] readFile(String path) {
        try {
            File file = new File(path);
            Scanner input = new Scanner(file);
            List<String> lines = new ArrayList<>();
            while (input.hasNextLine()) {
                lines.add(input.nextLine());
            }
            String[] result = new String[lines.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = lines.get(i);
            }
            input.close();
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String[]{};
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

    private class SubSystem {
        String name;
        String androidRepositoryURL;
        String CMRepositoryURL;

        public SubSystem(String name, String androidRepositoryURL, String CMRepositoryURL) {
            this.name = name;
            this.androidRepositoryURL = androidRepositoryURL;
            this.CMRepositoryURL = CMRepositoryURL;
        }
    }
}
