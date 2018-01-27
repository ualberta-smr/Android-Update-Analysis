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

            case REFACTORED:
            case ARGUMENTS_CHANGE:
            case BODY_CHANGE_ONLY:
                return thisMethod.readFromFile().equals(otherMethod.readFromFile());
        }

        return super.equals(obj);
    }

    @Override
    public String toString() {
        return " -> " + getDestinationMethod() + "    " + getType().toString();
    }

    /**
     * IDENTICAL: No change in method's package, class, name, arguments and body. (SourcererCC)
     * REFACTORED: No change in method's arguments and body. Changes in method's package, class and name. (RefactoringMiner)
     * ARGUMENTS_CHANGE: Change in method's arguments and probably body. (ChangeDistiller and partly RefactoringMiner for class refactorings)
     * BODY_CHANGE_ONLY: No change in method's package, class, name, arguments. Changes at body level. (Script)
     * KNG_TRANSFORMATIONS: Matches discovered by Kim Miryung's tool.
     */
    public enum Type {
        IDENTICAL, REFACTORED, ARGUMENTS_CHANGE, BODY_CHANGE_ONLY, NOT_FOUND, ADDED, OTHER, KNG_TRANSFORMATIONS
    }

}
