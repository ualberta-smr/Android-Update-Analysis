package ca.ualberta.mehran.androidevolution.mapping.discovery.implementation;

import ca.ualberta.mehran.androidevolution.mapping.MethodMapping;
import ca.ualberta.mehran.androidevolution.mapping.MethodModel;
import ca.ualberta.mehran.androidevolution.mapping.discovery.MappingDiscoverer;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;

import java.io.File;
import java.util.*;


public class ChangeDistillerHelper extends MappingDiscoverer {

    public ChangeDistillerHelper() {
        super("ChangeDistiller");
    }

    public Map<MethodModel, MethodMapping> identifyMethodArgumentChanges(String projectOldPath,
                                                                         String projectNewPath,
                                                                         Collection<MethodModel> projectOldMethods,
                                                                         Collection<MethodModel> projectNewMethods,
                                                                         Collection<MethodModel> projectOldDiscoveredMethods,
                                                                         Collection<MethodModel> projectNewDiscoveredMethods,
                                                                         Map<String, String> refactoredClassFilesMapping) {
        onStart();

        Map<String, Collection<MethodModel>> projectOldMethodByFilePath = getMappingByFilePath(projectOldMethods);
        Map<String, MethodModel> projectNewNoReturnTypeSignatureMap = getMappingByChangeDistillerStyle(projectNewMethods);

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
                        if (change.getChangeType().toString().toLowerCase().startsWith("parameter") &&
                                projectOldMethodByFilePath.containsKey(oldFilePath)) {
                            MethodModel[] methods = fetchOriginalAndDestinationMethods(change,
                                    oldFilePath, projectOldMethodByFilePath.get(oldFilePath), projectNewNoReturnTypeSignatureMap);
                            MethodMapping.Type mappingType = getMappingType(change.getChangeType());
                            if (methods != null && methods.length == 2) {
                                MethodModel oldMethod = methods[0];
                                MethodModel newMethod = methods[1];
                                if (!projectOldDiscoveredMethods.contains(oldMethod) && !projectNewDiscoveredMethods.contains(newMethod)) {
                                    result.put(oldMethod, new MethodMapping(newMethod, mappingType));
                                }
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
                e.printStackTrace();
            }
        }
        onFinish();
        return result;
    }

    private MethodMapping.Type getMappingType(ChangeType changeType) {
        switch (changeType) {
            case PARAMETER_DELETE:
                return MethodMapping.Type.ARGUMENTS_CHANGE_REMOVE;
            case PARAMETER_INSERT:
                return MethodMapping.Type.ARGUMENTS_CHANGE_ADD;
            case PARAMETER_RENAMING:
                return MethodMapping.Type.REFACTORED_ARGUMENTS_RENAME;
            case PARAMETER_ORDERING_CHANGE:
                return MethodMapping.Type.REFACTORED_ARGUMENTS_REORDER;
            case PARAMETER_TYPE_CHANGE:
                return MethodMapping.Type.ARGUMENTS_CHANGE_TYPE_CHANGE;
        }
        return null;
    }

    private Map<String, MethodModel> getMappingByChangeDistillerStyle(Collection<MethodModel> methods) {
        Map<String, MethodModel> result = new HashMap<>();

        if (methods != null) {
            for (MethodModel method : methods) {
                String signatureWithNoReturnType = method.getUMLFormSignature();
                // Remove return type
                signatureWithNoReturnType = signatureWithNoReturnType.substring(0, signatureWithNoReturnType.lastIndexOf(":"));
                // Replace <init> with class name for constructors
//                signatureWithNoReturnType = signatureWithNoReturnType.replace("<init>", method.getSimpleClassName());
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
            // Remove generics
            destinationMethodSignature = removeGenericsAndParents(destinationMethodSignature);
            MethodModel destinationMethod = newMethodsBySignature.get(destinationMethodSignature);
//            MethodModel originalMethod =
//                    resolveMethodByFileAndCharacterRange(oldFilePath, oldMethods, change.getParentEntity().getSourceRange().getStart());
            MethodModel originalMethod = resolveOriginalMethod(change, oldMethods, destinationMethod);
            if (destinationMethod != null && originalMethod != null) {
                return new MethodModel[]{originalMethod, destinationMethod};
            }
        }
        return null;
    }


    private MethodModel resolveOriginalMethod(SourceCodeChange change, Collection<MethodModel> oldMethods, MethodModel newMethod) {
        if (newMethod == null || oldMethods == null) return null;
        List<MethodModel> candidateMethods = new ArrayList<>();
        for (MethodModel oldMethod : oldMethods) {
            if (oldMethod.getName().equals(newMethod.getName())) {
                candidateMethods.add(oldMethod);
            }
        }
        if (candidateMethods.size() == 1) {
            return candidateMethods.get(0);
        }
        if (candidateMethods.size() > 1) {
            if (change.getChangeType() == ChangeType.PARAMETER_INSERT) {
                for (MethodModel candidateMethod : candidateMethods) {
                    if (newMethod.getUMLFormSignature().length() > candidateMethod.getUMLFormSignature().length()) {
                        return candidateMethod;
                    }
                }
                return candidateMethods.get(0);
            }
            if (change.getChangeType() == ChangeType.PARAMETER_DELETE) {
                for (MethodModel candidateMethod : candidateMethods) {
                    if (newMethod.getUMLFormSignature().length() < candidateMethod.getUMLFormSignature().length()) {
                        return candidateMethod;
                    }
                }
                return candidateMethods.get(0);
            }
            return candidateMethods.get(0);
        }
        return null;
    }

    private String removeGenericsAndParents(String methodSignature) {
//        if (!methodSignature.contains("<")) return methodSignature;
        StringBuilder newParams = new StringBuilder();
        String params[] = methodSignature.substring(methodSignature.indexOf("(") + 1, methodSignature.lastIndexOf(")")).split(",");
        for (int i = 0; i < params.length; i++) {
            String newParam = params[i].replaceAll("<.*>", "");
            if (newParam.contains(".")) newParam = newParam.substring(newParam.lastIndexOf(".") + 1);
            newParams.append(newParam);
            if (i < params.length - 1) newParams.append(",");
        }
        String newSignature = methodSignature.substring(0, methodSignature.indexOf("(") + 1) + newParams.toString() + ")";
        return newSignature;
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
        } catch (Exception e) {
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
