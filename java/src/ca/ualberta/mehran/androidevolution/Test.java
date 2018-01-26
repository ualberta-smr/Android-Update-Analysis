package ca.ualberta.mehran.androidevolution;

import ca.ualberta.mehran.androidevolution.mapping.EvolutionAnalyser;

import java.io.File;

public class Test {

    public static void main(String[] args) {
        EvolutionAnalyser evolutionAnalyser = new EvolutionAnalyser();
        String outputPath = new File("output/test/").getAbsolutePath();


        String AoAn = "/home/mehran/Thesis/Code/java/input/repos/packages_apps_Contacts/packages_apps_Contacts_android-6.0.1_r81_android-7.0.0_r1_CM_cm-13.0/android-6.0.1_r81_android-7.0.0_r1";
        String AoAn_Ao = "/home/mehran/Thesis/Code/java/input/repos/packages_apps_Contacts/packages_apps_Contacts_android-6.0.1_r81_android-7.0.0_r1_CM_cm-13.0/android-6.0.1_r81_android-7.0.0_r1/old";
        String AoAn_An = "/home/mehran/Thesis/Code/java/input/repos/packages_apps_Contacts/packages_apps_Contacts_android-6.0.1_r81_android-7.0.0_r1_CM_cm-13.0/android-6.0.1_r81_android-7.0.0_r1/new";


        String AoCm = "/home/mehran/Thesis/Code/java/input/repos/packages_apps_Contacts/packages_apps_Contacts_android-6.0.1_r81_android-7.0.0_r1_CM_cm-13.0/android-6.0.1_r81_cm-13.0";
        String AoCm_Ao = "/home/mehran/Thesis/Code/java/input/repos/packages_apps_Contacts/packages_apps_Contacts_android-6.0.1_r81_android-7.0.0_r1_CM_cm-13.0/android-6.0.1_r81_cm-13.0/old";
        String AoCm_Cm = "/home/mehran/Thesis/Code/java/input/repos/packages_apps_Contacts/packages_apps_Contacts_android-6.0.1_r81_android-7.0.0_r1_CM_cm-13.0/android-6.0.1_r81_cm-13.0/new";

        evolutionAnalyser.run("test_contacts", AoAn, AoAn_Ao, AoAn_An, AoCm, AoCm_Ao, AoCm_Cm, "/home/mehran/sourcerercc/", outputPath);
    }
}
