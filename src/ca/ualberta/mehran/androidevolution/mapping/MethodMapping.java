package ca.ualberta.mehran.androidevolution.mapping;

public class MethodMapping {

    private MethodModel destionationMethod;
    private Type type;

    public MethodMapping(MethodModel destionationMethod, Type type) {
        this.destionationMethod = destionationMethod;
        this.type = type;
    }

    public MethodModel getDestinationMethod() {
        return destionationMethod;
    }

    public Type getType() {
        return type;
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
