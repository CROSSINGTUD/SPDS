package test.cases.realworld;

import org.junit.Test;

import test.cases.realworld.FixAfterInsertion.Entry;
import test.core.selfrunning.AbstractBoomerangTest;

public class FixAfterInsertionLongTest  extends AbstractBoomerangTest{

	@Test
	public void main(){
		Entry<Object, Object> entry = new Entry<Object,Object>(null,null,null);
		new FixAfterInsertion<>().fixAfterInsertion(entry);
		Entry<Object, Object> query = entry.parent;
		queryFor(query);
	}

	@Test
	public void rotateLeftAndRightInLoop(){
		Entry<Object, Object> entry = new Entry<Object,Object>(null,null,null);
		while(true){
			new FixAfterInsertion<>().rotateLeft(entry);	
			new FixAfterInsertion<>().rotateRight(entry);
			if(staticallyUnknown())
				break;
		}
		Entry<Object, Object> query = entry.parent;
		queryFor(query);
	}
}
