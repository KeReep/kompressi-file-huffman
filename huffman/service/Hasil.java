package huffman.service;

public class Hasil {
    public String jenis;            // "Kompres" atau "Dekompres"
    public String mode = "-";       // "HUF1 (Huffman)" atau "HUF0 (mentah)"
    public boolean mentah;
    public long awal, akhir, ms;    // ukuran file masuk, ukuran file keluar, waktu
    public long panjangAsli;        // jumlah byte data asli
    public int jumlahSimbol;        // jumlah jenis byte yang muncul
    public long totalBit;           // bit data kalau memakai kode Huffman
    public long headerByte;
    public long[] freq;             // null kalau tidak ada tabel
    public String[] kode;           // kode tiap byte (null kalau byte tidak muncul)
    public double entropi;          // bit per byte

    // entropi = batas terbaik panjang kode rata-rata
    static double hitungEntropi(long[] f, long n) {
        if (n == 0) return 0;
        double h = 0;
        for (long x : f) {
            if (x > 0) {
                double p = (double) x / n;
                h -= p * (Math.log(p) / Math.log(2));
            }
        }
        return h;
    }

    // panjang kode rata-rata = total bit / jumlah byte asli
    public double rataKode() {
        return panjangAsli > 0 ? (double) totalBit / panjangAsli : 0;
    }

    @Override
    public String toString() {
         // persen = ukuran akhir / ukuran awal (saat dekompres bisa lebih dari 100%)
        String rasio = awal > 0 ? String.format("%.1f%%", akhir * 100.0 / awal) : "-";
        return jenis + ": " + awal + " -> " + akhir + " byte (" + rasio + "), " + ms + " ms";
    }
}