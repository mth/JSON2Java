import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class Test {
    public static void main(String[] a) throws Exception {
        ByteArrayOutputStream bytebuf = new ByteArrayOutputStream();
        int cycles = Integer.parseInt(a[0]);
        try (InputStream f = new FileInputStream(a[1])) {
            byte[] tmp = new byte[65536];
            for (;;) {
                int n = f.read(tmp);
                if (n < 0)
                    break;
                bytebuf.write(tmp, 0, n);
            }
        }
        byte[] bytes = bytebuf.toByteArray();
        bytebuf = null;
        int n = 0;
        long t = System.currentTimeMillis();
        for (int i = 0; i < cycles; ++i) {
            Object x = JSON2Java.parse(ByteBuffer.wrap(bytes), UTF_8);
            if (x instanceof Map) {
                n += ((Map) x).size();
            } else if (x instanceof Collection) {
                n += ((Collection) x).size();
            }
        }
        t = System.currentTimeMillis() - t;
        System.out.println(cycles + " iterations in " + (t / 10 / 100.0) + "s; "
            + (cycles * 100000 / t / 100.0) + " iter/s; "
            + ((long) cycles * bytes.length * 10000 / t / 1024 / 1024 / 10.0) + " MB/s");
    }
}
