import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;


public class ChangeDistillerHelper extends MappingDiscoverer {

    public ChangeDistillerHelper() {
        super("ChangeDistiller");
    }

    public Map<MethodModel, MethodMapping> identifyMethodArgumentChanges(String projectPath,
                                                                         String projectOldPath,
                                                                         String projectNewPath,
                                                                         Collection<MethodModel> projectOldMethods,
                                                                         Collection<MethodModel> projectNewMethods,
                                                                         Map<String, String> refactoredClassFilesMapping) {
        onStart();

        Map<String, Collection<MethodModel>> projectOldMethodByFilePath = getMappingByFilePath(projectOldMethods);
        Map<String, MethodModel> projectNewNoReturnTypeSignatureMap = getMappingByNoReturnTypeSignature(projectNewMethods);

        Map<MethodModel, MethodMapping> result = new HashMap<>();
        Map<String, String> filesMapping = getClassFilesMapping(projectOldPath, projectNewPath, refactoredClassFilesMapping);
        FileDistiller distiller = ChangeDistiller.createFileDistiller(ChangeDistiller.Language.JAVA);

        for (String oldFilePath : filesMapping.keySet()) {
            String newFilePath = filesMapping.get(oldFilePath);
            File oldFile = new File(oldFilePath);
            File newFile = new File(newFilePath);
            try {
                distiller.extractClassifiedSourceCodeChanges(oldFile, newFile);
                List<SourceCodeChange> changes = distiller.getSourceCodeChanges();
                if (changes != null) {
                    for (SourceCodeChange change : changes) {
                        if (change.getChangeType().toString().toLowerCase().startsWith("parameter")) {
                            MethodModel[] methods = fetchOriginalAndDestinationMethods(change,
                                    oldFilePath, projectOldMethodByFilePath.get(oldFilePath), projectNewNoReturnTypeSignatureMap);
                            MethodMapping.Type mappingType = MethodMapping.Type.ARGUMENTS_CHANGE;
                            if (change.getChangeType() == ChangeType.PARAMETER_RENAMING ||
                                    change.getChangeType() == ChangeType.PARAMETER_ORDERING_CHANGE) {
                                mappingType = MethodMapping.Type.REFACTORED;
                            }
                            if (methods != null && methods.length == 2) {
                                result.put(methods[0], new MethodMapping(methods[1], mappingType));
                            } else {
                                System.out.println("Could not find a method in ChangeDistiller:");
                                System.out.println("\tOriginal method: " + oldFilePath + ":" + change.getParentEntity().getSourceRange().toString());
                                System.out.println("\tDestination method: " + change.getRootEntity().getUniqueName());
                            }
                        }
//                        switch (change.getChangeType()) {
//                            case ADDITIONAL_FUNCTIONALITY:
//                            case REMOVED_FUNCTIONALITY:
//                            case METHOD_RENAMING:
//                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Warning: error while change distilling. " + e.getMessage());
            }
        }
        onFinish();
        return result;
    }

    private Map<String, MethodModel> getMappingByNoReturnTypeSignature(Collection<MethodModel> methods) {
        Map<String, MethodModel> result = new HashMap<>();

        if (methods != null) {
            for (MethodModel method : methods) {
                String signatureWithNoReturnType = method.getUMLFormSignature();
                signatureWithNoReturnType = signatureWithNoReturnType.substring(0, signatureWithNoReturnType.lastIndexOf(":"));
                result.put(signatureWithNoReturnType, method);
            }
        }

        return result;
    }

    private Map<String, Collection<MethodModel>> getMappingByFilePath(Collection<MethodModel> methods) {
        Map<String, Collection<MethodModel>> result = new HashMap<>();
        if (methods != null) {
            for (MethodModel method : methods) {
                String filePath = method.getFilePath();
                if (result.containsKey(filePath)) {
                    result.get(filePath).add(method);
                } else {
                    Set<MethodModel> classMethods = new HashSet<>();
                    classMethods.add(method);
                    result.put(filePath, classMethods);
                }
            }
        }
        return result;
    }

    private MethodModel[] fetchOriginalAndDestinationMethods(SourceCodeChange change,
                                                             String oldFilePath,
                                                             Collection<MethodModel> oldMethods,
                                                             Map<String, MethodModel> newMethodsBySignature) {
        if (change.getChangeType().toString().toLowerCase().startsWith("parameter")) {
            String destinationMethodSignature = change.getRootEntity().getUniqueName();
            MethodModel destinationMethod = newMethodsBySignature.get(destinationMethodSignature);
            MethodModel originalMethod =
                    resolveMethodByFileAndCharacterRange(oldFilePath, oldMethods, change.getParentEntity().getSourceRange().getStart());
            return new MethodModel[]{originalMethod, destinationMethod};
        }
        return null;
    }

    private MethodModel resolveMethodByFileAndCharacterRange(String filePath,
                                                             Collection<MethodModel> methods,
                                                             int rangeStart) {
        try {
            Scanner input = new Scanner(new File(filePath));
            int charsRead = 0;
            int linesRead = 0;
            while (input.hasNextLine()) {
                charsRead += input.nextLine().length() + 1;
                linesRead++;
                if (charsRead > rangeStart) {
                    break;
                }
            }
            input.close();

            for (MethodModel method : methods) {
                if (method.getLineStart() <= linesRead && method.getLineEnd() >= linesRead) {
                    return method;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Map<String, String> getClassFilesMapping(String projectOldPath,
                                                     String projectNewPath,
                                                     Map<String, String> refactoredClassFilesMapping) {
        Map<String, String> classFilesMapping = new HashMap<>();

        List<String> allClassesRelativePath = getAllFiles(projectOldPath, projectOldPath);
        for (String oldFileRelativePath : allClassesRelativePath) {
            String oldFileAbsolutePath = new File(projectOldPath, oldFileRelativePath).getAbsolutePath();
            if (refactoredClassFilesMapping.containsKey(oldFileAbsolutePath)) {
                classFilesMapping.put(oldFileAbsolutePath, refactoredClassFilesMapping.get(oldFileAbsolutePath));
            } else if (new File(projectNewPath, oldFileRelativePath).exists()) {
                classFilesMapping.put(oldFileAbsolutePath, new File(projectNewPath, oldFileRelativePath).getAbsolutePath());
            }
        }

        return classFilesMapping;
    }


    private List<String> getAllFiles(String projectPath, String path) {
        List<String> filesPath = new ArrayList<>();
        File folder = new File(path);
        try {
            for (File file : folder.listFiles()) {
                if (file.isDirectory()) {
                    filesPath.addAll(getAllFiles(projectPath, file.getAbsolutePath()));
                } else if (file.getName().endsWith(".java")) {
                    filesPath.add(file.getAbsolutePath().substring(projectPath.length()));
                }
            }
        } catch (NullPointerException e) {
        }
        return filesPath;
    }
}
