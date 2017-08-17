package wpds.interfaces;

public interface ReachabilityListener<D extends State> {
	void reachable(D node);
}
