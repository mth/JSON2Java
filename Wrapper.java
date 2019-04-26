import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class Wrapper implements InvocationHandler {
    private Map<?, ?> object;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        return object.get(method.getName());
    }

    public static <T> T proxy(Object json, Class<T> type) {
        if (json == null) {
            return null;
        }
        if (type.isInterface()) {
            Wrapper wrapper = new Wrapper();
            wrapper.object = (Map<?, ?>) json;
            return (T) Proxy.newProxyInstance(type.getClassLoader(),
                                              new Class<?>[] { type }, wrapper);
        }
        Class<?> ct = type.getComponentType();
        if (ct != null) {
            List<?> array = (List<?>) json;
            Object[] result = (Object[]) Array.newInstance(ct, array.size());
            for (int i = 0; i < result.length; ++i) {
                result[i] = proxy(array.get(i), ct);
            }
            return (T) (Object) result;
        }
        return (T) json;
    }
}
