package ca.ualberta.mehran.androidevolution.mapping.discovery.implementation;

import anonymous.authors.androidevolution.mapping.MethodMapping;
import anonymous.authors.androidevolution.mapping.MethodModel;
import anonymous.authors.androidevolution.mapping.discovery.MappingDiscoverer;

import java.io.File;
import java.util.*;

public class BodyChangeOnlyHelper extends MappingDiscoverer {

    public BodyChangeOnlyHelper() {
        super("BodyChangeOnlyHelper");
    }

    public Map<MethodModel, MethodMapping> identifyBodyChanges(Collection<MethodModel> projectOldRemainingMethods,
                                                               Collection<MethodModel> projectNewRemainingMethods) {
        onStart();
        Map<MethodModel, MethodMapping> result = new HashMap<>();

        Map<String, Collection<MethodModel>> oldMethodsByClassName = getMappingByClassName(projectOldRemainingMethods);
        Map<String, MethodModel> newMethodsBySignature = getMappingBySignature(projectNewRemainingMethods);
        for (String className : oldMethodsByClassName.keySet()) {
            Collection<MethodModel> oldMethodsInClass = oldMethodsByClassName.get(className);
            for (MethodModel oldMethodInClass : oldMethodsInClass) {
                if (newMethodsBySignature.containsKey(oldMethodInClass.getUMLFormSignature())) {
                    MethodModel newMethod = newMethodsBySignature.get(oldMethodInClass.getUMLFormSignature());
                    try {
                        if (oldMethodInClass.readFromFile().equals(newMethod.readFromFile())) {
                            result.put(oldMethodInClass, new MethodMapping(newMethod, MethodMapping.Type.IDENTICAL));
                        } else {
                            result.put(oldMethodInClass, new MethodMapping(newMethod, MethodMapping.Type.BODY_CHANGE_ONLY));
                        }
                    } catch (Exception e) {
                        if (!result.containsKey(oldMethodInClass)){
                            result.put(oldMethodInClass, new MethodMapping(newMethod, MethodMapping.Type.BODY_CHANGE_ONLY));
                        }
                    }
                }
            }
        }
        onFinish();
        return result;
    }


    private Map<String, Collection<MethodModel>> getMappingByClassName(Collection<MethodModel> methods) {
        Map<String, Collection<MethodModel>> result = new HashMap<>();
        if (methods != null) {
            for (MethodModel method : methods) {
                String className = method.getFullClassName();
                if (result.containsKey(className)) {
                    result.get(className).add(method);
                } else {
                    Set<MethodModel> classMethods = new HashSet<>();
                    classMethods.add(method);
                    result.put(className, classMethods);
                }
            }
        }
        return result;
    }

    private Map<String, MethodModel> getMappingBySignature(Collection<MethodModel> methods) {
        Map<String, MethodModel> map = new HashMap<>();

        for (MethodModel method : methods) {
            map.put(method.getUMLFormSignature(), method);
        }

        return map;
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
