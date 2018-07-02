package test.cases.callgraph;

import org.junit.Test;
import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class ContextSensitivityTest extends AbstractBoomerangTest {

    private SuperClass type;

    public void wrongContext(){
        type = new SubClass2();
    }


    public Object method(){
        Alloc alloc = new Alloc();
        type = new SubClass1();
        type.foo(alloc);
        return alloc;
    }

    @Test
    public void testOnlyCorrectContextInCallGraph(){
        Object alloc = method();
        queryFor(alloc);
    }

    public class SuperClass {

        public void foo(Object o){
            unreachable(o);
        }

    }

    class SubClass1 extends SuperClass {

        public void foo(Object o){

        }
    }

    class SubClass2 extends SuperClass {

        public void foo(Object o){
            unreachable(o);
        }
    }
}
