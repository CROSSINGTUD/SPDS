package ideal;

import wpds.interfaces.State;

/**
 * Created by johannesspath on 07.12.17.
 */
public class Reachable implements State {

    private static Reachable instance;
    private Reachable(){}

    public static Reachable v(){
        if(instance == null)
            instance = new Reachable();
        return instance;
    }
}
