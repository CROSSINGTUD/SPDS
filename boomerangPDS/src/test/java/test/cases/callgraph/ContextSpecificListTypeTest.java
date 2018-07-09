package test.cases.callgraph;

import org.junit.Test;
import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ContextSpecificListTypeTest extends AbstractBoomerangTest {

        public void wrongContext(){
            List list = new LinkedList();
            method(list);
        }

        public Object method(List list){
            Alloc alloc = new Alloc();
            list.add(alloc);
            return alloc;
        }

        @Test
        public void testListType(){
            List list = new ArrayList();
            Object query = method(list);
            queryFor(query);
        }
}
