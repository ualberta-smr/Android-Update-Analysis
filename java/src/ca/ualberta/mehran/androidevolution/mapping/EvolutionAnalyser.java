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

    private String mSourcererCCPath;

    private static final boolean DEBUG_OUTPUT_MAPPINGS_INTERSECTION = true;

    public void run(String subsystemName,
                    String pathAndroidOldAndNew,
                    String pathAndroidOldAndNew_old,
                    String pathAndroidOldAndNew_new,
                    String pathAndroidOldAndModified,
                    String pathAndroidOldAndModified_old,
                    String pathAndroidOldAndModified_new,
                    String sourcererCCPath,
                    String outputDir) {
        mSourcererCCPath = sourcererCCPath;

        Map<String, MethodMapping> mappingAndroidOldNew = new HashMap<>();
        Map<String, MethodMapping> mappingAndroidOldModified = new HashMap<>();
        Map<String, MethodModel> projectOldMethods = new HashMap<>();
        Map<String, MethodModel> projectNewMethods = new HashMap<>();
        Map<String, MethodModel> projectModifiedMethods = new HashMap<>();

        int[] methodsCount = discoverMappings(pathAndroidOldAndNew,
                pathAndroidOldAndNew_old,
                pathAndroidOldAndNew_new,
                pathAndroidOldAndModified,
                pathAndroidOldAndModified_old,
                pathAndroidOldAndModified_new,
                mappingAndroidOldNew,
                mappingAndroidOldModified,
                projectOldMethods,
                projectNewMethods,
                projectModifiedMethods);


//        randomSampleCategory(projectOldMethods,
//                projectNewMethods,
//                projectModifiedMethods,
//                mappingAndroidOldNew,
//                mappingAndroidOldModified, MethodMapping.Type.BODY_CHANGE_ONLY, MethodMapping.Type.BODY_CHANGE_ONLY, 20);

        if (DEBUG_OUTPUT_MAPPINGS_INTERSECTION) {
            printMappingsIntersection(mappingAndroidOldNew, mappingAndroidOldModified, projectOldMethods);
        }

        Map<MethodMapping.Type, Map<MethodMapping.Type, List<Integer>>> stats = generateIntersectionsOfMappings(projectOldMethods,
                projectNewMethods,
                projectModifiedMethods,
                mappingAndroidOldNew,
                mappingAndroidOldModified);
        writeToOutput(methodsCount[0], methodsCount[1], methodsCount[2], stats, new File(outputDir, subsystemName + ".csv").getAbsolutePath());
    }

    private void writeToOutput(int projectOldMethodsCount, int projectNewMethodsCount,
                               int projectModifiedMethodsCount,
                               Map<MethodMapping.Type, Map<MethodMapping.Type, List<Integer>>> stats,
                               String outputPath) {
        try {
            File outputFile = new File(outputPath);
            if (!outputFile.exists()) {
                outputFile.getParentFile().mkdirs();
                outputFile.createNewFile();
            }

            FileWriter outputWriter = new FileWriter(outputPath);

            CSVUtils.writeLine(outputWriter, Arrays.asList(String.valueOf(projectOldMethodsCount),
                    String.valueOf(projectNewMethodsCount), String.valueOf(projectModifiedMethodsCount)));

            MethodMapping.Type[] types = new MethodMapping.Type[]{MethodMapping.Type.IDENTICAL,
                    MethodMapping.Type.REFACTORED_MOVE, MethodMapping.Type.REFACTORED_RENAME, MethodMapping.Type.REFACTORED_INLINE, MethodMapping.Type.REFACTORED_EXTRACT, MethodMapping.Type.REFACTORED_ARGUMENTS_RENAME, MethodMapping.Type.REFACTORED_ARGUMENTS_REORDER,
                    MethodMapping.Type.ARGUMENTS_CHANGE_ADD, MethodMapping.Type.ARGUMENTS_CHANGE_REMOVE, MethodMapping.Type.ARGUMENTS_CHANGE_TYPE_CHANGE,
                    MethodMapping.Type.BODY_CHANGE_ONLY, MethodMapping.Type.NOT_FOUND};//,
//                    MethodMapping.Type.ADDED};

            List<String> typesString = new ArrayList<>();
            for (MethodMapping.Type type : types) {
                typesString.add(type.toString());
            }
            CSVUtils.writeLine(outputWriter, typesString);
            for (MethodMapping.Type type : types) {
                Map<MethodMapping.Type, List<Integer>> thisTypeStats = stats.getOrDefault(type, new HashMap<>());
                int total = 0;
                List<String> catStats = new ArrayList<>();
                for (MethodMapping.Type type1 : types) {
                    int intersectionCount = 0;
                    int purgedCount = 0;
                    try {
                        intersectionCount = thisTypeStats.get(type1).get(0);
                        purgedCount = thisTypeStats.get(type1).get(1);
                    } catch (Exception e) {
                    }
                    catStats.add(purgedCount == 0 ?
                            String.valueOf(intersectionCount) :
                            String.valueOf(intersectionCount) + "(" + purgedCount + ")");
                    total += intersectionCount;
                }
                catStats.add(String.valueOf(total));
                CSVUtils.writeLine(outputWriter, catStats);
            }
            CSVUtils.writeLine(outputWriter, Arrays.asList(new String[]{"New methods in new AOSP", "New methods in proprietary", "New methods in both"}));
            Map<MethodMapping.Type, List<Integer>> addedStats = stats.get(MethodMapping.Type.ADDED);
            int identicalMutualNewMethods = addedStats.get(MethodMapping.Type.ADDED).get(1);
            CSVUtils.writeLine(outputWriter, Arrays.asList(String.valueOf(addedStats.get(MethodMapping.Type.NOT_FOUND).get(0)), // New methods in new project
                    String.valueOf(addedStats.get(MethodMapping.Type.OTHER).get(0)), // New methods in modified project
                    String.valueOf(addedStats.get(MethodMapping.Type.ADDED).get(0)) + "(" + identicalMutualNewMethods + ")")); // Mutual new methods
            outputWriter.flush();
            outputWriter.close();
        } catch (IOException e) {

        }


    }

    private Map<MethodMapping.Type, Map<MethodMapping.Type, List<Integer>>> generateIntersectionsOfMappings(Map<String, MethodModel> projectOldMethods,
                                                                                                            Map<String, MethodModel> projectNewMethods,
                                                                                                            Map<String, MethodModel> projectModifiedMethods,
                                                                                                            Map<String, MethodMapping> mappingAndroidOldNew,
                                                                                                            Map<String, MethodMapping> mappingAndroidOldModified) {
        Map<MethodMapping.Type, Collection<String>> mappingOldNewStats = categorizeMappingTypes(mappingAndroidOldNew);
//        Map<MethodMapping.Type, Collection<MethodModel>> mappingOldModifiedStats = categorizeMappingTypes(mappingAndroidOldModified);

        Map<MethodMapping.Type, Map<MethodMapping.Type, List<Integer>>> oldNewAndModifiedIntersectionMap = new HashMap<>();
        for (MethodMapping.Type oldNewType : mappingOldNewStats.keySet()) {
            Collection<String> thisTypeMethods = mappingOldNewStats.get(oldNewType);
            Map<MethodMapping.Type, Collection<String>> thisTypeMappingForModified = filterMethodMapping(mappingAndroidOldModified, thisTypeMethods);

            // Purge duplicate changes
            int purgedMutualMethods = 0;
            if (oldNewType != MethodMapping.Type.IDENTICAL && thisTypeMappingForModified.containsKey(oldNewType)) {
                Collection<String> genuineChangesMethodsIntersection = new HashSet<>();
                for (String mutualOldMethod : thisTypeMappingForModified.get(oldNewType)) {
                    MethodMapping oldNewMapping = mappingAndroidOldNew.get(mutualOldMethod);
                    MethodMapping oldManipulatedMapping = mappingAndroidOldModified.get(mutualOldMethod);
                    if (oldNewMapping != null && oldManipulatedMapping != null && oldNewMapping.equals(oldManipulatedMapping)) {
                        purgedMutualMethods++;
                    } else if (oldNewMapping != null && oldManipulatedMapping != null) { // Manual inspection
                        genuineChangesMethodsIntersection.add(mutualOldMethod);
                    }
                }
                randomSample(genuineChangesMethodsIntersection, 20, projectOldMethods, projectNewMethods,
                        projectModifiedMethods, mappingAndroidOldNew, mappingAndroidOldModified);
            }

            Map<MethodMapping.Type, List<Integer>> thisTypeStatsForModified = new HashMap<>();
            for (MethodMapping.Type newModifiedType : thisTypeMappingForModified.keySet()) {
                // Random sample
//                if ((type == MethodMapping.Type.IDENTICAL || type == MethodMapping.Type.NOT_FOUND) && type1 == MethodMapping.Type.NOT_FOUND) {
//                    randomSample(thisTypeMappingForModified.get(type1), 20, projectOldMethods, projectNewMethods,
//                            projectModifiedMethods, mappingAndroidOldNew, mappingAndroidOldModified);
//                }
                int intersectionCount = thisTypeMappingForModified.get(newModifiedType).size();
                if (newModifiedType != MethodMapping.Type.IDENTICAL && newModifiedType == oldNewType) {
                    List<Integer> countList = new ArrayList<>();
                    countList.add(intersectionCount - purgedMutualMethods);
                    countList.add(purgedMutualMethods);
                    thisTypeStatsForModified.put(newModifiedType, countList);
                } else {
                    thisTypeStatsForModified.put(newModifiedType, Arrays.asList(intersectionCount));
                }
            }
            oldNewAndModifiedIntersectionMap.put(oldNewType, thisTypeStatsForModified);
        }

        // Identify deleted methods
        Map<MethodMapping.Type, List<Integer>> notFoundMethods = new HashMap<>();
        for (String methodModel : projectOldMethods.keySet()) {
            if (!mappingAndroidOldNew.containsKey(methodModel)) {
                MethodMapping.Type modifiedType = MethodMapping.Type.NOT_FOUND;
                if (mappingAndroidOldModified.containsKey(methodModel)) {
                    modifiedType = mappingAndroidOldModified.get(methodModel).getType();
                }
                if (!notFoundMethods.containsKey(modifiedType)) {
                    notFoundMethods.put(modifiedType, new ArrayList<>());
                    notFoundMethods.get(modifiedType).add(0);
                }
                int count = notFoundMethods.get(modifiedType).get(0) + 1;
                notFoundMethods.get(modifiedType).clear();
                notFoundMethods.get(modifiedType).add(count);
//                if (modifiedType == MethodMapping.Type.NOT_FOUND) {
//                    System.out.println(methodModel);
//                    System.out.println("AO: " + projectOldMethods.get(methodModel).getFilePath());
//                    System.out.println("AN: Deleted");
//                    System.out.println("CM: Deleted");
//                    System.out.println("--------------------------");
//                }
            }
        }
        oldNewAndModifiedIntersectionMap.put(MethodMapping.Type.NOT_FOUND, notFoundMethods);

        // Identify new methods
        Collection<String> newMethodsInProjectNew = filterUnmatchedMethods(projectNewMethods.keySet(),
                getStringListOfDestinationMethods(mappingAndroidOldNew.values()));
        Collection<String> newMethodsInProjectModified = filterUnmatchedMethods(projectModifiedMethods.keySet(),
                getStringListOfDestinationMethods(mappingAndroidOldModified.values()));
        int mutualNewMethods = 0;
        int identicalMutualNewMethods = 0;
        for (String newProjectNewMethod : newMethodsInProjectNew) {
            if (newMethodsInProjectModified.contains(newProjectNewMethod)) {
                mutualNewMethods++;
                if (projectNewMethods.containsKey(newProjectNewMethod) && projectModifiedMethods.containsKey(newProjectNewMethod)) {
                    if (projectNewMethods.get(newProjectNewMethod).readFromFile().equals(projectModifiedMethods.get(newProjectNewMethod).readFromFile())) {
                        identicalMutualNewMethods++;
                    }
                }
            }
        }
        Map<MethodMapping.Type, List<Integer>> newMethods = new HashMap<>();
        newMethods.put(MethodMapping.Type.ADDED, Arrays.asList(mutualNewMethods - identicalMutualNewMethods, identicalMutualNewMethods));
        newMethods.put(MethodMapping.Type.NOT_FOUND, Arrays.asList(newMethodsInProjectNew.size())); // Bad notation, just to save the data
        newMethods.put(MethodMapping.Type.OTHER, Arrays.asList(newMethodsInProjectModified.size()));// Bad notation, just to save the data
        oldNewAndModifiedIntersectionMap.put(MethodMapping.Type.ADDED, newMethods);

        return oldNewAndModifiedIntersectionMap;
    }

    private void printMappingsIntersection(Map<String, MethodMapping> mappingAndroidOldNew, Map<String, MethodMapping> mappingAndroidOldModified, Map<String, MethodModel> projectOldMethods) {
        for (String oldMethod : mappingAndroidOldModified.keySet()) {
            if (mappingAndroidOldNew.containsKey(oldMethod) && !(mappingAndroidOldModified.get(oldMethod).getType() == MethodMapping.Type.IDENTICAL && mappingAndroidOldNew.get(oldMethod).getType() == MethodMapping.Type.IDENTICAL)) {
                System.out.println("AO: " + oldMethod + " " + projectOldMethods.get(oldMethod).getFilePath());
                System.out.println("AN: " + mappingAndroidOldNew.get(oldMethod).getType() + "\t" +  mappingAndroidOldNew.get(oldMethod).getDestinationMethod() + " " + mappingAndroidOldNew.get(oldMethod).getDestinationMethod().getFilePath());
                System.out.println("CM: " + mappingAndroidOldModified.get(oldMethod).getType() + "\t" + mappingAndroidOldModified.get(oldMethod).getDestinationMethod() + " " +  mappingAndroidOldModified.get(oldMethod).getDestinationMethod().getFilePath());
                System.out.println("-------------------------------------------------------");
            }
        }
    }

    private Collection<String> filterUnmatchedMethods(Collection<String> allMethods, Collection<String> matchedMethods) {
        Collection<String> results = new HashSet<>();
        results.addAll(allMethods);
        results.removeAll(matchedMethods);
        return results;
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

    private Map<MethodMapping.Type, Collection<String>> filterMethodMapping(Map<String, MethodMapping> mapping,
                                                                            Collection<String> methodsToFilter) {
        Map<MethodMapping.Type, Collection<String>> result = new HashMap<>();
        for (String methodModel : methodsToFilter) {
            MethodMapping.Type mappingType = MethodMapping.Type.NOT_FOUND;
            if (mapping.containsKey(methodModel)) {
                mappingType = mapping.get(methodModel).getType();
            }
            if (!result.containsKey(mappingType)) {
                result.put(mappingType, new HashSet<>());
            }
            result.get(mappingType).add(methodModel);
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
                                   Map<String, MethodModel> projectOldMethods,
                                   Map<String, MethodModel> projectNewMethods,
                                   Map<String, MethodModel> projectModifiedMethods) {

        Map<String, String> classesByQualifiedNameAndroidOldAndNew_old = new HashMap<>();
        Map<String, String> classesByQualifiedNameAndroidOldAndNew_new = new HashMap<>();
        Map<String, String> classesByQualifiedNameAndroidOldAndModified_old = new HashMap<>();
        Map<String, String> classesByQualifiedNameAndroidOldAndModified_new = new HashMap<>();

        SpoonHelper spoonHelper = new SpoonHelper();
        Map<String, MethodModel> methodsBySignatureAndroidOldAndNew_old = spoonHelper.extractAllMethodsBySignature(pathAndroidOldAndNew_old, classesByQualifiedNameAndroidOldAndNew_old);
        Map<String, MethodModel> methodsBySignatureAndroidOldAndNew_new = spoonHelper.extractAllMethodsBySignature(pathAndroidOldAndNew_new, classesByQualifiedNameAndroidOldAndNew_new);
        Map<String, MethodModel> methodsBySignatureAndroidOldAndModified_old = spoonHelper.extractAllMethodsBySignature(pathAndroidOldAndModified_old, classesByQualifiedNameAndroidOldAndModified_old);
        Map<String, MethodModel> methodsBySignatureAndroidOldAndModified_new = spoonHelper.extractAllMethodsBySignature(pathAndroidOldAndModified_new, classesByQualifiedNameAndroidOldAndModified_new);
        projectOldMethods.putAll(methodsBySignatureAndroidOldAndNew_old);
        projectNewMethods.putAll(methodsBySignatureAndroidOldAndNew_new);
        projectModifiedMethods.putAll(methodsBySignatureAndroidOldAndModified_new);

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
            result.add(methodMapping.getDestinationMethod());
        }

        return result;
    }

    private Collection<String> getStringListOfDestinationMethods(Collection<MethodMapping> methodMappingList) {
        Collection<MethodModel> methods = getListOfDestinationMethods(methodMappingList);
        Collection<String> result = new HashSet<>();

        for (MethodModel method : methods) {
            result.add(method.toString());
        }

        return result;
    }

    private void randomSampleCategory(Map<String, MethodModel> projectOldMethods,
                                      Map<String, MethodModel> projectNewMethods,
                                      Map<String, MethodModel> projectModifiedMethods,
                                      Map<String, MethodMapping> mappingAndroidOldNew,
                                      Map<String, MethodMapping> mappingAndroidOldModified,
                                      MethodMapping.Type androidOldNewType,
                                      MethodMapping.Type androidOldModifiedType,
                                      int sampleSize) {

        Map<MethodMapping.Type, Collection<String>> mappingOldNewStats = categorizeMappingTypes(mappingAndroidOldNew);
        Map<MethodMapping.Type, Collection<String>> mappingOldModifiedStats = categorizeMappingTypes(mappingAndroidOldModified);
        List<String> finalSet = new ArrayList<>();

        Collection<String> oldMethodsForModified = mappingOldModifiedStats.get(androidOldModifiedType);
        for (String oldMethod : mappingOldNewStats.get(androidOldNewType)) {
            if (oldMethodsForModified.contains(oldMethod)) {
                finalSet.add(oldMethod);
            }
        }

        Random rand = new Random();
        for (int i = 0; i < sampleSize && finalSet.size() > 0; i++) {
            int randomIndex = rand.nextInt(finalSet.size());
            String mutualOldMethod = finalSet.get(randomIndex);
            finalSet.remove(randomIndex);
            MethodModel anMethod = mappingAndroidOldNew.get(mutualOldMethod).getDestinationMethod();
            MethodModel cmMethod = mappingAndroidOldModified.get(mutualOldMethod).getDestinationMethod();
            System.out.println("AO method: " + projectOldMethods.get(mutualOldMethod).getFilePath() + " " + projectOldMethods.get(mutualOldMethod).getName());
            System.out.println("AN method: " + anMethod.getFilePath() + " " + anMethod.getName());
            System.out.println("MO method: " + cmMethod.getFilePath() + " " + cmMethod.getName());
            System.out.println("AO->AN " + androidOldNewType + " " + mappingAndroidOldNew.get(mutualOldMethod).getType());
            System.out.println("AO->MO " + androidOldModifiedType + " " + mappingAndroidOldModified.get(mutualOldMethod).getType());
            System.out.println("-----------------------------------------");
        }
    }

    private void randomSample(Collection<String> mainSet, int samplesCount, Map<String, MethodModel> projectOldMethods,
                              Map<String, MethodModel> projectNewMethods,
                              Map<String, MethodModel> projectModifiedMethods,
                              Map<String, MethodMapping> mappingAndroidOldNew,
                              Map<String, MethodMapping> mappingAndroidOldModified) {
//        Set<Integer> randomIndices = new HashSet<>();
//        Random rand = new Random();
//        while (randomIndices.size() < Math.min(samplesCount, mainSet.size())) {
//            int newIndex = rand.nextInt(mainSet.size());
//            if (!randomIndices.contains(newIndex)) {
//                randomIndices.add(newIndex);
//            }
//        }
//        int index = -1;
        for (String mutualOldMethod : mainSet) {
//            index++;
//            if (randomIndices.contains(index)) {
            MethodMapping oldNewMapping = mappingAndroidOldNew.get(mutualOldMethod);
            MethodMapping oldManipulatedMapping = mappingAndroidOldModified.get(mutualOldMethod);
//                if (oldManipulatedMapping != null) {
//                    List<String> aoString = Arrays.asList(projectOldMethods.get(mutualOldMethod).readFromFile().split("\n"));
//                    List<String> anString = Arrays.asList(projectNewMethods.get(oldNewMapping.getDestinationMethod().getUMLFormSignature()).readFromFile().split("\n"));
//                    List<String> cmString = Arrays.asList(projectModifiedMethods.get(oldManipulatedMapping.getDestinationMethod().getUMLFormSignature()).readFromFile().split("\n"));
//                    List<Delta> anDeltas = DiffUtils.diff(aoString, anString).getDeltas();
//                    Set<Integer> anDeltasPosition = new HashSet<>();
            System.out.println(mutualOldMethod);
            System.out.println("AO: " + projectOldMethods.get(mutualOldMethod).getFilePath());
            System.out.println("AN: " + ((oldNewMapping == null) ? "Deleted" : oldNewMapping.getType()));
            System.out.println("CM: " + ((oldManipulatedMapping == null) ? "Deleted" : oldManipulatedMapping.getType()));
//                    System.out.println("AN: " + projectNewMethods.get(oldNewMapping.getDestinationMethod().getUMLFormSignature()).getFilePath());
//
//                    for (Delta delta : anDeltas) {
////                        anDeltasPosition.add(delta.getOriginal().getPosition());
//                        System.out.println(delta);
//                    }
//                    System.out.println("CM: " + projectModifiedMethods.get(oldManipulatedMapping.getDestinationMethod().getUMLFormSignature()).getFilePath());
//                    List<Delta> cmDeltas = DiffUtils.diff(aoString, cmString).getDeltas();
//                    for (Delta delta : cmDeltas) {
////                        if (anDeltasPosition.contains(delta.getOriginal().getPosition())){
//
//                        System.out.println(delta);
////                        }
//                    }
            System.out.println("--------------------------");
//                }
//            }
        }
    }
}
