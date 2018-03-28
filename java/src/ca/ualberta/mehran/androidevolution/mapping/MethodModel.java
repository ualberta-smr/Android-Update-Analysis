package ca.ualberta.mehran.androidevolution.mapping;

import anonymous.authors.androidevolution.Utils;
import spoon.reflect.declaration.*;
import spoon.support.reflect.declaration.CtTypeImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MethodModel<T> {

    private CtElement methodOrConstructor;
//    private CtMethod<T> method;
//    private CtConstructor<T> constructor;

    private String filePath;
    private String relativeFilePath;
    private String packageName;
    private String className;
    private int lineStart = -1, lineEnd = -1;
    private String KNGFormSignature;
    private String UMLFormSignature;

    public MethodModel(CtMethod<T> method) {
        this.methodOrConstructor = method;
//        this.constructor = null;
        update();
    }

    public MethodModel(CtConstructor<T> constructor) {
        this.methodOrConstructor = constructor;
//        this.method = null;
        update();
    }

    public CtElement getMethodOrConstructor() {
        return methodOrConstructor;
    }

    public String getName() {
        if (isConstructor()) return getSimpleClassName();
        return ((CtNamedElement) methodOrConstructor).getSimpleName();
    }

    public String getFilePath() {
        return filePath;
    }

    public String getRelativeFilePath() {
        return relativeFilePath;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public String getFullClassName() {
        return getPackageName() + "." + getClassName();
    }

    public String getSimpleClassName() {
        if (className.contains("$")) {
            return className.substring(className.indexOf("$") + 1);
        } else return className;
    }

    public int getLineStart() {
        return lineStart;
    }

    public int getLineEnd() {
        return lineEnd;
    }


    private void update() {
//        CtElement methodOrConstructor;
//        if (isConstructor()) {
//            methodOrConstructor = constructor;
//        } else {
//            methodOrConstructor = method;
//        }
        if (methodOrConstructor.getParent() instanceof CtTypeImpl) {
            packageName = ((CtTypeImpl) methodOrConstructor.getParent()).getPackage().getQualifiedName();
            className = ((CtTypeImpl) methodOrConstructor.getParent()).getQualifiedName();
            className = className.substring(className.lastIndexOf('.') + 1);
        } else {
            packageName = null;
            className = null;
        }
        // TODO: Handle constructors with empty bodies
        try {
            filePath = methodOrConstructor.getPosition().getCompilationUnit().getFile().getAbsolutePath();
            lineStart = methodOrConstructor.getPosition().getLine();
            lineEnd = methodOrConstructor.getPosition().getEndLine();
        } catch (NullPointerException e) {
            filePath = null;
            lineStart = -1;
            lineEnd = -1;
        }
    }

    public List<String> getListOfParameterTypes() {
        List<String> result = new ArrayList<>();

        for (CtParameter<?> p : ((CtExecutable<?>) methodOrConstructor).getParameters()) {
            String pType = p.getType().getSimpleName();
            if (p.isVarArgs()) {
//                System.err.println("Don't know how to handle varags. Check " + this.getClass().getName());
                pType = pType.substring(0, pType.length() - 2);
            }

            result.add(pType);
        }
        return result;
    }

    public boolean isConstructor() {
        return (methodOrConstructor instanceof CtConstructor);
    }


    public CtMethod<T> getMethod() {
        return (CtMethod) methodOrConstructor;
    }

    public CtConstructor<T> getConstructor() {
        return (CtConstructor) methodOrConstructor;
    }

    public String readFromFile() {
        List<String> lines = Utils.readFile(new File(getFilePath()), getLineStart(), getLineEnd());
        StringBuilder daEntireThing = new StringBuilder();
        for (String line : lines) {
            daEntireThing.append(line.trim());
            daEntireThing.append("\n");
        }
        return daEntireThing.toString();
    }

    @Override
    public String toString() {
        if (packageName != null) {
            return getUMLFormSignature();
        }
        return super.toString();
    }

    public String getUMLFormSignature() {
        if (UMLFormSignature == null) {
            String parameters = "";
            for (String paramType : getListOfParameterTypes()) {
                parameters += paramType + ",";
            }
            if (!parameters.equals("")) {
                parameters = parameters.substring(0, parameters.length() - 1);
            }

            String returnType = "void";
            if (!isConstructor()) {
                returnType = ((CtTypedElement) getMethodOrConstructor()).getType().getSimpleName();
            }
            UMLFormSignature = getPackageName() + "." + getClassName().replace("$", ".") + "." + getName() + "(" + parameters + "):" + returnType;
        }
        return UMLFormSignature;
    }

    public String getKNGFormSignature() {
        if (KNGFormSignature == null) {
            String params = "";
            for (CtParameter<?> p : ((CtExecutable<?>) methodOrConstructor).getParameters()) {
                String pType = p.getType().getSimpleName();
                if (p.isVarArgs()) pType = pType.substring(0, pType.length() - 2);
                params += pType + ", ";
            }
            if (params.length() > 2) params = params.substring(0, params.length() - 2);

            KNGFormSignature = packageName + ":" +
                    className + "-" +
                    ((CtNamedElement) methodOrConstructor).getSimpleName() + "__["
                    + params + "]->void";
        }
        return KNGFormSignature;
    }
}
