package huffman.service;

import java.io.*;
import huffman.core.HuffmanCoder;

public class HufInspector {

    // maxByte = berapa byte data yang ditampilkan dalam biner
    public static String inspect(File f, int maxByte) throws Exception {
        StringBuilder sb = new StringBuilder();

        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(f)))) {

            byte[] magic = new byte[4];
            in.readFully(magic);
            // ISO-8859-1 = 1 byte jadi 1 karakter, jadi penanda terbaca apa adanya
            String m = new String(magic, "ISO-8859-1");
            sb.append("Penanda       : ").append(m).append('\n');

            if (m.equals("HUF0")) {
                sb.append("Mode mentah: isi file asli disimpan apa adanya (")
                  .append(f.length() - 4)
                  .append(" byte). Tidak ada kode Huffman.\n");
                return sb.toString();
            }
            if (!m.equals("HUF1")) {
                throw new IOException("Bukan file .huf yang valid");
            }

            long panjang = in.readLong();
            int count = in.readShort();
            long[] freq = new long[256];
            for (int i = 0; i < count; i++) {
                int sym = in.readUnsignedByte();
                freq[sym] = in.readLong();
            }
            // 14 byte tetap + 9 byte per simbol (ukuran header harus sama)
            long headerSize = 4 + 8 + 2 + 9L * count;

            sb.append("Panjang asli  : ").append(panjang).append(" byte\n");
            sb.append("Jumlah simbol : ").append(count).append("\n\n");

            // tabel kode
            HuffmanCoder h = new HuffmanCoder(freq);
            long totalBit = 0;
            sb.append("Tabel kode Huffman:\n");
            sb.append(String.format("%-5s %-6s %-10s %s%n", "byte", "huruf", "frekuensi", "kode"));
            for (int i = 0; i < 256; i++) {
                if (h.codeOf(i) != null) {
                    String huruf = (i >= 33 && i <= 126) ? String.valueOf((char) i)
                                 : (i == 32 ? "spasi" : "-");
                    sb.append(String.format("%-5d %-6s %-10d %s%n", i, huruf, freq[i], h.codeOf(i)));
                    totalBit += freq[i] * h.codeOf(i).length();
                }
            }

            // sisa file setelah header = data terkompresi
            long dataByte = f.length() - headerSize;
            sb.append('\n');
            sb.append("Bit data (tanpa padding) : ").append(totalBit).append(" bit\n");
            sb.append("Jika 8 bit per byte      : ").append(panjang * 8).append(" bit\n");
            sb.append("Header                   : ").append(headerSize).append(" byte\n");
            sb.append("Data terkompresi         : ").append(dataByte).append(" byte\n\n");

            // bit data
            int tampil = (int) Math.min(maxByte, dataByte);
            sb.append("Isi data dalam biner (").append(tampil).append(" byte pertama):\n");
            for (int i = 0; i < tampil; i++) {
                int v = in.read();
                if (v == -1) break;
                // | 0x100 menambah angka 1 di depan, lalu substring(1) membuangnya
                // biar nol di depan tetap muncul (jadi selalu 8 digit)
                String bits = Integer.toBinaryString(v | 0x100).substring(1);
                sb.append(bits).append(' ');
                if ((i + 1) % 8 == 0) sb.append('\n'); // ganti baris tiap 8 byte
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}