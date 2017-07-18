import java.util.HashMap;
import java.util.Map;

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

        // Identify identical methods
        SourcererHelper sourcererHelper = new SourcererHelper(SOURCERERCC_PATH);
        Map<MethodModel, MethodMapping> identicalMapping = sourcererHelper.identifyIdenticalMethods(projectPath,
                projectOldPath,
                projectNewPath,
                projectOldMethodsMap.values(),
                projectNewMethodsMap.values());

        // Identify refactoring changes
        Map<String, String> refactoredClassFilesMapping = new HashMap<>();
        Map<MethodModel, MethodMapping> refactoringMapping = new RefactoringMinerHelper().identifyRefactoring(projectPath,
                projectOldPath,
                projectNewPath,
                projectOldMethodsMap.values(),
                projectNewMethodsMap.values(),
                oldClassesByQualifiedName,
                newClassesByQualifiedName,
                refactoredClassFilesMapping);

        // Identify argument changes
        ChangeDistillerHelper changeDistillerHelper = new ChangeDistillerHelper();
        Map<MethodModel, MethodMapping> changeDistillerMapping = changeDistillerHelper.identifyMethodArgumentChanges(projectPath,
                projectOldPath,
                projectNewPath,
                projectOldMethodsMap.values(),
                projectNewMethodsMap.values(),
                refactoredClassFilesMapping);

        // Identify body changes


        return combineMappings(identicalMapping, refactoringMapping, changeDistillerMapping);
    }

    private static <K, V> Map<K, V> combineMappings(Map<K, V>... mappings) {
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
}
