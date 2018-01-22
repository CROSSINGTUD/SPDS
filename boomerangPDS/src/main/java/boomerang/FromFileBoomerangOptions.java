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
package boomerang;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class FromFileBoomerangOptions extends DefaultBoomerangOptions {
	
	private Properties options = new Properties();

	public FromFileBoomerangOptions(File optFile) {
		try {
			options.load(new FileInputStream(optFile));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean staticFlows() {
		return getBooleanFromFile("staticFlows");
	}

	private boolean getBooleanFromFile(String prop) {
		return Boolean.parseBoolean(getProperty(prop));
	}

	private String getProperty(String prop) {
		String property = options.getProperty(prop);
		if(property == null){
			throw new RuntimeException("Boomerang property file does not contain a value for " + prop);
		};
		return property;
	}

	@Override
	public boolean arrayFlows() {
		return getBooleanFromFile("arrayFlows");
	}

	@Override
	public boolean fastForwardFlows() {
		return true;
	}

	@Override
	public boolean typeCheck() {
		return getBooleanFromFile("typeCheck");
	}

	@Override
	public boolean onTheFlyCallGraph() {
		return getBooleanFromFile("on-the-fly-cg");
	}

	@Override
	public boolean throwFlows() {
		return false;
	}

	@Override
	public boolean callSummaries() {
		return getBooleanFromFile("callSummaries");
	}

	@Override
	public boolean fieldSummaries() {
		return getBooleanFromFile("fieldSummaries");
	}

	@Override
	public int analysisTimeoutMS() {
		return Integer.parseInt(getProperty("timeout"));
	}
}
