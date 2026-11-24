package huffman;

import java.io.File;
import huffman.service.HufInspector;

public class Inspect {
    public static void main(String[] args) throws Exception {
        int maxByte = args.length > 1 ? Integer.parseInt(args[1]) : 64;
        System.out.println(HufInspector.inspect(new File(args[0]), maxByte));
    }
}