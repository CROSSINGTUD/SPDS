package test.cases.realworld;

import java.util.Map;

import test.core.selfrunning.AllocatedObject;

public class FixAfterInsertion<K,V>{
	
	
    private static final boolean RED   = false;
    private static final boolean BLACK = true;

    private transient Entry<K,V> root = null;
	static final class Entry<K,V> implements Map.Entry<K,V>, AllocatedObject {
        K key;
        V value;
        Entry<K,V> left = null;
        Entry<K,V> right = null;
        Entry<K,V> parent;
        boolean color = BLACK;

        /**
         * Make a new cell with given key, value, and parent, and with
         * {@code null} child links, and BLACK color.
         */
        Entry(K key, V value, Entry<K,V> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
        }

        /**
         * Returns the key.
         *
         * @return the key
         */
        public K getKey() {
            return key;
        }

        /**
         * Returns the value associated with the key.
         *
         * @return the value associated with the key
         */
        public V getValue() {
            return value;
        }

        /**
         * Replaces the value currently associated with the key with the given
         * value.
         *
         * @return the value associated with the key before this method was
         *         called
         */
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        public int hashCode() {
            int keyHash = (key==null ? 0 : key.hashCode());
            int valueHash = (value==null ? 0 : value.hashCode());
            return keyHash ^ valueHash;
        }

        public String toString() {
            return key + "=" + value;
        }
    }
	  void fixAfterInsertion(Entry<K,V> x) {

	        while (x != null && x != root && x.parent.color == RED) {
	            if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
	                Entry<K,V> y = rightOf(parentOf(parentOf(x)));
	                if (colorOf(y) == RED) {
	                    x = parentOf(parentOf(x));
	                } else {
	                    if (x == rightOf(parentOf(x))) {
	                        x = parentOf(x);
	                        rotateLeft(x);
	                    }
	                    rotateRight(parentOf(parentOf(x)));
	                }
	            } else {
	                Entry<K,V> y = leftOf(parentOf(parentOf(x)));
	                if (colorOf(y) == RED) {
	                    x = parentOf(parentOf(x));
	                } else {
	                    if (x == leftOf(parentOf(x))) {
	                        x = parentOf(x);
	                        rotateRight(x);
	                    }
	                    rotateLeft(parentOf(parentOf(x)));
	                }
	            }
	        }
	        root.color = BLACK;
	    }

	    private static <K,V> boolean colorOf(Entry<K,V> p) {
	        return (p == null ? BLACK : p.color);
	    }

	    private static <K,V> Entry<K,V> parentOf(Entry<K,V> p) {
	        return (p == null ? null: p.parent);
	    }

	    private static <K,V> Entry<K,V> leftOf(Entry<K,V> p) {
	        return (p == null) ? null: p.left;
	    }

	    private static <K,V> Entry<K,V> rightOf(Entry<K,V> p) {
	        return (p == null) ? null: p.right;
	    }

	    /** From CLR */
	     void rotateLeft(Entry<K,V> p) {
	        if (p != null) {
	            Entry<K,V> r = p.right;
	            p.right = r.left;
	            if (r.left != null)
	                r.left.parent = p;
	            r.parent = p.parent;
	            if (p.parent == null)
	                root = r;
	            else if (p.parent.left == p)
	                p.parent.left = r;
	            else
	                p.parent.right = r;
	            r.left = p;
	            p.parent = r;
	        }
	    }

	    /** From CLR */
	     void rotateRight(Entry<K,V> p) {
	        if (p != null) {
	            Entry<K,V> l = p.left;
	            p.left = l.right;
	            if (l.right != null) l.right.parent = p;
	            l.parent = p.parent;
	            if (p.parent == null)
	                root = l;
	            else if (p.parent.right == p)
	                p.parent.right = l;
	            else p.parent.left = l;
	            l.right = p;
	            p.parent = l;
	        }
	    }
}
