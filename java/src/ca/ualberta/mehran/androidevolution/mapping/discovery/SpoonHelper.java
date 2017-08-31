package ca.ualberta.mehran.androidevolution.mapping.discovery;

import ca.ualberta.mehran.androidevolution.mapping.MethodModel;
import ca.ualberta.mehran.androidevolution.mapping.discovery.MappingDiscoverer;
import spoon.Launcher;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.support.reflect.declaration.CtClassImpl;

import java.util.*;

public class SpoonHelper extends MappingDiscoverer {

    public SpoonHelper() {
        super("Spoon");
    }


    public Map<String, MethodModel> extractAllMethodsBySignature(String path, Map<String, String> classNameToFileMapping) {
        List<MethodModel> methods = extractAllMethods(path, classNameToFileMapping);
        Map<String, MethodModel> map = new HashMap<>();

        for (MethodModel method : methods) {
            map.put(method.getUMLFormSignature(), method);
        }

        return map;
    }


    public List<MethodModel> extractAllMethods(String path, Map<String, String> classNameToFileMapping) {
        onStart();
        List<MethodModel> methods = new ArrayList<>();

        Launcher spoon = new Launcher();
        spoon.addInputResource(path);
        spoon.getEnvironment().setNoClasspath(true);
        Factory factory = spoon.getFactory();
        factory.getEnvironment().setNoClasspath(true);
        spoon.getModelBuilder().build();
        for (CtType<?> s : factory.Class().getAll()) {
            methods.addAll(extractAllMethodsFromClass(s, classNameToFileMapping));
        }
        onFinish();
        return methods;
    }

    private List<MethodModel> extractAllMethodsFromClass(CtType<?> cls, Map<String, String> classNameToFileMapping) {

        List<MethodModel> result = new ArrayList<>();

        if (cls instanceof CtClassImpl<?>) {
            if (!(cls.getPosition() instanceof NoSourcePosition) && cls.isTopLevel()) {
                classNameToFileMapping.put(cls.getQualifiedName(), cls.getPosition().getFile().getAbsolutePath());
            }
            Set<CtConstructor<?>> constructors = ((CtClassImpl) cls).getConstructors();
            for (CtConstructor cons : constructors) {
                if (cons.getPosition() instanceof NoSourcePosition) continue;
                MethodModel model = new MethodModel(cons);
                result.add(model);
            }

            for (CtMethod<?> m : cls.getMethods()) {
                if (m.getPosition() instanceof NoSourcePosition) continue;
                MethodModel model = new MethodModel(m);
                result.add(model);
            }
            for (CtType<?> s : cls.getNestedTypes()) {
                result.addAll(extractAllMethodsFromClass(s, classNameToFileMapping));
            }
        }
        return result;
    }


}
