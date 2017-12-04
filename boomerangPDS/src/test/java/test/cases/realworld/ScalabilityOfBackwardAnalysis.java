package test.cases.realworld;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import test.core.AbstractBoomerangTest;

public class ScalabilityOfBackwardAnalysis extends AbstractBoomerangTest{
	@Test
	public void simpleButDifficult() throws IOException{
		//This test case scales in Whole Program PTS Analysis when we do NOT track subtypes of Exceptions.
		//The backward analysis runs into scalability problen, when we enable unbalanced flows. 
		InputStream inputStream = new FileInputStream("");
		inputStream.close();
		inputStream.read();
		queryFor(inputStream);
	}
	
	@Override
	protected boolean includeJDK() {
		return true;
	}
}
