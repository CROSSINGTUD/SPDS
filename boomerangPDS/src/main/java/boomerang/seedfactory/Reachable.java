package boomerang.seedfactory;

import wpds.interfaces.State;

/**
 * Created by johannesspath on 07.12.17.
 */
public class Reachable implements State {

    private static Reachable instance;
    private static Reachable entry;
    private Reachable(){}

    public static Reachable v(){
        if(instance == null)
            instance = new Reachable();
        return instance;
    }

    public static Reachable entry(){
        if(entry == null)
        	entry = new Reachable();
        return entry;
    }
    @Override
    public String toString() {
    	return "0";
    }
}
