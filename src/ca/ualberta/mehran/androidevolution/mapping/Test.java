package ca.ualberta.mehran.androidevolution.mapping;

import ca.ualberta.mehran.androidevolution.mapping.discovery.implementation.ChangeDistillerHelper;

import java.util.HashMap;


public class Test {
    public static void main(String[] args) {
        new ChangeDistillerHelper().identifyMethodArgumentChanges(
                "/Users/mehran/Android API/settings/android1_android2/settings-1.0.0",
                "/Users/mehran/Android API/settings/android1_android2/settings-2.0.0",
                null,
                null,
                null,
                null,
                new HashMap<>());
    }

}
