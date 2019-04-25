import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.text.ParseException;
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
    private JSON2Java prev;
    private List<Object> result;
    private int state;

    private JSON2Java(JSON2Java prev, List<Object> result, int state) {
        this.prev = prev;
        this.result = result;
        this.state = state;
    }

    public static void main(String[] a) throws ParseException {
        System.err.println(parse(a[0].toCharArray()));
    }

    public static Object parse(ByteBuffer bytes, Charset charset)
            throws ParseException {
        CharBuffer c = charset.decode(bytes);
        return parse(c.array(), 0, c.length());
    }

    public static Object parse(char[] data) throws ParseException {
        return parse(data, 0, data.length);
    }

    public static Object parse(char[] data, int pos, int end)
            throws ParseException {
        List<Object> result = new ArrayList<>(1);
        pos = parse(data, pos, end, result);
        for (; pos >= 0 && pos < end && data[pos] <= ' '; ++pos);
        if (pos != end) {
            pos = Math.abs(pos);
            throw new ParseException("Parse error at " + pos, pos);
        }
        return result.size() == 0 ? null : result.get(0);
    }

    public static Map<String, Object> object(Object json) {
        return json instanceof Map ? (Map<String, Object>) json : Collections.emptyMap();
    }

    public static List<Object> array(Object json) {
        return json instanceof List ? (List<Object>) json : Collections.emptyList();
    }

    static int parse(char[] data, int pos, int end, List<Object> result) {
        JSON2Java stack = new JSON2Java(null, result, 0);
        List<Object> array = null;
        int state = 0;
        for (;;) {
            for (; pos < end && data[pos] <= ' '; ++pos);
            if (pos >= end) {
                return -pos;
            }
            char c = data[pos];
            ++pos;
        pop:
            switch (state) {
            case 0:
                switch (c) {
                case '[':
                    state = ARRAY_FST;
                    array = new ArrayList<>();
                    stack.result.add(array);
                    continue;
                case '{':
                    state = OBJ_FST;
                    array = new ArrayList<>();
                    continue;
                case '"':
                    StringBuilder buf = null;
                    for (int ss = pos; pos < end; ++pos) {
                        switch (data[pos]) {
                        case '"':
                            String s = new String(data, ss, pos - ss);
                            stack.result.add(buf == null ? s : buf.append(s).toString());
                            ++pos;
                            break pop;
                        case '\\':
                            if (buf == null) {
                                buf = new StringBuilder();
                            }
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
                    return -pos;
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
                        stack.result.add(null);
                    } else if (s.equals("false")) {
                        stack.result.add(Boolean.FALSE);
                    } else if (s.equals("true")) {
                        stack.result.add(Boolean.TRUE);
                    } else {
                        try {
                            stack.result.add(new Double(s));
                        } catch (Exception ex) {
                            return -pos;
                        }
                    }
                }
                break;
            case ARRAY_FST:
                if (c == ']') {
                    break;
                }
            case ARRAY:
                stack = new JSON2Java(stack, array, ARRAY_SEP);
                state = 0;
                --pos;
                continue;
            case ARRAY_SEP:
                if (c == ',') {
                    state = ARRAY;
                    continue;
                } else if (c != ']') {
                    return -pos;
                }
                break;
            case OBJ_FST:
                if (c == '}') {
                    stack.result.add(Collections.emptyMap());
                    break;
                }
            case OBJ:
                if (c != '"') {
                    return -pos;
                }
                stack = new JSON2Java(stack, array, PAIR);
                state = 0;
                --pos;
                continue;
            case PAIR:
                if (c != ':') {
                    return -pos;
                }
                stack = new JSON2Java(stack, array, OBJ_SEP);
                state = 0;
                continue;
            case OBJ_SEP:
                if (c == ',') {
                    state = OBJ;
                    continue;
                } else if (c != '}') {
                    return -pos;
                }
                Map<Object, Object> map = new HashMap<>(array.size() / 2);
                for (int i = 0; i < array.size(); i += 2) {
                    map.put(array.get(i), array.get(i + 1));
                }
                stack.result.add(map);
            }
            if (stack.prev == null) {
                return pos;
            }
            array = stack.result;
            state = stack.state;
            stack = stack.prev;
        }
    }
}
