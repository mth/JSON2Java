import java.io.*;
import java.nio.*;
import java.util.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class Test {
    public static void main(String[] a) throws Exception {
        ByteArrayOutputStream bytebuf = new ByteArrayOutputStream();
        boolean usechar = "c".equals(a[0]);
        int cycles = Integer.parseInt(a[1]);
        try (InputStream f = new FileInputStream(a[2])) {
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
            Object x = usechar ? parseC(bytes) : JSONb.parse(bytes);
            if (x instanceof Map) {
                n += ((Map) x).size();
            } else if (x instanceof Collection) {
                n += ((Collection) x).size();
            }
        }
        t = System.currentTimeMillis() - t;
        System.out.println(cycles + " iterations, average "
                            + (t * 10000 / cycles / 10000.0) + "ms");
    }

    private static Object parseC(byte[] bytes) {
        CharBuffer c = UTF_8.decode(ByteBuffer.wrap(bytes));
        return JSONc.parse(c.array(), c.length());
    }
}
