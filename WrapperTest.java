public class WrapperTest {
    public static interface Thing {
        Boolean[] pieces();
        Thing another();
        Double value();
    }

    public static void main(String[] aa) throws Exception {
        Thing t = Wrapper.wrap(JSON2Java.parse("{\"pieces\": [true, false], \"another\": {\"pieces\": [null]}, \"value\": 3.14 }".toCharArray()), Thing.class);
        System.err.println(t.pieces()[0]);
        System.err.println(t.pieces()[1]);
        System.err.println(t.another().pieces()[0]);
        System.err.println(t.value());
    }
}
