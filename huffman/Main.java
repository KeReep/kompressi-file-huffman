package huffman;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import huffman.service.FileCompressor;

public class Main {

    private static void bantuan() {
        System.out.println("Pemakaian:");
        System.out.println("  c <file_asli> <file.huf>      kompres");
        System.out.println("  d <file.huf> <file_hasil>     dekompres");
        System.out.println("  t <file_asli>                 uji round trip");
    }

    // tampilkan error lalu keluar (kode 1 = gagal)
    private static void gagal(String pesan) {
        System.err.println("ERROR: " + pesan);
        System.exit(1);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            bantuan();
            return;
        }

        String mode = args[0];
        if (!mode.equals("c") && !mode.equals("d") && !mode.equals("t")) {
            System.err.println("Mode tidak dikenal: " + mode);
            bantuan();
            System.exit(1);
        }

        // mode t butuh 2 argumen (mode + file), mode c dan d butuh 3
        int perlu = mode.equals("t") ? 2 : 3;
        if (args.length < perlu) {
            System.err.println("Argumen kurang untuk mode '" + mode + "'.");
            bantuan();
            System.exit(1);
        }

        try {
            File a = new File(args[1]);
            if (!a.isFile()) gagal("File tidak ditemukan: " + a);

            if (mode.equals("t")) {
                roundTrip(a);
                return;
            }

            // getCanonicalFile = path lengkap, biar tetap ketahuan sama walau penulisannya beda
            File b = new File(args[2]);
            if (a.getCanonicalFile().equals(b.getCanonicalFile())) {
                gagal("File tujuan tidak boleh sama dengan file asal.");
            }

            if (mode.equals("c")) {
                System.out.println(FileCompressor.compress(a, b));
            } else {
                System.out.println(FileCompressor.decompress(a, b));
            }
        } catch (Exception e) {
            gagal(e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    // uji: kompres, dekompres, lalu cek hasilnya sama persis dengan file asli
    private static void roundTrip(File a) throws Exception {
        File folder = new File("hasil");
        folder.mkdirs();
        File huf = new File(folder, a.getName() + ".huf");
        File balik = new File(folder, a.getName() + ".balik");
        System.out.println(FileCompressor.compress(a, huf));
        System.out.println(FileCompressor.decompress(huf, balik));
        boolean sama = Arrays.equals(Files.readAllBytes(a.toPath()),
                                     Files.readAllBytes(balik.toPath()));
        System.out.println(sama ? "ROUND TRIP: IDENTIK" : "ROUND TRIP: BEDA (ada bug!)");
        if (!sama) System.exit(2); // kode 2 = hasil tidak sama
    }
}