import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class JSON2Java {
    private static final int ARRAY_FST = 1;
    private static final int ARRAY     = 2;
    private static final int ARRAY_SEP = 3;
    private static final int OBJ_FST   = 4;
    private static final int OBJ       = 5;
    private static final int PAIR      = 6;
    private static final int OBJ_SEP   = 7;

    public static void main(String[] a) {
        System.err.println(parse(a[0].toCharArray()));
    }

    public static Object parse(ByteBuffer bytes, Charset charset) {
        CharBuffer c = charset.decode(bytes);
        return parse(c.array(), 0, c.length());
    }

    public static Object parse(char[] data) {
        return parse(data, 0, data.length);
    }

    public static Object parse(char[] data, int pos, int end) {
        List<Object> result = new ArrayList<>(1);
        pos = parse(data, pos, end, result);
        for (; pos >= 0 && pos < end && data[pos] <= ' '; ++pos);
        if (pos != end) {
            throw new RuntimeException("parse error at " + Math.abs(pos));
        }
        return result.get(0);
    }

    public static Map<String, Object> object(Object json) {
        return json instanceof Map ? (Map<String, Object>) json : Collections.emptyMap();
    }

    public static List<Object> array(Object json) {
        return json instanceof List ? (List<Object>) json : Collections.emptyList();
    }

    static int parse(char[] data, int pos, int end, List<Object> result) {
        List<Object> array = null;
        HashMap<Object, Object> map = null;
        int state = 0;
        while (pos >= 0) {
            for (; pos < end && data[pos] <= ' '; ++pos);
            char c = pos < end ? data[pos] : 0;
            ++pos;
            switch (state) { // break means error
            case 0:
                switch (c) {
                case '[':
                    state = ARRAY_FST;
                    array = new ArrayList<>();
                    result.add(array);
                    continue;
                case '{':
                    state = OBJ_FST;
                    map = new HashMap<>();
                    array = new ArrayList<>(2);
                    result.add(map);
                    continue;
                case '"':
                    StringBuilder buf = new StringBuilder();
                    for (int ss = pos; pos < end; ++pos) {
                        switch (data[pos]) {
                        case '"':
                            buf.append(new String(data, ss, pos - ss));
                            result.add(buf.toString());
                            return pos + 1;
                        case '\\':
                            buf.append(new String(data, ss, pos - ss));
                            if (++pos < end) {
                                ss = pos + 1;
                                switch (data[pos]) {
                                case '/':
                                case '\\':
                                case '"':
                                    ss = pos;
                                    continue;
                                case 'b':
                                    buf.append('\b');
                                    continue;
                                case 'f':
                                    buf.append('\f');
                                    continue;
                                case 'n':
                                    buf.append('\n');
                                    continue;
                                case 'r':
                                    buf.append('\r');
                                    continue;
                                case 't':
                                    buf.append('\t');
                                    continue;
                                case 'u':
                                    try {
                                        buf.append(Character.toChars(Integer.parseInt(
                                                        new String(data, pos + 1, 4), 16)));
                                    } catch (Exception ex) {
                                        return -pos;
                                    }
                                    pos += 4;
                                    ss = pos + 1;
                                    continue;
                                }
                            }
                            return -pos;
                        }
                    }
                    break;
                default:
                    int ss = --pos;
                    for (; pos < end; ++pos) {
                        c = data[pos];
                        if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'Z' 
                                || c >= 'a' && c <= 'z' || c == '.'
                                || c == '+' || c == '-')) {
                            break;
                        }
                    }
                    String s = new String(data, ss, pos - ss);
                    if (s.equals("null")) {
                        result.add(null);
                    } else if (s.equals("false")) {
                        result.add(Boolean.FALSE);
                    } else if (s.equals("true")) {
                        result.add(Boolean.TRUE);
                    } else {
                        try {
                            result.add(new Double(s));
                        } catch (Exception ex) {
                            break;
                        }
                    }
                    return pos;
                }
                break;
            case ARRAY_FST:
                if (c == ']') {
                    return pos;
                }
            case ARRAY:
                pos = parse(data, pos - 1, end, array);
                state = ARRAY_SEP;
                continue;
            case ARRAY_SEP:
                switch (c) {
                case ',':
                    state = ARRAY;
                    continue;
                case ']':
                    return pos;
                }
                break;
            case OBJ_FST:
                if (c == '}') {
                    return pos;
                }
            case OBJ:
                if (c != '"') {
                    break;
                }
                pos = parse(data, pos - 1, end, array);
                state = PAIR;
                continue;
            case PAIR:
                if (c != ':') {
                    break;
                }
                pos = parse(data, pos, end, array);
                if (pos >= 0) {
                    map.put(array.get(0), array.get(1));
                    array.clear();
                    state = OBJ_SEP;
                }
                continue;
            case OBJ_SEP:
                switch (c) {
                case ',':
                    state = OBJ;
                    continue;
                case '}':
                    return pos;
                }
                break;
            }
            return -pos; // error
        }
        return pos;
    }
}
