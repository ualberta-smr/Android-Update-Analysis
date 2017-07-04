import java.util.*;

public class Main {

    private static final String SOURCERERCC_PATH = "/Users/mehran/Android API/SourcererCC";

    public static void main(String[] args) {

        String pathAndroidOldAndNew = "/Users/mehran/Library/Mobile Documents/com~apple~CloudDocs/Android API/Contacts/android6.0_android7.0";
        String pathAndroidOldAndNew_old = "/Users/mehran/Library/Mobile Documents/com~apple~CloudDocs/Android API/Contacts/android6.0_android7.0/1.0.0";
        String pathAndroidOldAndNew_new = "/Users/mehran/Library/Mobile Documents/com~apple~CloudDocs/Android API/Contacts/android6.0_android7.0/2.0.0";


        String pathAndroidOldAndModified = "/Users/mehran/Library/Mobile Documents/com~apple~CloudDocs/Android API/Contacts/android6.0_cm13.0";
        String pathAndroidOldAndModified_old = "/Users/mehran/Library/Mobile Documents/com~apple~CloudDocs/Android API/Contacts/android6.0_cm13.0/1.0.0";
        String pathAndroidOldAndModified_new = "/Users/mehran/Library/Mobile Documents/com~apple~CloudDocs/Android API/Contacts/android6.0_cm13.0/2.0.0";

        Map<String, String> classesByQualifiedNameAndroidOldAndNew_old = new HashMap<>();
        Map<String, String> classesByQualifiedNameAndroidOldAndNew_new = new HashMap<>();
        Map<String, String> classesByQualifiedNameAndroidOldAndModified_old = new HashMap<>();
        Map<String, String> classesByQualifiedNameAndroidOldAndModified_new = new HashMap<>();
        Map<String, MethodModel> methodsBySignatureAndroidOldAndNew_old =
                SpoonHelper.getInstance().extractAllMethodsBySignature(pathAndroidOldAndNew_old, classesByQualifiedNameAndroidOldAndNew_old);
        Map<String, MethodModel> methodsBySignatureAndroidOldAndNew_new = SpoonHelper.getInstance().extractAllMethodsBySignature(pathAndroidOldAndNew_new, classesByQualifiedNameAndroidOldAndNew_new);
//        Map<String, MethodModel> methodsBySignatureAndroidOldAndModified_old = SpoonHelper.getInstance().extractAllMethodsBySignature(pathAndroidOldAndModified_old, classesByQualifiedNameAndroidOldAndModified_old);
//        Map<String, MethodModel> methodsBySignatureAndroidOldAndModified_new = SpoonHelper.getInstance().extractAllMethodsBySignature(pathAndroidOldAndModified_new, classesByQualifiedNameAndroidOldAndModified_new);

        Map<MethodModel, MethodMapping> mappingAndroidOldNew = new HashMap<>();
        Map<MethodModel, MethodMapping> mappingAndroidOldModified = new HashMap<>();


        mappingAndroidOldNew = extractMethodMapping(pathAndroidOldAndNew,
                pathAndroidOldAndNew_old,
                pathAndroidOldAndNew_new,
                methodsBySignatureAndroidOldAndNew_old,
                methodsBySignatureAndroidOldAndNew_new,
                classesByQualifiedNameAndroidOldAndNew_old,
                classesByQualifiedNameAndroidOldAndNew_new);
//        mappingAndroidOldModified = extractMethodMapping(pathAndroidOldAndModified,
//                pathAndroidOldAndModified_old,
//                pathAndroidOldAndModified_new,
//                methodsBySignatureAndroidOldAndModified_old.values(),
//                methodsBySignatureAndroidOldAndModified_new.values(),
//                classesByQualifiedNameAndroidOldAndModified_old,
//                classesByQualifiedNameAndroidOldAndModified_new);

        System.out.println("Done.");

    }

    private static Map<MethodModel, MethodMapping> extractMethodMapping(String projectPath,
                                                                        String projectOldPath,
                                                                        String projectNewPath,
                                                                        Map<String, MethodModel> projectOldMethodsMap,
                                                                        Map<String, MethodModel> projectNewMethodsMap,
                                                                        Map<String, String> oldClassesByQualifiedName,
                                                                        Map<String, String> newClassesByQualifiedName) {
        Map<MethodModel, MethodMapping> mapping = new HashMap<>();

        // Identify identical methods
//        SourcererHelper sourcererHelper = new SourcererHelper(SOURCERERCC_PATH);
//        mapping.putAll(sourcererHelper.identifyIdenticalMethods(projectPath,
//                projectOldPath,
//                projectNewPath,
//                projectOldMethods,
//                projectNewMethods));

        // Identify refactoring changes
        Map<String, String> refactoredClassFilesMapping = new HashMap<>();
        mapping.putAll(new RefactoringMinerHelper().identifyRefactoring(projectPath,
                projectOldPath,
                projectNewPath,
                removeEntries(projectOldMethodsMap.values(), mapping.keySet()),
                removeEntries(projectNewMethodsMap.values(), mappingArrayToDestMethods(mapping.values())),
                oldClassesByQualifiedName,
                newClassesByQualifiedName,
                refactoredClassFilesMapping));

        // Identify argument changes
        ChangeDistillerHelper changeDistillerHelper = new ChangeDistillerHelper();
        mapping.putAll(changeDistillerHelper.identifyMethodArgumentChanges(projectPath,
                projectOldPath,
                projectNewPath,
                removeEntries(projectOldMethodsMap.values(), mapping.keySet()),
                removeEntries(projectNewMethodsMap.values(), mappingArrayToDestMethods(mapping.values())),
                refactoredClassFilesMapping));

        // Identify body changes


        return mapping;
    }

    private static Collection<MethodModel> mappingArrayToDestMethods(Collection<MethodMapping> mappings) {
        Set<MethodModel> result = new HashSet<>();

        for (MethodMapping mapping : mappings) {
            result.add(mapping.getDestionationMethod());
        }

        return result;
    }

    private static <T> Collection<T> removeEntries(Collection<T> originalCollection, Collection<T> itemsToRemove) {
        Collection<T> result = new HashSet<>();

        for (T entry : originalCollection) {
            if (!itemsToRemove.contains(entry)) {
                result.add(entry);
            }
        }

        return result;
    }
}
