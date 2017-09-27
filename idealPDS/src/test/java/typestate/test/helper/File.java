package typestate.test.helper;
public class File {
  public void open() {

  }

  public void close() {
  };

  public int hashCode() {
    return 9;
  }
  
  public void wrappedClose(){
	  close();
  }
}
