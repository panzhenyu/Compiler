package DataStructure;

public class MyHashMap<K, V>
{
    public static void main(String[] args)
    {
        MyHashMap<Integer, Integer> h = new MyHashMap<>();
    }

    static class Node<K, V>
    {
        int hash;
        final K key;
        V value;
        Node<K, V> next;
        public Node(int hash, K key, V value, Node<K, V> next)
        {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    final static int DEFAULT_CAPACITY = 1 << 4;
    final static int MAXIMUM_CAPACITY = 1 << 30;
    final static float DEFAULT_LOAD_FACTORY = 0.75f;

    Node<K, V>[] table;
    int size;
    int capacity;
    float factory;
    int threshold;

    public MyHashMap(int initCapacity, float loadFactory)
    {
        if(initCapacity < 0)
            throw new IllegalArgumentException("Illegal argument: " + initCapacity);
        if(initCapacity > MAXIMUM_CAPACITY)
            initCapacity = MAXIMUM_CAPACITY;
        if(loadFactory<=0 || loadFactory>1)
            throw new IllegalArgumentException("Illegal argument: " + loadFactory);
        this.capacity = initCapacity;
        this.factory = loadFactory;
    }

    public MyHashMap(int initCapcity)
    {
        this(initCapcity, DEFAULT_LOAD_FACTORY);
    }

    public MyHashMap()
    {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTORY);
    }

    public int getSize()
    {
        return size;
    }

    public boolean isEmpty()
    {
        return size == 0;
    }

    public V put(K key, V value)
    {
        return putValue(key, value);
    }

    final V putValue(K key, V value)
    {
        Node<K, V>[] tab; Node<K, V> p; int pos, n, hash = hash(key);
        if((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
        if((p = tab[pos = hash & (n - 1)]) == null)
            tab[pos] = new Node<>(hash, key, value, null);
        else {
            while(p.next != null && (p.hash != hash || p.key != key))
                p = p.next;
            if(p == null)
                p.next = new Node<>(hash, key, value, null);
            else
                p.value = value;
        }
        return value;
    }

    public V get(K key)
    {
        Node<K, V> e;
        return (e = getNode(key)) == null ? null : e.value;
    }

    final Node<K, V> getNode(K key)
    {
        Node<K, V>[] tab; Node<K, V> first; int hash = hash(key), n;
        if((tab = table) != null && (n = table.length) > 0 && (first = tab[hash & (n-1)]) != null)
        {
            do {
                if(first.hash == hash && first.key == key)
                    return first;
                first = first.next;
            } while (first != null);
        }
        return null;
    }

//    public boolean containsKey(K key) {}
//    public Iterable<K> keySet() {}
    final Node<K, V>[] resize()
    {
        Node<K, V>[] newTable = new Node[capacity];
        table = newTable;
        return newTable;
    }
    final int hash(K key)
    {
        int h;
        return key == null ? 0 : (h = key.hashCode())^(h >>> 16);
    }
}
