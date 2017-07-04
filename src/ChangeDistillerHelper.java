import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;

import java.io.File;
import java.util.*;


public class ChangeDistillerHelper {

    public Map<MethodModel, MethodMapping> identifyMethodArgumentChanges(String projectPath,
                                                                         String projectOldPath,
                                                                         String projectNewPath,
                                                                         Collection<MethodModel> projectOldMethods,
                                                                         Collection<MethodModel> projectNewMethods,
                                                                         Map<String, String> refactoredClassFilesMapping) {

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
                        if (change.getChangeType() == ChangeType.PARAMETER_DELETE) {
                            System.out.println("Hi.");
                            // This is the new method for delete, insert and type change
                            String methodUniqueName = change.getRootEntity().getUniqueName(); // no return type
                            change.getChangedEntity().getUniqueName(); // paramName: paramType

                        }
                        MethodMapping.Type mappingType;
                        switch (change.getChangeType()) {
                            // PARAMETER_ORDERING_CHANGE doesn't seem to work right
//                            case PARAMETER_ORDERING_CHANGE:
                            case PARAMETER_RENAMING:
//                                case METHOD_RENAMING:
                                mappingType = MethodMapping.Type.REFACTORED;
                                break;
                            case PARAMETER_DELETE:
                            case PARAMETER_INSERT:
                            case PARAMETER_TYPE_CHANGE:
                                mappingType = MethodMapping.Type.ARGUMENTS_CHANGE;
                                break;
                            case ADDITIONAL_FUNCTIONALITY:
                            case REMOVED_FUNCTIONALITY:
                            case METHOD_RENAMING:
                                // Method is added or removed
                                // Renaming is already covered by RefactoringMiner
                                continue;
                            default:
//                                System.err.println("Unsupported change in method found by ChangeDistiller");
//                                System.out.println(change);
                                continue;
                        }
                        System.out.println(change);
                    }
                }
            } catch (Exception e) {
                System.err.println("Warning: error while change distilling. " + e.getMessage());
            }
        }
        return result;
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
