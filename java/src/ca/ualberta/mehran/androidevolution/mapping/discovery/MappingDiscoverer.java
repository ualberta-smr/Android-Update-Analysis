package ca.ualberta.mehran.androidevolution.mapping.discovery;

import static ca.ualberta.mehran.androidevolution.Utils.log;

public abstract class MappingDiscoverer {

    private long startTime;
    private String helperName;

    protected MappingDiscoverer(String helperName) {
        this.helperName = helperName;
    }

    protected void onStart() {
        startTime = System.currentTimeMillis();
        log(helperName + " started");
    }

    protected void onFinish() {
        long totalTime = System.currentTimeMillis() - startTime;
        log(helperName + " took " + (totalTime/1000) + " seconds");
    }

}
