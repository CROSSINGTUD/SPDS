package test.cases.hashmap;

class Entry<K, V> extends Node<K, V> {
	Entry<K, V> before, after;

	Entry(int hash, K key, V value, Node<K, V> next) {
		super(hash, key, value, next);
	}
}
