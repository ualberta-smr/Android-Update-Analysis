
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mehran on 7/17/17.
 */
public abstract class MappingDiscoverer {

    private long startTime;
    private String helperName;

    MappingDiscoverer(String helperName) {
        this.helperName = helperName;
    }

    void onStart() {
        startTime = System.currentTimeMillis();
        log(helperName + " started");
    }

    void onFinish() {
        long totalTime = System.currentTimeMillis() - startTime;
        log(helperName + " took " + (totalTime/1000) + " seconds");
    }

    private void log(String message){
        String timeStamp = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss z").format(new Date());
        System.out.println(timeStamp + " " + message);
    }
}
