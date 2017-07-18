import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class SourcererHelper extends MappingDiscoverer {

    private static final String TOKENS_FILE_RELATIVE_PATH = "input/dataset/tokens.file";
    private static final String HEADERS_FILE_RELATIVE_PATH = "input/bookkeping/headers.file";
    private static final String OUTPUT_FILE_RELATIVE_PATH = "output10.0/tokensclones_index_WITH_FILTER.txt";

    private String sourcererccPath;
    private File tokensFile;
    private File headersFile;
    private File outputFile;

    public SourcererHelper(String sourcererccPath) {
        super("SourcererCC");
        this.sourcererccPath = sourcererccPath;

        tokensFile = new File(sourcererccPath, TOKENS_FILE_RELATIVE_PATH);
        headersFile = new File(sourcererccPath, HEADERS_FILE_RELATIVE_PATH);
        outputFile = new File(sourcererccPath, OUTPUT_FILE_RELATIVE_PATH);
    }


    private void runSourcererCC(String projectPath) {
        String tokenizingCommand[] = new String[]{"java",
                "-jar",
                "InputBuilderClassic.jar",
                projectPath,
                tokensFile.getAbsolutePath(),
                headersFile.getAbsolutePath(),
                "functions",
                "java",
                "0",
                "0",
                "0",
                "0",
                "false",
                "false",
                "false",
                "8"};
        String[] indexingCommand = new String[]{"java", "-jar", "dist/indexbased.SearchManager.jar", "index", "10"};
        String[] searchingCommand = new String[]{"java", "-jar", "dist/indexbased.SearchManager.jar", "search", "10"};

        long startTime = System.currentTimeMillis();
        runSystemCommand(new File(sourcererccPath, "parser/java").getAbsolutePath(), false, tokenizingCommand);
        runSystemCommand(sourcererccPath, false, indexingCommand);
        runSystemCommand(sourcererccPath, false, searchingCommand);
//        System.out.println("Sourcerer ran in " + (System.currentTimeMillis() - startTime) + " milliseconds");

    }

    private void runSystemCommand(String dir, boolean verbose, String... commands) {
        try {
            if (verbose) {
                for (String command : commands) {
                    System.out.print(command + " ");
                }
                System.out.println();
            }
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(commands, null, new File(dir));

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));

            String s = null;
            while ((s = stdInput.readLine()) != null) {
                if (verbose) System.out.println(s);
            }

            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void populateBlocks(String projectBothPath,
                                String projectOldPath,
                                String projectNewPath,
                                Map<Integer, CodeBlock> projectOldBlocks,
                                Map<Integer, CodeBlock> projectNewBlocks) {
        try {
            Scanner reader = null;
            reader = new Scanner(headersFile);
            while (reader.hasNextLine()) {
                String line = reader.nextLine();
                CodeBlock block = new CodeBlock();
                String[] fields = line.split(",");
                block.id = Integer.valueOf(fields[0]);
                block.startLine = Integer.valueOf(fields[2]);
                block.endLine = Integer.valueOf(fields[3]);
                block.path = fields[1];
                block.relativePath = block.path.substring(projectBothPath.length() + 1);
                block.relativePath = block.relativePath.substring(block.relativePath.indexOf("/") + 1);
                block.project = block.path.substring(0, block.path.length() - block.relativePath.length());
                if (new File(block.project).getAbsolutePath().equals(new File(projectOldPath).getAbsolutePath())) {
                    projectOldBlocks.put(block.id, block);
                } else {
                    projectNewBlocks.put(block.id, block);
                }
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Map<MethodModel, MethodMapping> readClonePairs(
            Collection<MethodModel> projectOldMethods,
            Collection<MethodModel> projectNewMethods,
            Map<Integer, CodeBlock> projectOldBlocks,
            Map<Integer, CodeBlock> projectNewBlocks) {

        Map<MethodModel, MethodMapping> mapping = new HashMap<>();

        Map<String, MethodModel> projectOldMethodsMapByFile = mapByFileLine(projectOldMethods);
        Map<String, MethodModel> projectNewMethodsMapByFile = mapByFileLine(projectNewMethods);

        try {
            Scanner clonesInput = new Scanner(outputFile);
            while (clonesInput.hasNextLine()) {
                String line = clonesInput.nextLine();
                int leftBlockId = Integer.valueOf(line.split(",")[0]);
                int rightBlockId = Integer.valueOf(line.split(",")[1]);

                if (!projectOldBlocks.containsKey(leftBlockId)) {
                    int temp = leftBlockId;
                    leftBlockId = rightBlockId;
                    rightBlockId = temp;
                }
                CodeBlock leftCodeBlock = projectOldBlocks.get(leftBlockId);
                CodeBlock rightCodeBlock = projectNewBlocks.get(rightBlockId);
                if (leftCodeBlock != null && rightCodeBlock != null) {
                    if (projectOldMethodsMapByFile.containsKey(generateUniqueKey(leftCodeBlock)) &&
                            projectNewMethodsMapByFile.containsKey(generateUniqueKey(rightCodeBlock))) {
                        MethodModel oldMethod = projectOldMethodsMapByFile.get(generateUniqueKey(leftCodeBlock));
                        MethodModel newMethod = projectNewMethodsMapByFile.get(generateUniqueKey(rightCodeBlock));
                        MethodMapping mappingInstance = new MethodMapping(
                                newMethod, MethodMapping.Type.IDENTICAL);
                        mapping.put(oldMethod, mappingInstance);
                    }
                }

            }

            clonesInput.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        return mapping;
    }

    private Map<String, MethodModel> mapByFileLine(Collection<MethodModel> methods) {
        Map<String, MethodModel> result = new HashMap<>();

        for (MethodModel method : methods) {
            String key = method.getFilePath() + "," + method.getLineStart() + ":" + method.getLineEnd();
            result.put(key, method);
        }

        return result;
    }

    private String generateUniqueKey(CodeBlock block) {
        return block.path + "," + block.startLine + ":" + block.endLine;
    }

    public Map<MethodModel, MethodMapping> identifyIdenticalMethods(String projectPath,
                                                                    String projectOldPath,
                                                                    String projectNewPath,
                                                                    Collection<MethodModel> projectOldMethods,
                                                                    Collection<MethodModel> projectNewMethods) {
        onStart();
        runSourcererCC(projectPath);
        Map<Integer, CodeBlock> projectOldBlocks = new HashMap<>();
        Map<Integer, CodeBlock> projectNewBlocks = new HashMap<>();
        populateBlocks(projectPath, projectOldPath, projectNewPath, projectOldBlocks, projectNewBlocks);
        onFinish();
        return readClonePairs(projectOldMethods, projectNewMethods, projectOldBlocks, projectNewBlocks);
    }

    private class CodeBlock {
        String project, path, relativePath;
        int id, startLine, endLine;
    }


}
