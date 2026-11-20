package huffman.core;

public class HuffmanCoder {
    public static class Node implements Comparable<Node> {
        public int symbol;
        public long freq;
        public Node left, right;

        // nodes pada tree huffman
        public Node (int symbol, long freq) {
            this.symbol = symbol;
            this.freq = freq;
        }

        public boolean isLeaf () { return left == null && right == null; }

        @Override
        public int compareTo(Node other) { return Long.compare(freq, other.freq); }

    }    

    private final long[] freq;
    private String codes[] = new String[256];
    private Node root;

    // hitung Frequensi Tiap byte
    static long[] countFreq (byte[] data) {
        long[] f = new long[256];
        for (byte b : data) { 
            f[b & 0xFF]++; 
        }
        return f;
    }

    public HuffmanCoder(byte[] data) throws Exception {
        this(countFreq(data));
    }

    // bisa dibuat dari tabel frekuensi saja
    // jadi dekompresi cukup baca tabel dari header, lalu bangun pohon yang sama lagi
    public HuffmanCoder(long[] freq) throws Exception {
        this.freq = freq.clone();
        Heap<Node> heap = new Heap<>();

        // masukkan tiap byte yg muncul sebagai daun ke heap
        for (int i = 0; i < 256; i++) {
            if (freq[i] > 0) heap.insert(new Node(i, freq[i]));
        }  
        if (heap.size() == 0) return ;

        // ambil 2 node dengan nilai terkecil dan gabung jadi 1 node baru
        while (heap.size() > 1) {
            Node a = heap.remove();
            Node b = heap.remove();
            Node p = new Node(-1, a.freq + b.freq);
            p.left = a;
            p.right = b;
            heap.insert(p);
        }
        root = heap.remove();

        if(root.isLeaf()) codes[root.symbol] = "0";
        else buildCodes(root, "");
    }

    // traversal tree dari root ke leaf
    private void buildCodes(Node n, String prefix) {
        if (n.isLeaf()) { codes[n.symbol] = prefix; return; }
        buildCodes(n.left, prefix + "0");
        buildCodes(n.right, prefix + "1");
    }

    public String codeOf(int symbol) { return codes[symbol]; }
    public Node getRoot() { return root; }
    public long[] getFreq () { return freq.clone();}
}