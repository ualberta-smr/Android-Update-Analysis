import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.diff.*;
import org.refactoringminer.api.Refactoring;

import java.io.File;
import java.util.*;


public class RefactoringMinerHelper extends MappingDiscoverer{

    public RefactoringMinerHelper(){
        super("RefactoringMiner");
    }

    public Map<MethodModel, MethodMapping> identifyRefactoring(String projectPath,
                                                               String projectOldPath,
                                                               String projectNewPath,
                                                               Collection<MethodModel> projectOldMethods,
                                                               Collection<MethodModel> projectNewMethods,
                                                               Map<String, String> oldClassesByQualifiedName,
                                                               Map<String, String> newClassesByQualifiedName,
                                                               Map<String, String> refactoredClassFilesMapping) {
        onStart();

        Map<String, MethodModel> projectOldUniqueSignatureMap = getMappingBySignature(projectOldMethods);
        Map<String, MethodModel> projectNewUniqueSignatureMap = getMappingBySignature(projectNewMethods);

        Map<String, String> oldClassesByQualifiedNameFixed = convertDollarSignToDot(oldClassesByQualifiedName);
        Map<String, String> newClassesByQualifiedNameFixed = convertDollarSignToDot(newClassesByQualifiedName);

        List<Refactoring> refactorings = getRefactorings(projectOldPath, projectNewPath);
        Map<MethodModel, MethodMapping> result = new HashMap<>();
        for (Refactoring refactoring : refactorings) {
            // This also includes method pull up and push downs
            // There are two types of refactoring: MERGE_OPERATION and CHANGE_METHOD_SIGNATURE which apparently are
            // not implemented yet.
            if (refactoring instanceof MoveOperationRefactoring || refactoring instanceof RenameOperationRefactoring ||
                    refactoring instanceof InlineOperationRefactoring || refactoring instanceof ExtractOperationRefactoring ||
                    refactoring instanceof ExtractAndMoveOperationRefactoring) {
                UMLOperation[] ops = fetchOriginalAndDestinationOperations(refactoring);
                UMLOperation originalMethodUML = ops[0];
                UMLOperation destMethodUML = ops[1];
                if (projectOldUniqueSignatureMap.containsKey(generateUniqueSignature(originalMethodUML)) &&
                        projectNewUniqueSignatureMap.containsKey(generateUniqueSignature(destMethodUML))) {
                    MethodModel oldMethod = projectOldUniqueSignatureMap.get(generateUniqueSignature(originalMethodUML));
                    MethodModel newMethod = projectNewUniqueSignatureMap.get(generateUniqueSignature(destMethodUML));
                    result.put(oldMethod, new MethodMapping(newMethod, MethodMapping.Type.REFACTORED));
                } else {
                    System.out.println("Could not find a method in RefactoringMiner:");
                    System.out.println("\tOriginal method: " + generateUniqueSignature(originalMethodUML));
                    System.out.println("\tDestination method: " + generateUniqueSignature(destMethodUML));
                }
                // There is another type: MOVE_RENAME_CLASS. Didn't find the corresponding class
            } else if (refactoring instanceof MoveClassRefactoring || refactoring instanceof RenameClassRefactoring) {
                String[] classes = fetchOriginalAndDestinationClasses(refactoring);
                if (oldClassesByQualifiedNameFixed.containsKey(classes[0]) && newClassesByQualifiedNameFixed.containsKey(classes[1])) {
                    refactoredClassFilesMapping.put(oldClassesByQualifiedNameFixed.get(classes[0]), newClassesByQualifiedNameFixed.get(classes[1]));
                } else {
                    System.out.println("Could not find a class in RefactoringMiner:");
                    System.out.println("\tOriginal class: " + classes[0]);
                    System.out.println("\tDestination class: " + classes[1]);
                }
            } else if (refactoring instanceof ExtractSuperclassRefactoring) {
                // A new superclass in introduces which includes older classes
                String originalClass = ((ExtractSuperclassRefactoring) refactoring).getExtractedClass().getName();
                Collection<String> newClasses = ((ExtractSuperclassRefactoring) refactoring).getSubclassSet();
                if (newClasses != null) {
                    for (String newClass : newClasses) {
                        if (oldClassesByQualifiedNameFixed.containsKey(originalClass) && newClassesByQualifiedNameFixed.containsKey(newClass)) {
                            refactoredClassFilesMapping.put(oldClassesByQualifiedNameFixed.get(originalClass), newClassesByQualifiedNameFixed.get(newClass));
                        } else {
                            System.out.println("Could not find a class:");
                            System.out.println("\tOriginal class: " + originalClass);
                            System.out.println("\tDestination class: " + newClass);
                        }
                    }
                }
            }
        }
        onFinish();
        return result;
    }

    private <T> Map<String, T> convertDollarSignToDot(Map<String, T> map) {
        Map<String, T> result = new HashMap<>();
        if (map != null) {
            for (String key : map.keySet()) {
                result.put(key.replace("$", "."), map.get(key));
            }
        }
        return result;
    }

    private UMLOperation[] fetchOriginalAndDestinationOperations(Refactoring refactoring) {
        UMLOperation[] ops = new UMLOperation[2];
        if (refactoring instanceof MoveOperationRefactoring) {
            ops[0] = ((MoveOperationRefactoring) refactoring).getOriginalOperation();
            ops[1] = ((MoveOperationRefactoring) refactoring).getMovedOperation();
        } else if (refactoring instanceof RenameOperationRefactoring) {
            ops[0] = ((RenameOperationRefactoring) refactoring).getOriginalOperation();
            ops[1] = ((RenameOperationRefactoring) refactoring).getRenamedOperation();
        } else if (refactoring instanceof InlineOperationRefactoring) {
            // TODO Not sure if this is correct
            ops[0] = ((InlineOperationRefactoring) refactoring).getInlinedOperation();
            ops[1] = ((InlineOperationRefactoring) refactoring).getInlinedToOperation();
        } else if (refactoring instanceof ExtractOperationRefactoring) {
            ops[0] = ((ExtractOperationRefactoring) refactoring).getExtractedFromOperation();
            ops[1] = ((ExtractOperationRefactoring) refactoring).getExtractedOperation();
        } else if (refactoring instanceof ExtractAndMoveOperationRefactoring) {
            ops[0] = ((ExtractAndMoveOperationRefactoring) refactoring).getExtractedFromOperation();
            ops[1] = ((ExtractAndMoveOperationRefactoring) refactoring).getExtractedOperation();
        } else {
            throw new UnsupportedOperationException("Refactoring " + refactoring.getClass().getName() + " is not supported");
        }
        return ops;
    }


    private String[] fetchOriginalAndDestinationClasses(Refactoring refactoring) {
        String[] classes = new String[2];
        if (refactoring instanceof RenameClassRefactoring) {
            classes[0] = ((RenameClassRefactoring) refactoring).getOriginalClassName();
            classes[1] = ((RenameClassRefactoring) refactoring).getRenamedClassName();
        } else if (refactoring instanceof MoveClassRefactoring) {
            classes[0] = ((MoveClassRefactoring) refactoring).getOriginalClassName();
            classes[1] = ((MoveClassRefactoring) refactoring).getMovedClassName();
        } else {
            throw new UnsupportedOperationException("Refactoring " + refactoring.getClass().getName() + " is not supported");
        }
        return classes;
    }

    private List<Refactoring> getRefactorings(String projectOldPath,
                                              String projectNewPath) {
        List<String> projectOldFiles = getAllSubFiles(projectOldPath, projectOldPath, ".java");
        List<String> projectNewFiles = getAllSubFiles(projectNewPath, projectNewPath, ".java");

        UMLModel modelOld = new UMLModelASTReader(new File(projectOldPath), projectOldFiles).getUmlModel();
        UMLModel modelNew = new UMLModelASTReader(new File(projectNewPath), projectNewFiles).getUmlModel();
        UMLModelDiff modelDiff = modelOld.diff(modelNew);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        return refactorings;
    }

    private List<String> getAllSubFiles(String path, String root, String suffix) {
        List<String> allFiles = new ArrayList<>();
        if (path == null) path = "";
        File file = new File(path);
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                allFiles.addAll(getAllSubFiles(subFile.getAbsolutePath(), root, suffix));
            }
        } else {
            if (file.getName().toLowerCase().endsWith(suffix.toLowerCase()))
                allFiles.add(file.getAbsolutePath().substring(root.length()));
        }
        return allFiles;
    }

    private Map<String, MethodModel> getMappingBySignature(Collection<MethodModel> methods) {
        Map<String, MethodModel> map = new HashMap<>();

        for (MethodModel method : methods) {
            map.put(method.getUMLFormSignature(), method);
        }

        return map;
    }

    private String generateUniqueSignature(UMLOperation method) {
        String parameters = "";
        for (UMLType paramType : method.getParameterTypeList()) {
            parameters += paramType.toString() + ",";
        }
        if (!parameters.equals("")) {
            parameters = parameters.substring(0, parameters.length() - 1);
        }
        return method.getClassName() + "." + method.getName() + "(" + parameters + "):" + method.getReturnParameter().getType().toString();
    }

}
