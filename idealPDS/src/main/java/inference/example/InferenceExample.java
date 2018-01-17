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
