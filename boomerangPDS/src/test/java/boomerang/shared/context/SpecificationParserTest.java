package boomerang.shared.context;

import org.junit.Test;

public class SpecificationParserTest {

  @Test
  public void specificationParserTest1(){
    Specification specification = Specification
        .create("<GO{F}java.lang.String: void <init>(ON{F}java.lang.String)>");
    assert specification.getMethodAndQueries().size() == 1;
  }

  @Test
  public void specificationParserTest2(){
    Specification specification = Specification
        .create("<ON{B}java.lang.String: void <init>(GO{B}java.lang.String)>)");
    assert specification.getMethodAndQueries().size() == 1;
  }
}
