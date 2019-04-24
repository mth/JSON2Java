import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class JSONb {
    private static final int ARRAY_FST = 1;
    private static final int ARRAY     = 2;
    private static final int ARRAY_SEP = 3;
    private static final int OBJ_FST   = 4;
    private static final int OBJ       = 5;
    private static final int PAIR      = 6;
    private static final int OBJ_SEP   = 7;

    public static void main(String[] a) {
        System.err.println(parse(a[0].getBytes(UTF_8)));
    }

    public static Object parse(byte[] data) {
        List<Object> result = new ArrayList<>(1);
        int pos = parse(data, 0, result);
        for (; pos >= 0 && pos < data.length && data[pos] <= ' '; ++pos);
        if (pos != data.length) {
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

    static int parse(byte[] data, int pos, List<Object> result) {
        List<Object> array = null;
        HashMap<Object, Object> map = null;
        int state = 0;
        while (pos >= 0) {
            for (; pos < data.length && data[pos] <= ' '; ++pos);
            int c = pos <= data.length ? data[pos] : 0;
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
                    for (int ss = pos; pos < data.length; ++pos) {
                        switch (data[pos]) {
                        case '"':
                            buf.append(new String(data, ss, pos - ss, UTF_8));
                            result.add(buf.toString());
                            return pos + 1;
                        case '\\':
                            buf.append(new String(data, ss, pos - ss, UTF_8));
                            if (pos < data.length) {
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
                                                   new String(data, pos, 4, UTF_8), 16)));
                                    } catch (Exception ex) {
                                        return -pos;
                                    }
                                    pos += 3;
                                    ss = pos + 1;
                                }
                            }
                        }
                    }
                    break;
                default:
                    int ss = --pos;
                    for (; pos < data.length; ++pos) {
                        c = data[pos];
                        if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'Z' 
                                || c >= 'a' && c <= 'z' || c == '.'
                                || c == '+' || c == '-')) {
                            break;
                        }
                    }
                    String s = new String(data, ss, pos - ss, UTF_8);
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
                pos = parse(data, pos - 1, array);
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
                pos = parse(data, pos - 1, array);
                state = PAIR;
                continue;
            case PAIR:
                if (c != ':') {
                    break;
                }
                pos = parse(data, pos, array);
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
