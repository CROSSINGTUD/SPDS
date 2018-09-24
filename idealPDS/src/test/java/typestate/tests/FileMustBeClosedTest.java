/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *  
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package typestate.tests;

import org.junit.Test;

import test.IDEALTestingFramework;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.impl.statemachines.FileMustBeClosedStateMachine;
import typestate.test.helper.File;
import typestate.test.helper.ObjectWithField;

public class FileMustBeClosedTest extends IDEALTestingFramework{
	@Test
	public void simple() {
		File file = new File();
		file.open();
		mustBeInErrorState(file);
		file.close();
		mustBeInAcceptingState(file);
	}
	@Test
	public void simple0() {
		File file = new File();
		file.open();
		escape(file);
		mustBeInErrorState(file);
	}
	@Test
	public void simple0a() {
		File file = new File();
		file.open();
		File alias = file;
		System.err.print("");
		escape(alias);
		mustBeInErrorState(file);
	}
	@Test
	public void simpleStrongUpdate() {
		File file = new File();
		File alias = file;
		file.open();
//		mustBeInErrorState(file);
		mustBeInErrorState(alias);
		alias.close();
//		mustBeInAcceptingState(alias);
		mustBeInAcceptingState(file);
	}
	@Test
	public void recursion() {
		File file = new File();
		file.open();
		mustBeInErrorState(file);
		recursive(file);
		mustBeInAcceptingState(file);
	}
	private void recursive(File file) {
		file.close();
		if(!staticallyUnknown()){
			File alias = file;
			recursive(alias);
			//TODO Commenting out this line introduces a bug
			int z = 2;
		}
	}
	private void escape(File other) {
		mustBeInErrorState(other);
	}

	@Test
	public void simple1() {
		File file = new File();
		File alias = file;
		alias.open();
		mustBeInErrorState(file);
		mustBeInErrorState(alias);
	}
	@Test
	public void simpleNoStrongUpdate() {
		File file = new File();
		File alias;
		if (staticallyUnknown()) {
			alias = file;
			alias.open();
			mustBeInErrorState(file);
		} else{
			alias = new File();
		}
		alias.open();
		mayBeInErrorState(file);
		mayBeInErrorState(alias);
	}
	@Test
	public void branching() {
		File file = new File();
		if (staticallyUnknown())
			file.open();
		mayBeInErrorState(file);
		file.close();
		mustBeInAcceptingState(file);
	}
	@Test
	public void branchingMay() {
		File file = new File();
		if (staticallyUnknown())
			file.open();
		else
			file.close();
		System.out.println(2);
		mayBeInErrorState(file);
		mayBeInAcceptingState(file);
	}
	@Test
	public void continued() {
		File file = new File();
		file.open();
		file.close();
		mustBeInAcceptingState(file);
		mustBeInAcceptingState(file);
		mustBeInAcceptingState(file);
		System.out.println(2);
	}
	@Test
	public void aliasing() {
		File file = new File();
		File alias = file;
		if (staticallyUnknown())
			file.open();
		mayBeInErrorState(file);
		alias.close();
		mustBeInAcceptingState(file);
		mustBeInAcceptingState(alias);
	}

	@Test
	public void summaryTest() {
		File file1 = new File();
		call(file1);
		int y = 1;
		file1.close();
		mustBeInAcceptingState(file1);
		File file = new File();
		File alias = file;
		call(alias);
		int x = 1;
		file.close();
		mustBeInAcceptingState(file);
		mustBeInAcceptingState(alias);
	}
	@Test
	public void simpleAlias() {
		File y = new File();
		File x = y;
		x.open();
		y.close();
		mustBeInAcceptingState(x);
		mustBeInAcceptingState(y);
	}
	private static void call(File alias) {
		alias.open();
	}

	@Test
	public void wrappedOpenCall() {
		File file1 = new File();
		call3(file1, file1);
		mustBeInErrorState(file1);
	}

	private static void call3(File other, File alias) {
		alias.open();
		mustBeInErrorState(alias);
	}

	@Test
	public void interprocedural() {
		File file = new File();
		file.open();
		flows(file, true);
		mayBeInAcceptingState(file);
		mayBeInErrorState(file);
	}

	private static void flows(File other, boolean b) {
		if (b)
			other.close();
	}
	@Test
	public void interprocedural2() {
		File file = new File();
		file.open();
		flows2(file, true);
		mustBeInAcceptingState(file);
	}

	private static void flows2(File other, boolean b) {
		other.close();
	}
	@Test
	public void intraprocedural() {
		File file = new File();
		file.open();
		if(staticallyUnknown())
			file.close();
		
		mayBeInAcceptingState(file);
		mayBeInErrorState(file);
	}
	@Test
	public void flowViaField() {
		ObjectWithField container = new ObjectWithField();
		flows(container);
		if (staticallyUnknown())
			container.field.close();

		mayBeInErrorState(container.field);
	}

	private static void flows(ObjectWithField container) {
		container.field = new File();
		File field = container.field;
		field.open();
	}
	@Test
	public void flowViaFieldDirect() {
		ObjectWithField container = new ObjectWithField();
		container.field = new File();
		File field = container.field;
		field.open();
		File f2 = container.field;
		mustBeInErrorState(f2);
	}
	
	@Test
	public void flowViaFieldDirect2() {
		ObjectWithField container = new ObjectWithField();
		container.field = new File();
		File field = container.field;
		field.open();
		mustBeInErrorState(container.field);
		File field2 = container.field;
		field2.close();
		mustBeInAcceptingState(container.field);
	}
	
	@Test
	public void flowViaFieldNotUnbalanced() {
		ObjectWithField container = new ObjectWithField();
		container.field = new File();
		open(container);
		if (staticallyUnknown()){
			container.field.close();
			mustBeInAcceptingState(container.field);
		}
		mayBeInErrorState(container.field);
		mayBeInAcceptingState(container.field);
	}

	private void open(ObjectWithField container) {
		File field = container.field;
		field.open();
	}

	@Test
	public void indirectFlow() {
		ObjectWithField a = new ObjectWithField();
		ObjectWithField b = a;
		flows(a, b);
		mayBeInAcceptingState(a.field);
		mayBeInAcceptingState(b.field);
	}

	private void flows(ObjectWithField aInner, ObjectWithField bInner) {
		File file = new File();
		file.open();
		aInner.field = file;
		File alias = bInner.field;
		mustBeInErrorState(alias);
		alias.close();
	}

	@Test
	public void noStrongUpdatePossible() {
		File b = null;
		File a = new File();
		a.open();
		File e = new File();
		e.open();
		if (staticallyUnknown()) {
			b = a;
		} else {
			b = e;
		}
		b.close();
		mayBeInErrorState(a);
		mustBeInAcceptingState(b);
	}

	@Test
	public void parameterAlias() {
		File file = new File();
		File alias = file;
		call(alias, file);
		mustBeInAcceptingState(file);
		mustBeInAcceptingState(alias);
	}

	private void call(File file1, File file2) {
		file1.open();
		//TODO: This test case fails if we comment out the next line
		int x = 1;
		file2.close();
		mustBeInAcceptingState(file1);
	}

	@Test
	public void parameterAlias2() {
		File file = new File();
		File alias = file;
		call2(alias, file);
		mayBeInErrorState(file);
		mayBeInErrorState(alias);
	}

	private void call2(File file1, File file2) {
		file1.open();
		if (staticallyUnknown())
			file2.close();
	}

	@Test
	public void aliasInInnerScope() {
		ObjectWithField a = new ObjectWithField();
		ObjectWithField b = a;
		File file = new File();
		file.open();
		bar(a, b, file);
		b.field.close();
		mustBeInAcceptingState(file);
		mustBeInAcceptingState(a.field);
	}

	@Test
	public void aliasSensitive() {
		ObjectWithField a = new ObjectWithField();
		ObjectWithField b = a;
		File file = new File();
		file.open();
		a.field = file;
		File loadedFromAlias = b.field;
		loadedFromAlias.close();
		mustBeInAcceptingState(file);
		mustBeInAcceptingState(a.field);
	}
	@Test
	public void noStrongUpdate() {
		ObjectWithField a = new ObjectWithField();
		ObjectWithField b = new ObjectWithField();
		File file = new File();
		if (staticallyUnknown()) {
			b.field = file;
		} else {
			a.field = file;
		}
		a.field.open();
		b.field.close();
		// Debatable
		mayBeInAcceptingState(file);
	}

	@Test
	public void unbalancedReturn1() {
		File second = createOpenedFile();
		mustBeInErrorState(second);
	}

	@Test
	public void unbalancedReturn2() {
		File first = createOpenedFile();
		int x= 1;
		clse(first);
		mustBeInAcceptingState(first);
		File second = createOpenedFile();
		second.hashCode();
		mustBeInErrorState(second);
	}

	@Test
	public void unbalancedReturnAndBalanced() {
		File first = createOpenedFile();
		int x= 1;
		clse(first);
		mustBeInAcceptingState(first);
	}
	private static void clse(File first) {
		first.close();
	}

	public static File createOpenedFile() {
		File f = new File();
		f.open();
		mustBeInErrorState(f);
		return f;
	}

	private void bar(ObjectWithField a, ObjectWithField b, File file) {
		a.field = file;
	}

	@Test
	public void lateWriteToField() {
		ObjectWithField a = new ObjectWithField();
		ObjectWithField b = a;
		File file = new File();
		bar(a, file);
		File c = b.field;
		c.close();
		mustBeInAcceptingState(file);
	}

	private void bar(ObjectWithField a, File file) {
		file.open();
		a.field = file;
		File whoAmI = a.field;
		mustBeInErrorState(whoAmI);
	}

	@Test
	public void fieldStoreAndLoad1() {
		ObjectWithField container = new ObjectWithField();
		File file = new File();
		container.field = file;
		File a = container.field;
		a.open();
		mustBeInErrorState(a);
		mustBeInErrorState(file);
	}

	@Test
	public void fieldStoreAndLoad2() {
		ObjectWithField container = new ObjectWithField();
		container.field = new File();
		ObjectWithField otherContainer = new ObjectWithField();
		File a = container.field;
		otherContainer.field = a;
		flowsToField(container);
//		mustBeInErrorState( container.field);
		mustBeInErrorState(a);
	}

	private void flowsToField(ObjectWithField parameterContainer) {
		File field = parameterContainer.field;
		field.open();
		mustBeInErrorState(field);
		File aliasedVar = parameterContainer.field;
		mustBeInErrorState(aliasedVar);
	}

	@Test
	public void wrappedClose() {
		File file = new File();
		File alias = file;
		alias.open();
		mustBeInErrorState(alias);
		mustBeInErrorState(file);
		file.wrappedClose();
		mustBeInAcceptingState(alias);
		mustBeInAcceptingState(file);
	}
	@Test
	public void wrappedClose2() {
		File file = new File();
		file.open();
		mustBeInErrorState(file);
		wrappedParamClose(file);
		mustBeInAcceptingState(file);
	}
	
	@Test
	public void wrappedOpen2() {
		File file = new File();
		wrappedParamOpen(file);
		mustBeInErrorState(file);
	}
	private void wrappedParamOpen(File a) {
		openCall(a);
	}
	private void openCall(File f) {
		f.open();
	}
	@Test
	public void wrappedClose1() {
		File file = new File();
		file.open();
		mustBeInErrorState(file);
		cls(file);
		mustBeInAcceptingState(file);
	}
	private void wrappedParamClose(File o1) {
		cls(o1);
	}

	private void cls(File o2) {
		o2.close();
	}
	@Test
	public void wrappedOpen() {
		File file = new File();
		change(file);
		mustBeInErrorState(file);
	}
	private void change(File other) {
		other.open();		
	}
	@Test
	public void multipleStates() {
		File file = new File();
		file.open();
		mustBeInErrorState(file);
		mustBeInErrorState(file);
		file.close();
		mustBeInAcceptingState(file);
		mustBeInAcceptingState(file);
	}

	@Test
	public void doubleBranching() {
		File file = new File();
		if (staticallyUnknown()) {
			file.open();
			if (staticallyUnknown())
				file.close();
		} else if (staticallyUnknown())
			file.close();
		else {
			System.out.println(2);
		}
		mayBeInErrorState(file);
	}
	@Test
	public void whileLoopBranching() {
		File file = new File();
		while(staticallyUnknown()){
			if (staticallyUnknown()) {
				file.open();
				if (staticallyUnknown())
					file.close();
			} else if (staticallyUnknown())
				file.close();
			else {
				System.out.println(2);
			}
		}
		mayBeInErrorState(file);
	}
	
	static File v;
	
	@Test
	public void staticFlow(){
		File a = new File();
		v = a;
		v.open();
		foo();
		mustBeInErrorState(v);
		v.close();
		mustBeInAcceptingState(v);
	} 
	@Test
	public void staticFlowSimple(){
		File a = new File();
		v = a;
		v.open();
		mustBeInErrorState(v);
	} 

	public static void foo() {
	}
	@Test
	public void storedInObject(){
		InnerObject o = new InnerObject();
		File file = o.file;
		mustBeInErrorState(file);
	}

	public static class InnerObject{
		public File file;

		public InnerObject(){
			this.file = new File();
			this.file.open();
		}

		public InnerObject(String string) {
			this.file = new File();
		}

		public void doClose() {
			mustBeInErrorState(file);
			this.file.close();
			mustBeInAcceptingState(file);
		}

		public void doOpen() {
			this.file.open();
			mustBeInErrorState(file);
		}
	}
	
	@Test
	public void storedInObject2(){
		InnerObject o = new InnerObject("");
		o.doOpen();
		o.doClose();
		mustBeInAcceptingState(o.file);
	}
	@Override
	protected TypeStateMachineWeightFunctions getStateMachine() {
		return new FileMustBeClosedStateMachine();
	}
}
