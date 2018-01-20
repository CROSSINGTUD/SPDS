package test.cases.sets;

public class MyMap {
	MyInnerMap m = new MyInnerMap();
	public void add(Object o){
		MyInnerMap map = this.m;
		map.innerAdd(o);
		MyInnerMap alias = this.m;
		Object retrieved = alias.content;
	}
	public Object get(){
		MyInnerMap map = this.m;
		return map.get();
	}
}
