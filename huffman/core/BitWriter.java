package huffman.core;
import java.io.IOException;
import java.io.OutputStream;

public class BitWriter {
    private final OutputStream out;
    private int curr = 0;   // byte yg sedang di susun
    private int n = 0;      // jumlah bit yg sudah masuk ke curr

    public BitWriter(OutputStream out) { this.out = out; }

    // code berisis 0 dan 1
    public void write (String code) throws IOException {
        for (int i = 0; i < code.length(); i++) {
            curr = (curr << 1) | (code.charAt(i) - '0');
            n++;
            if(n == 8)  {
                out.write(curr);
                curr = 0;
                n = 0;
            }
        }
    }

    // handle bit yg belum 8 bit dengan menambahkan padding 0
    public void flush () throws IOException {
        if (n > 0) {
            out.write(curr << (8 - n));
            curr = 0;
            n = 0;
        }
        out.flush();
    }
}