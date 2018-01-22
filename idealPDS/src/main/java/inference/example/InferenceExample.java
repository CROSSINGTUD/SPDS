/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package inference.example;

public class InferenceExample {
	public static void main(String...args){
		File file = new File();
		file.open();
		staticCallOnFile(file);
		file.open();
	}
	private static void staticCallOnFile(File file) {
		file.close();
	}
	public static class File{

		public void open() {
		}

		public void close() {
		}
		
	}
}
