package ca.ualberta.mehran.androidevolution.repositories;


import ca.ualberta.mehran.androidevolution.mapping.EvolutionAnalyser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static ca.ualberta.mehran.androidevolution.Utils.runSystemCommand;

public class Main {


    private static final String VERSIONS_FILE = "versions.csv";
    private static final String SUBSYSTEMS_FILE = "subsystems.csv";

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        String[] versions = readFile(VERSIONS_FILE);
        SubSystem[] subsystems = getSubsystems(SUBSYSTEMS_FILE);

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

                ComparisionFolder androidOldNew = new ComparisionFolder(subsystemDir.getAbsolutePath(), androidBaseVersion, androidNewVersion);
                ComparisionFolder androidOldCM = new ComparisionFolder(subsystemDir.getAbsolutePath(), androidBaseVersion, CMVersion);

                gitChangeBranch(androidRawFolder.getAbsolutePath(), androidBaseVersion);
                File androidSrcFolder = new File(androidRawFolder, "src");
                if (!androidSrcFolder.exists()) {
                    System.out.println("No src folder for " + androidRawFolder.getAbsolutePath());
                    continue;
                }
                copyFolder(androidRawFolder.getAbsolutePath(), "src", androidOldNew.getOldVersionPath() + "/");
                copyFolder(androidRawFolder.getAbsolutePath(), "src", androidOldCM.getOldVersionPath() + "/");

                gitChangeBranch(androidRawFolder.getAbsolutePath(), androidNewVersion);
                androidSrcFolder = new File(androidRawFolder, "src");
                if (!androidSrcFolder.exists()) {
                    System.out.println("No src folder for " + androidRawFolder.getAbsolutePath());
                    continue;
                }
                copyFolder(androidRawFolder.getAbsolutePath(), "src", androidOldNew.getNewVersionPath() + "/");

                gitChangeBranch(CMRawFolder.getAbsolutePath(), CMVersion);
                File CMSrcFolder = new File(CMRawFolder, "src");
                if (!CMSrcFolder.exists()) {
                    System.out.println("No src folder for " + CMSrcFolder.getAbsolutePath());
                    continue;
                }
                copyFolder(CMRawFolder.getAbsolutePath(), "src", androidOldCM.getNewVersionPath() + "/");

                String analysisName = subsystem.name + "-" + androidBaseVersion + "-" + androidNewVersion + "-" + CMVersion;
                try {
                    evolutionAnalyser.run(analysisName, androidOldNew.getPath(), androidOldNew.getOldVersionPath(), androidOldNew.getNewVersionPath(),
                            androidOldCM.getPath(), androidOldCM.getOldVersionPath(), androidOldCM.getNewVersionPath());
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }


    }

    private void copyFolder(String srcPath, String fileName, String dest) {
        runSystemCommand(srcPath, true, "cp", "-r", fileName, dest);
    }

    private void gitClone(String url, String path, String folderName) {
        runSystemCommand(path, true, "git", "clone", url, folderName);
    }

    private void gitChangeBranch(String path, String branchName) {
        runSystemCommand(path, true, "git", "checkout", branchName);
    }


    private SubSystem[] getSubsystems(String subsystemFilePath) {
        String[] subsystemsLines = readFile(subsystemFilePath);
        SubSystem[] subsystems = new SubSystem[subsystemsLines.length];
        for (int i = 0; i < subsystemsLines.length; i++) {
            String[] cells = subsystemsLines[i].split(",");
            subsystems[i] = new SubSystem(cells[0], cells[1], cells[2]);
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
            return oldVersionName + "-" + newVersionName;
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
