import sun.misc.Unsafe;
import java.lang.reflect.Field;

/**
 * @author: zhangdididi
 * @description: 通过反射创建一个Unsafe实例
 */
public class UnsafeIntance {

    public static Unsafe reflectGetUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);//设置为可访问
            return (Unsafe)field.get(null);

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
