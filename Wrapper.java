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
        return wrap(object.get(method.getName()), method.getReturnType());
    }

    public static <T> T wrap(Object json, Class<T> type) {
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
            T result = (T) Array.newInstance(ct, array.size());
            for (int i = 0; i < array.size(); ++i) {
                Object v = wrap(array.get(i), ct);
                if (v != null) {
                    Array.set(result, i, v);
                }
            }
            return result;
        }
        return (T) json;
    }
}
