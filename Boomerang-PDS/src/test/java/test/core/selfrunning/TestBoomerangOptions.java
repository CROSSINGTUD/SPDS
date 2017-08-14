package test.core.selfrunning;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import boomerang.BoomerangOptions;
import boomerang.debug.DefaultBoomerangDebugger;
import boomerang.debug.IBoomerangDebugger;
import boomerang.debug.JSONOutputDebugger;

public abstract class TestBoomerangOptions extends BoomerangOptions {
	private File vizFile;

	public TestBoomerangOptions(Class testClassName, String testMethodName) {
		// this.setDebugger(new TestDebugger());
		if(testClassName.getName().contains("LongTest")){
			return;
		}
		vizFile = new File("target/IDEViz/" + testClassName.getName() + "/IDEViz-" + testMethodName + ".json");
		if (!vizFile.getParentFile().exists()) {
			try {
				Files.createDirectories(vizFile.getParentFile().toPath());
			} catch (IOException e) {
				throw new RuntimeException("Was not able to create directories for IDEViz output!");
			}
		}
		
	}

	@Override
	public IBoomerangDebugger getDebugger() {
		return (vizFile == null ? new DefaultBoomerangDebugger() :  new JSONOutputDebugger(vizFile));
	}
	public void removeVizFile() {
		if(vizFile == null)
			return;
		File parentFile = vizFile.getParentFile();
		if (vizFile.exists())
			vizFile.delete();
		try {
			if (isDirEmpty(parentFile.toPath()))
				parentFile.delete();
		} catch (IOException e) {
			throw new RuntimeException("Was not able to delete directories for IDEViz output!");
		}
	}

	private static boolean isDirEmpty(final Path directory) throws IOException {
		try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
			return !dirStream.iterator().hasNext();
		}
	}
}
