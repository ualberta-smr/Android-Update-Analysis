package ca.ualberta.mehran.androidevolution.mapping;

import ca.ualberta.mehran.androidevolution.Utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Experiments {

    private static char getIntersectionColor(MethodMapping.Type typeSource, MethodMapping.Type typeDest) {
        if (typeSource == MethodMapping.Type.NOT_FOUND || typeDest == MethodMapping.Type.NOT_FOUND) return 'r';
        if (typeSource == MethodMapping.Type.IDENTICAL || typeDest == MethodMapping.Type.IDENTICAL) return 'g';
        if (typeSource == typeDest) return 'y';

        switch (typeSource) {
            case REFACTORED_MOVE:
                if (typeDest == MethodMapping.Type.REFACTORED_INLINE || typeDest == MethodMapping.Type.REFACTORED_EXTRACT)
                    return 'r';
                return 'g';
            case REFACTORED_RENAME:
                if (typeDest == MethodMapping.Type.REFACTORED_INLINE) return 'r';
                return 'g';
            case REFACTORED_INLINE:
                if (typeDest == MethodMapping.Type.BODY_CHANGE_ONLY) return 'g';
                return 'r';
            case REFACTORED_EXTRACT:
                if (typeDest == MethodMapping.Type.REFACTORED_RENAME || typeDest == MethodMapping.Type.REFACTORED_ARGUMENTS_REORDER || typeDest == MethodMapping.Type.REFACTORED_ARGUMENTS_RENAME)
                    return 'g';
                return 'r';
            case REFACTORED_ARGUMENTS_RENAME:
                if (typeDest == MethodMapping.Type.ARGUMENTS_CHANGE_ADD || typeDest == MethodMapping.Type.ARGUMENTS_CHANGE_REMOVE)
                    return 'y';
                if (typeDest == MethodMapping.Type.REFACTORED_INLINE) return 'r';
                return 'g';
            case REFACTORED_ARGUMENTS_REORDER:
                if (typeDest == MethodMapping.Type.REFACTORED_INLINE || typeDest == MethodMapping.Type.ARGUMENTS_CHANGE_ADD || typeDest == MethodMapping.Type.ARGUMENTS_CHANGE_REMOVE)
                    return 'r';
                return 'g';
            case ARGUMENTS_CHANGE_ADD:
            case ARGUMENTS_CHANGE_REMOVE:
                if (typeDest == MethodMapping.Type.REFACTORED_ARGUMENTS_RENAME) return 'y';
                if (typeDest == MethodMapping.Type.REFACTORED_MOVE || typeDest == MethodMapping.Type.REFACTORED_RENAME)
                    return 'g';
                return 'r';
            case ARGUMENTS_CHANGE_TYPE_CHANGE:
                if (typeDest == MethodMapping.Type.REFACTORED_MOVE || typeDest == MethodMapping.Type.REFACTORED_RENAME || typeDest == MethodMapping.Type.REFACTORED_ARGUMENTS_RENAME || typeDest == MethodMapping.Type.REFACTORED_ARGUMENTS_REORDER)
                    return 'g';
                return 'r';
            case BODY_CHANGE_ONLY:
                if (typeDest == MethodMapping.Type.REFACTORED_MOVE || typeDest == MethodMapping.Type.REFACTORED_RENAME || typeDest == MethodMapping.Type.REFACTORED_INLINE || typeDest == MethodMapping.Type.REFACTORED_ARGUMENTS_RENAME || typeDest == MethodMapping.Type.REFACTORED_ARGUMENTS_REORDER)
                    return 'g';
                return 'r';
        }
        return 'n';
    }

    public static void writeMappingsIntersectionToFile(String subsystemName, Map<String, MethodMapping> mappingAndroidOldNew, Map<String, MethodMapping> mappingAndroidOldModified, Map<String, MethodModel> projectOldMethods){
        File outputParentFolder = new File("/home/mehran/Thesis/Experiments/" + subsystemName);
        HashMap<String, Integer> intersectionsCount = new HashMap<>();

        for (String oldMethod : projectOldMethods.keySet()) {
            MethodMapping.Type typeAN = MethodMapping.Type.NOT_FOUND;
            if (mappingAndroidOldNew.containsKey(oldMethod)) {
                typeAN = mappingAndroidOldNew.get(oldMethod).getType();
            }
            MethodMapping.Type typeCM = MethodMapping.Type.NOT_FOUND;
            if (mappingAndroidOldModified.containsKey(oldMethod)) {
                typeCM = mappingAndroidOldModified.get(oldMethod).getType();
            }

            char intersectionColor = getIntersectionColor(typeAN, typeCM);
//            if (intersectionColor == 'y' || intersectionColor == 'r') {

                String intersectionLabel = typeAN.toString() + "-" + typeCM;
                intersectionsCount.put(intersectionLabel, intersectionsCount.getOrDefault(intersectionLabel, 0) + 1);
                File intersectionColorFolder = new File(outputParentFolder, String.valueOf(intersectionColor));
                File intersectionFolder = new File(intersectionColorFolder, intersectionLabel + "-" + intersectionsCount.get(intersectionLabel));


                Utils.writeToFile(new File(intersectionFolder, "ao"),
                        "// " + oldMethod + " " + projectOldMethods.get(oldMethod).getFilePath() + "\n"+
                                projectOldMethods.get(oldMethod).readFromFileUnformatted());

                if (typeAN != MethodMapping.Type.NOT_FOUND) {
                    Utils.writeToFile(new File(intersectionFolder, "an"),
                            "// " + mappingAndroidOldNew.get(oldMethod).getDestinationMethod() + " " + mappingAndroidOldNew.get(oldMethod).getDestinationMethod().getFilePath() + "\n"+
                                    mappingAndroidOldNew.get(oldMethod).getDestinationMethod().readFromFileUnformatted());
                }

                if (typeCM != MethodMapping.Type.NOT_FOUND) {
                    Utils.writeToFile(new File(intersectionFolder, "cm"),
                            "// " + mappingAndroidOldModified.get(oldMethod).getDestinationMethod() + " " + mappingAndroidOldModified.get(oldMethod).getDestinationMethod().getFilePath() + "\n"+
                                    mappingAndroidOldModified.get(oldMethod).getDestinationMethod().readFromFileUnformatted());
                }
//            }
        }
    }

    public static void printMappingsIntersection(Map<String, MethodMapping> mappingAndroidOldNew, Map<String, MethodMapping> mappingAndroidOldModified, Map<String, MethodModel> projectOldMethods) {
        for (String oldMethod : projectOldMethods.keySet()) {
            MethodMapping.Type typeAN = MethodMapping.Type.NOT_FOUND;
            if (mappingAndroidOldNew.containsKey(oldMethod)) {
                typeAN = mappingAndroidOldNew.get(oldMethod).getType();
            }
            MethodMapping.Type typeCM = MethodMapping.Type.NOT_FOUND;
            if (mappingAndroidOldModified.containsKey(oldMethod)) {
                typeCM = mappingAndroidOldModified.get(oldMethod).getType();
            }

            char intersectionColor = getIntersectionColor(typeAN, typeCM);
            if (intersectionColor == 'y' || intersectionColor == 'r') {
                System.out.println("***** Color: " + intersectionColor);
                System.out.println("***** AO: " + oldMethod + " " + projectOldMethods.get(oldMethod).getFilePath());
                System.out.println(projectOldMethods.get(oldMethod).readFromFileUnformatted());

                System.out.println("***** AN: " + typeAN);
                if (typeAN != MethodMapping.Type.NOT_FOUND) {
                    System.out.println("***** " + mappingAndroidOldNew.get(oldMethod).getDestinationMethod() + " " + mappingAndroidOldNew.get(oldMethod).getDestinationMethod().getFilePath());
                    System.out.println(mappingAndroidOldNew.get(oldMethod).getDestinationMethod().readFromFileUnformatted());
                }

                System.out.println("***** CM: " + typeCM);
                if (typeCM != MethodMapping.Type.NOT_FOUND) {
                    System.out.println("***** " + mappingAndroidOldModified.get(oldMethod).getDestinationMethod() + " " + mappingAndroidOldModified.get(oldMethod).getDestinationMethod().getFilePath());
                    System.out.println(mappingAndroidOldModified.get(oldMethod).getDestinationMethod().readFromFileUnformatted());
                }
                System.out.println("-------------------------------------------------------");
            }
        }
    }
}
