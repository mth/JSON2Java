import java.io.*;
import java.nio.*;
import java.util.Map;
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
            n += ((Map) (usechar ? parseC(bytes)
                                 : JSONb.parse(bytes))).size();
        }
        t = System.currentTimeMillis() - t;
        System.out.println(cycles + " iterations, average "
                            + (t * 1000 / cycles / 1000.0) + "ms");
    }

    private static Object parseC(byte[] bytes) {
        CharBuffer c = UTF_8.decode(ByteBuffer.wrap(bytes));
        return JSONc.parse(c.array(), c.length());
    }
}
