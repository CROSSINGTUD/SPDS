package test.cases.callgraph;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.junit.Test;
import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class ContextSensitivityMyListTest extends AbstractBoomerangTest {

  public void wrongContext() {
    List type = new MyList();
    method(type);
  }

  public Object method(List type) {
    Alloc alloc = new Alloc();
    type.add(alloc);
    return alloc;
  }

  @Test
  public void testOnlyCorrectContextInCallGraph() {
    wrongContext();
    MyCorrectList type = new MyCorrectList();
    Object alloc = method(type);
    queryFor(alloc);
  }

  static class MyList implements List {

    @Override
    public int size() {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public boolean isEmpty() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean contains(Object o) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public Iterator iterator() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object[] toArray() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object[] toArray(Object[] a) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean add(Object e) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean remove(Object o) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean containsAll(Collection c) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean addAll(Collection c) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean addAll(int index, Collection c) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean removeAll(Collection c) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean retainAll(Collection c) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public void clear() {
      // TODO Auto-generated method stub

    }

    @Override
    public Object get(int index) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object set(int index, Object element) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void add(int index, Object element) {
      unreachable();
    }

    public void unreachable() {
      // TODO Auto-generated method stub

    }

    @Override
    public Object remove(int index) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public int indexOf(Object o) {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public int lastIndexOf(Object o) {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public ListIterator listIterator() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public ListIterator listIterator(int index) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public List subList(int fromIndex, int toIndex) {
      // TODO Auto-generated method stub
      return null;
    }
  }

  static class MyCorrectList implements List {

    @Override
    public int size() {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public boolean isEmpty() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean contains(Object o) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public Iterator iterator() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object[] toArray() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object[] toArray(Object[] a) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean add(Object e) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean remove(Object o) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean containsAll(Collection c) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean addAll(Collection c) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean addAll(int index, Collection c) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean removeAll(Collection c) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean retainAll(Collection c) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public void clear() {
      // TODO Auto-generated method stub

    }

    @Override
    public Object get(int index) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object set(int index, Object element) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void add(int index, Object element) {
      // TODO Auto-generated method stub

    }

    @Override
    public Object remove(int index) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public int indexOf(Object o) {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public int lastIndexOf(Object o) {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public ListIterator listIterator() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public ListIterator listIterator(int index) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public List subList(int fromIndex, int toIndex) {
      // TODO Auto-generated method stub
      return null;
    }
  }
}
