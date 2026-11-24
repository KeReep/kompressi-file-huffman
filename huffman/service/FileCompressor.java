package huffman.service;

import huffman.core.BitReader;
import huffman.core.BitWriter;
import huffman.core.HuffmanCoder;
import java.io.*;

import java.nio.file.Files;
import java.util.Arrays;

public class FileCompressor {
    // penanda di awal file .huf, buat bedain format file
    // HUF1 = hasil kompresi Huffman, HUF0 = file asli disimpan apa adanya
    private static final byte[] FORMAT_FILE     = {'H', 'U', 'F', '1'};
    private static final byte[] FORMAT_FILE_RAW = {'H', 'U', 'F', '0'};

    public static Hasil compress(File in, File outFile) throws Exception {
        long start = System.nanoTime();
        byte[] data = Files.readAllBytes(in.toPath());
        HuffmanCoder h = new HuffmanCoder(data);
        long[] freq = h.getFreq();

        int count = 0;
        long totalBit = 0;
        String[] kode = new String[256];
        for (int i = 0; i < 256; i++) {
            if (freq[i] > 0) {
                count++;
                kode[i] = h.codeOf(i);
                totalBit += freq[i] * kode[i].length();
            }
        }
        // header = 14 byte tetap (4 penanda + 8 panjang asli + 2 jumlah simbol)
        // ditambah 9 byte per simbol (1 byte simbol + 8 byte frekuensi)
        long headerHuffman = 14 + 9L * count;

        // Kalau hanya ada 1 jenis byte, dekompresor tidak membaca bit sama sekali,
        // jadi bit data tidak perlu ditulis.
        long bitDitulis = (count == 1) ? 0 : totalBit;
        
        // (bit + 7) / 8 = bit dibulatkan ke atas jadi byte
        long ukuranHuffman = headerHuffman + (bitDitulis + 7) / 8;
        long ukuranMentah = 4 + data.length;

         // kalau Huffman tidak lebih kecil (biasanya file kecil, header lebih besar), simpan apa adanya
        boolean mentah = ukuranMentah <= ukuranHuffman;

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(outFile)))) {

            if (mentah) {
                out.write(FORMAT_FILE_RAW);
                out.write(data);
            } else {
                // urutan header harus sama persis dengan urutan baca di decompress
                out.write(FORMAT_FILE);                 // HUF1
                out.writeLong(data.length);             // Panjang asli
                out.writeShort(count);                  // Jumlah simbol
                for (int i = 0; i < 256; i++) {
                    if (freq[i] > 0) {
                        out.writeByte(i);               // Simbol
                        out.writeLong(freq[i]);         // Frequensi
                    }
                }
                if (count > 1) {
                    BitWriter bw = new BitWriter(out);
                    for (byte b : data) {
                        bw.write(h.codeOf(b & 0xFF));   // Kode tiap byte -> bit
                    }
                    bw.flush();                         // Sisa byte + padding
                }
            }
        }

        Hasil r = new Hasil();
        r.jenis = "Kompres";
        r.mentah = mentah;
        r.mode = mentah ? "HUF0 (mentah)" : "HUF1 (Huffman)";
        r.awal = in.length();
        r.akhir = outFile.length();
        r.ms = (System.nanoTime() - start) / 1_000_000;
        r.panjangAsli = data.length;
        r.jumlahSimbol = count;
        r.totalBit = totalBit;
        r.headerByte = mentah ? 4 : headerHuffman;
        r.freq = freq;
        r.kode = kode;
        r.entropi = Hasil.hitungEntropi(freq, data.length);
        return r;
    }

    public static Hasil decompress(File in, File outFile) throws Exception {
        long start = System.nanoTime();
        Hasil r = new Hasil();
        r.jenis = "Dekompres";

        try (DataInputStream dataIn = new DataInputStream(
                new BufferedInputStream(new FileInputStream(in)));
             BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream(outFile))) {

            byte[] format = new byte[4];
            dataIn.readFully(format);

            if (Arrays.equals(format, FORMAT_FILE_RAW)) {
                dataIn.transferTo(out);
                r.mentah = true;
                r.mode = "HUF0 (mentah)";
                r.headerByte = 4;

            } else if (Arrays.equals(format, FORMAT_FILE)) {
                long length = dataIn.readLong();
                int count = dataIn.readShort();
                long[] freq = new long[256];

                // bangun ulang tabel freq
                for (int i = 0; i < count; i++) {
                    int sym = dataIn.readUnsignedByte();
                    freq[sym] = dataIn.readLong();
                }

                // pohon dibangun ulang dari tabel frekuensi, hasilnya sama dengan saat kompresi
                HuffmanCoder h = new HuffmanCoder(freq);
                HuffmanCoder.Node root = h.getRoot();

                if (length > 0) {
                    if (root.isLeaf()) {
                        // cuma 1 jenis byte, tulis byte itu sebanyak panjang asli
                        for (long i = 0; i < length; i++) out.write(root.symbol);
                    } else {
                        BitReader br = new BitReader(dataIn);
                        HuffmanCoder.Node node = root;
                        long written = 0;
                         // berhenti berdasarkan panjang asli, jadi bit padding di akhir diabaikan
                        while (written < length) {
                            int bit = br.readBit();
                            if (bit == -1) throw new IOException("Data rusak: bit habis terlalu cepat");
                            node = (bit == 0) ? node.left : node.right;
                            if (node.isLeaf()) {
                                out.write(node.symbol);
                                written++;
                                node = root;
                            }
                        }
                    }
                }

                String[] kode = new String[256];
                long totalBit = 0;
                for (int i = 0; i < 256; i++) {
                    if (freq[i] > 0) {
                        kode[i] = h.codeOf(i);
                        totalBit += freq[i] * kode[i].length();
                    }
                }
                
                r.mode = "HUF1 (Huffman)";
                r.freq = freq;
                r.kode = kode;
                r.totalBit = totalBit;
                r.jumlahSimbol = count;
                r.headerByte = 14 + 9L * count;
                r.panjangAsli = length;
                r.entropi = Hasil.hitungEntropi(freq, length);

            } else {
                throw new IOException("Bukan file .huf yang valid");
            }
        }

        r.awal = in.length();
        r.akhir = outFile.length();
        r.ms = (System.nanoTime() - start) / 1_000_000;
        if (r.mentah) r.panjangAsli = r.akhir;
        return r;
    }
}