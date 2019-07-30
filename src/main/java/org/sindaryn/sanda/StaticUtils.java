package org.sindaryn.sanda;

public class StaticUtils {
    public static String toPascalCase(String string){
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }
}
