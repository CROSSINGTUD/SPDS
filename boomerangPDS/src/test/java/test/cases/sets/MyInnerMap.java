package test.cases.sets;

public class MyInnerMap {
	public Object content= null;
	public void innerAdd(Object o) {
		content = o;
	}
	
	public Object get(){
		return content;
	}
}
