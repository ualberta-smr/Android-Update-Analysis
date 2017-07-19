package ca.ualberta.mehran.androidevolution.mapping;

import ca.ualberta.mehran.androidevolution.CSVUtils;
import ca.ualberta.mehran.androidevolution.mapping.discovery.SpoonHelper;
import ca.ualberta.mehran.androidevolution.mapping.discovery.implementation.BodyChangeOnlyHelper;
import ca.ualberta.mehran.androidevolution.mapping.discovery.implementation.ChangeDistillerHelper;
import ca.ualberta.mehran.androidevolution.mapping.discovery.implementation.RefactoringMinerHelper;
import ca.ualberta.mehran.androidevolution.mapping.discovery.implementation.SourcererHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class EvolutionAnalyser {

    private String mSourcererCCPath = "/Users/mehran/Android API/SourcererCC";


    public static void main(String[] args) {

        String subsystemName = "contacts_a6_a7_cm13";

        String pathAndroidOldAndNew = "/Users/mehran/Library/Mobile Documents/com~apple~CloudDocs/Android API/Contacts/android6.0_android7.0";
        String pathAndroidOldAndNew_old = "/Users/mehran/Library/Mobile Documents/com~apple~CloudDocs/Android API/Contacts/android6.0_android7.0/1.0.0";
        String pathAndroidOldAndNew_new = "/Users/mehran/Library/Mobile Documents/com~apple~CloudDocs/Android API/Contacts/android6.0_android7.0/2.0.0";

        String pathAndroidOldAndModified = "/Users/mehran/Library/Mobile Documents/com~apple~CloudDocs/Android API/Contacts/android6.0_cm13.0";
        String pathAndroidOldAndModified_old = "/Users/mehran/Library/Mobile Documents/com~apple~CloudDocs/Android API/Contacts/android6.0_cm13.0/1.0.0";
        String pathAndroidOldAndModified_new = "/Users/mehran/Library/Mobile Documents/com~apple~CloudDocs/Android API/Contacts/android6.0_cm13.0/2.0.0";

        String sourcererCCPath = "/Users/mehran/Android API/SourcererCC";
        if (args.length == 8) {
            subsystemName = args[0];
            pathAndroidOldAndNew = args[1];
            pathAndroidOldAndNew_old = args[2];
            pathAndroidOldAndNew_new = args[3];
            pathAndroidOldAndModified = args[4];
            pathAndroidOldAndModified_old = args[5];
            pathAndroidOldAndModified_new = args[6];
            sourcererCCPath = args[7];
        }

        new EvolutionAnalyser().run(subsystemName, pathAndroidOldAndNew, pathAndroidOldAndNew_old, pathAndroidOldAndNew_new,
                pathAndroidOldAndModified, pathAndroidOldAndModified_old, pathAndroidOldAndModified_new, sourcererCCPath);
    }

    public void run(String subsystemName,
                    String pathAndroidOldAndNew,
                    String pathAndroidOldAndNew_old,
                    String pathAndroidOldAndNew_new,
                    String pathAndroidOldAndModified,
                    String pathAndroidOldAndModified_old,
                    String pathAndroidOldAndModified_new,
                    String sourcererCCPath) {
        mSourcererCCPath = sourcererCCPath;

        Map<String, MethodMapping> mappingAndroidOldNew = new HashMap<>();
        Map<String, MethodMapping> mappingAndroidOldModified = new HashMap<>();
        Collection<String> projectOldMethods = new HashSet<>();

        int[] methodsCount = discoverMappings(pathAndroidOldAndNew,
                pathAndroidOldAndNew_old,
                pathAndroidOldAndNew_new,
                pathAndroidOldAndModified,
                pathAndroidOldAndModified_old,
                pathAndroidOldAndModified_new,
                mappingAndroidOldNew,
                mappingAndroidOldModified,
                projectOldMethods);

        Map<MethodMapping.Type, Map<MethodMapping.Type, Integer>> stats = analyseMappings(projectOldMethods,
                mappingAndroidOldNew,
                mappingAndroidOldModified);

        writeToOutput(methodsCount[0], methodsCount[1], methodsCount[2], stats, subsystemName + ".csv");
    }


    private void writeToOutput(int projectOldMethodsCount, int projectNewMethodsCount,
                               int projectModifiedMethodsCount,
                               Map<MethodMapping.Type, Map<MethodMapping.Type, Integer>> stats,
                               String outputPath) {
        try {
            File outputFile = new File(outputPath);
            if (!outputFile.exists())
                outputFile.createNewFile();

            FileWriter outputWriter = new FileWriter(outputPath);

            CSVUtils.writeLine(outputWriter, Arrays.asList(String.valueOf(projectOldMethodsCount),
                    String.valueOf(projectNewMethodsCount), String.valueOf(projectModifiedMethodsCount)));

            MethodMapping.Type[] types = new MethodMapping.Type[]{MethodMapping.Type.IDENTICAL, MethodMapping.Type.BODY_CHANGE_ONLY,
                    MethodMapping.Type.REFACTORED, MethodMapping.Type.ARGUMENTS_CHANGE, MethodMapping.Type.NOT_FOUND};

//            CSVUtils.writeLine(outputWriter, Arrays.asList(types));
            for (MethodMapping.Type type : types) {
                Map<MethodMapping.Type, Integer> thisTypeStats = stats.getOrDefault(type, new HashMap<>());
                int total = 0;
                List<String> catStats = new ArrayList<>();
                for (MethodMapping.Type type1 : types) {
                    catStats.add(String.valueOf(thisTypeStats.getOrDefault(type1, 0)));
                    total += thisTypeStats.getOrDefault(type1, 0);
                }
                catStats.add(String.valueOf(total));
                CSVUtils.writeLine(outputWriter, catStats);
            }

            outputWriter.flush();
            outputWriter.close();
        } catch (IOException e) {

        }


    }

    private Map<MethodMapping.Type, Map<MethodMapping.Type, Integer>> analyseMappings(Collection<String> projectOldMethods,
                                                                                      Map<String, MethodMapping> mappingAndroidOldNew,
                                                                                      Map<String, MethodMapping> mappingAndroidOldModified) {
        Map<MethodMapping.Type, Collection<String>> mappingOldNewStats = categorizeMappingTypes(mappingAndroidOldNew);
//        Map<MethodMapping.Type, Collection<MethodModel>> mappingOldModifiedStats = categorizeMappingTypes(mappingAndroidOldModified);

        Map<MethodMapping.Type, Map<MethodMapping.Type, Integer>> oldNewAndModifiedIntersectionMap = new HashMap<>();
        for (MethodMapping.Type type : mappingOldNewStats.keySet()) {
            Collection<String> thisTypeMethods = mappingOldNewStats.get(type);
            Map<MethodMapping.Type, Integer> thisTypeStats = filterMethodMapping(mappingAndroidOldModified, thisTypeMethods);
            oldNewAndModifiedIntersectionMap.put(type, thisTypeStats);
        }

        Map<MethodMapping.Type, Integer> notFoundMethods = new HashMap<>();
        for (String methodModel : projectOldMethods) {
            if (!mappingAndroidOldNew.containsKey(methodModel)) {
                MethodMapping.Type modifiedType = MethodMapping.Type.NOT_FOUND;
                if (mappingAndroidOldModified.containsKey(methodModel)) {
                    modifiedType = mappingAndroidOldModified.get(methodModel).getType();
                }
                int count = notFoundMethods.getOrDefault(modifiedType, 0);
                notFoundMethods.put(modifiedType, count + 1);
            }
        }
        oldNewAndModifiedIntersectionMap.put(MethodMapping.Type.NOT_FOUND, notFoundMethods);

        return oldNewAndModifiedIntersectionMap;
    }

    private Map<MethodMapping.Type, Collection<String>> categorizeMappingTypes(Map<String, MethodMapping> mapping) {
        Map<MethodMapping.Type, Collection<String>> result = new HashMap<>();

        for (String methodModel : mapping.keySet()) {
            MethodMapping.Type mappingType = mapping.get(methodModel).getType();
            if (!result.containsKey(mappingType)) {
                result.put(mappingType, new HashSet<>());
            }
            result.get(mappingType).add(methodModel);
        }

        return result;
    }

    private Map<MethodMapping.Type, Integer> filterMethodMapping(Map<String, MethodMapping> mapping,
                                                                 Collection<String> methodsToFilter) {
        Map<MethodMapping.Type, Integer> result = new HashMap<>();
        for (String methodModel : methodsToFilter) {
            MethodMapping.Type mappingType = MethodMapping.Type.NOT_FOUND;
            if (mapping.containsKey(methodModel)) {
                mappingType = mapping.get(methodModel).getType();
            }
            result.put(mappingType, result.getOrDefault(mappingType, 0) + 1);
        }
        return result;
    }

    private Map<MethodMapping.Type, Integer> filterAndSummerizeProjectModifiedMethods(Map<MethodModel, MethodMapping> allProjectModifiedMapping,
                                                                                      Collection<MethodModel> methodsToFilter) {
        Map<MethodMapping.Type, Integer> result = new HashMap<>();
        for (MethodModel methodModel : methodsToFilter) {
            if (allProjectModifiedMapping.containsKey(methodModel)) {
                MethodMapping.Type mappingType = allProjectModifiedMapping.get(methodModel).getType();
                result.put(mappingType, result.getOrDefault(mappingType, 0) + 1);
            }
        }
        return result;
    }


    private int[] discoverMappings(String pathAndroidOldAndNew, String pathAndroidOldAndNew_old,
                                   String pathAndroidOldAndNew_new,
                                   String pathAndroidOldAndModified,
                                   String pathAndroidOldAndModified_old,
                                   String pathAndroidOldAndModified_new,
                                   Map<String, MethodMapping> mappingAndroidOldNew,
                                   Map<String, MethodMapping> mappingAndroidOldModified,
                                   Collection<String> projectOldMethods) {

        Map<String, String> classesByQualifiedNameAndroidOldAndNew_old = new HashMap<>();
        Map<String, String> classesByQualifiedNameAndroidOldAndNew_new = new HashMap<>();
        Map<String, String> classesByQualifiedNameAndroidOldAndModified_old = new HashMap<>();
        Map<String, String> classesByQualifiedNameAndroidOldAndModified_new = new HashMap<>();

        SpoonHelper spoonHelper = new SpoonHelper();
        Map<String, MethodModel> methodsBySignatureAndroidOldAndNew_old = spoonHelper.extractAllMethodsBySignature(pathAndroidOldAndNew_old, classesByQualifiedNameAndroidOldAndNew_old);
        Map<String, MethodModel> methodsBySignatureAndroidOldAndNew_new = spoonHelper.extractAllMethodsBySignature(pathAndroidOldAndNew_new, classesByQualifiedNameAndroidOldAndNew_new);
        Map<String, MethodModel> methodsBySignatureAndroidOldAndModified_old = spoonHelper.extractAllMethodsBySignature(pathAndroidOldAndModified_old, classesByQualifiedNameAndroidOldAndModified_old);
        Map<String, MethodModel> methodsBySignatureAndroidOldAndModified_new = spoonHelper.extractAllMethodsBySignature(pathAndroidOldAndModified_new, classesByQualifiedNameAndroidOldAndModified_new);
        projectOldMethods.addAll(methodsBySignatureAndroidOldAndNew_old.keySet());

        mappingAndroidOldNew.clear();
        mappingAndroidOldModified.clear();

        mappingAndroidOldNew.putAll(discoverMappingForProject(pathAndroidOldAndNew,
                pathAndroidOldAndNew_old,
                pathAndroidOldAndNew_new,
                methodsBySignatureAndroidOldAndNew_old,
                methodsBySignatureAndroidOldAndNew_new,
                classesByQualifiedNameAndroidOldAndNew_old,
                classesByQualifiedNameAndroidOldAndNew_new));
        mappingAndroidOldModified.putAll(discoverMappingForProject(pathAndroidOldAndModified,
                pathAndroidOldAndModified_old,
                pathAndroidOldAndModified_new,
                methodsBySignatureAndroidOldAndModified_old,
                methodsBySignatureAndroidOldAndModified_new,
                classesByQualifiedNameAndroidOldAndModified_old,
                classesByQualifiedNameAndroidOldAndModified_new));
        return new int[]{methodsBySignatureAndroidOldAndNew_old.size(), methodsBySignatureAndroidOldAndNew_new.size(),
                methodsBySignatureAndroidOldAndModified_new.size()};
    }


    private Map<String, MethodMapping> discoverMappingForProject(String projectPath,
                                                                 String projectOldPath,
                                                                 String projectNewPath,
                                                                 Map<String, MethodModel> projectOldMethodsMap,
                                                                 Map<String, MethodModel> projectNewMethodsMap,
                                                                 Map<String, String> oldClassesByQualifiedName,
                                                                 Map<String, String> newClassesByQualifiedName) {

        Map<MethodModel, MethodMapping> mapping = new HashMap<>();

        // Identify identical methods
        SourcererHelper sourcererHelper = new SourcererHelper(mSourcererCCPath);
        Map<MethodModel, MethodMapping> identicalMapping = sourcererHelper.identifyIdenticalMethods(projectPath,
                projectOldPath,
                projectNewPath,
                projectOldMethodsMap.values(),
                projectNewMethodsMap.values());

        mapping.putAll(identicalMapping);


        // Identify refactoring changes
        Map<String, String> refactoredClassFilesMapping = new HashMap<>();
        Map<MethodModel, MethodMapping> refactoringMapping = new HashMap<>();
        refactoringMapping = new RefactoringMinerHelper().identifyRefactoring(
                projectOldPath,
                projectNewPath,
                projectOldMethodsMap.values(),
                projectNewMethodsMap.values(),
                mapping.keySet(),
                getListOfDestinationMethods(mapping.values()),
                oldClassesByQualifiedName,
                newClassesByQualifiedName,
                refactoredClassFilesMapping);

        mapping = combineMappings(mapping, refactoringMapping);
        // Identify argument changes
        ChangeDistillerHelper changeDistillerHelper = new ChangeDistillerHelper();
        Map<MethodModel, MethodMapping> changeDistillerMapping = changeDistillerHelper.identifyMethodArgumentChanges(
                projectOldPath,
                projectNewPath,
                projectOldMethodsMap.values(),
                projectNewMethodsMap.values(),
                mapping.keySet(),
                getListOfDestinationMethods(mapping.values()),
                refactoredClassFilesMapping);

        mapping = combineMappings(mapping, changeDistillerMapping);

        // Identify body changes
        BodyChangeOnlyHelper bodyChangeOnlyHelper = new BodyChangeOnlyHelper();
        Map<MethodModel, MethodMapping> bodyChangeOnlyMapping = bodyChangeOnlyHelper.identifyBodyChanges(
                removeEntities(projectOldMethodsMap.values(), mapping.keySet()),
                removeEntities(projectNewMethodsMap.values(), getListOfDestinationMethods(mapping.values())));

        mapping = combineMappings(mapping, bodyChangeOnlyMapping);

        Map<String, MethodMapping> result = new HashMap<>();
        for (MethodModel methodModel : mapping.keySet()) {
            result.put(methodModel.getUMLFormSignature(), mapping.get(methodModel));
        }
        return result;
    }

    private <K, V> Map<K, V> combineMappings(Map<K, V>... mappings) {
        Map<K, V> result = new HashMap<>();

        for (Map<K, V> mapping : mappings) {
            for (K key : mapping.keySet()) {
                if (!result.containsKey(key)) {
                    result.put(key, mapping.get(key));
                }
            }
        }

        return result;
    }

    private <K> Collection<K> removeEntities(Collection<K> mainCollection, Collection<K> toBeRemoved) {
        Collection<K> result = new HashSet<>(mainCollection);
        result.removeAll(toBeRemoved);
        return result;
    }

    private Collection<MethodModel> getListOfDestinationMethods(Collection<MethodMapping> methodMappingList) {
        Collection<MethodModel> result = new HashSet<>();

        for (MethodMapping methodMapping : methodMappingList) {
            result.add(methodMapping.getDestionationMethod());
        }

        return result;
    }
}
