package ca.ualberta.mehran.androidevolution.mapping;

public class MethodMapping {

    private MethodModel destinationMethod;
    private Type type;

    public MethodMapping(MethodModel destinationMethod, Type type) {
        this.destinationMethod = destinationMethod;
        this.type = type;
    }

    public MethodModel getDestinationMethod() {
        return destinationMethod;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof MethodMapping)) return false;
        MethodMapping otherMapping = (MethodMapping) obj;

        if (getType() != otherMapping.getType()) return false;
        MethodModel otherMethod = otherMapping.getDestinationMethod();
        MethodModel thisMethod = getDestinationMethod();

        if (otherMethod == null || thisMethod == null) return false;

        if (!thisMethod.getUMLFormSignature().equals(otherMethod.getUMLFormSignature())) return false;

        switch (getType()) {
            case IDENTICAL:
            case NOT_FOUND:
            case ADDED:
            case OTHER:
                return true;

            case REFACTORED_MOVE:
            case REFACTORED_RENAME:
            case REFACTORED_INLINE:
            case REFACTORED_EXTRACT:
            case REFACTORED_ARGUMENTS_RENAME:
            case REFACTORED_ARGUMENTS_REORDER:
            case ARGUMENTS_CHANGE_ADD:
            case ARGUMENTS_CHANGE_REMOVE:
            case ARGUMENTS_CHANGE_TYPE_CHANGE:
            case BODY_CHANGE_ONLY:
                return thisMethod.readFromFile().equals(otherMethod.readFromFile());
        }

        return super.equals(obj);
    }

    /**
     * IDENTICAL: No change in method's package, class, name, arguments and body. (SourcererCC)
     * REFACTORED: No change in method's arguments and body. Changes in method's package, class and name. (RefactoringMiner)
     * ARGUMENTS_CHANGE: Change in method's arguments and probably body. (ChangeDistiller and partly RefactoringMiner for class refactorings)
     * BODY_CHANGE_ONLY: No change in method's package, class, name, arguments. Changes at body level. (Script)
     * KNG_TRANSFORMATIONS: Matches discovered by Kim Miryung's tool.
     */
    public enum Type {
        IDENTICAL,
        REFACTORED_MOVE, REFACTORED_RENAME, REFACTORED_INLINE, REFACTORED_EXTRACT, REFACTORED_ARGUMENTS_RENAME, REFACTORED_ARGUMENTS_REORDER,
        ARGUMENTS_CHANGE_ADD, ARGUMENTS_CHANGE_REMOVE, ARGUMENTS_CHANGE_TYPE_CHANGE,
        BODY_CHANGE_ONLY, NOT_FOUND, ADDED, OTHER, KNG_TRANSFORMATIONS
    }

}
