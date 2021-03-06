package ths.commons.util;

import static ths.commons.lang.Assert.*;
import static ths.commons.util.StringEscapeUtils.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ths.commons.internal.Entities;

/**
 * 处理异常的工具类。
 */
public class ExceptionUtils {
    /**
     * 检查异常是否由指定类型的异常引起。
     */
    public static boolean causedBy(Throwable t, Class<? extends Throwable> causeType) {
        assertNotNull(causeType, "causeType");

        Set<Throwable> causes = new HashSet<Throwable>();

        for (; t != null && !causeType.isInstance(t) && !causes.contains(t); t = t.getCause()) {
            causes.add(t);
        }

        return t != null && causeType.isInstance(t);
    }

    /**
     * 取得最根本的异常。
     */
    public static Throwable getRootCause(Throwable t) {
        List<Throwable> causes = getCauses(t, true);

        if (causes.isEmpty()) {
            return null;
        } else {
            return causes.get(0);
        }
    }

    /**
     * 取得包括当前异常在内的所有的causes异常，按出现的顺序排列。
     */
    public static List<Throwable> getCauses(Throwable t) {
        return getCauses(t, false);
    }

    /**
     * 取得包括当前异常在内的所有的causes异常，按出现的顺序排列。
     */
    public static List<Throwable> getCauses(Throwable t, boolean reversed) {
        LinkedList<Throwable> causes = new LinkedList<Throwable>();

        for (; t != null && !causes.contains(t); t = t.getCause()) {
            if (reversed) {
                causes.addFirst(t);
            } else {
                causes.addLast(t);
            }
        }

        return causes;
    }

    /**
     * 将异常转换成<code>RuntimeException</code>。
     */
    public static RuntimeException toRuntimeException(Exception e) {
        return toRuntimeException(e, null);
    }

    /**
     * 将异常转换成<code>RuntimeException</code>。
     */
    public static RuntimeException toRuntimeException(Exception e,
                                                      Class<? extends RuntimeException> runtimeExceptionClass) {
        if (e == null) {
            return null;
        } else if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        } else {
            if (runtimeExceptionClass == null) {
                return new RuntimeException(e);
            } else {
                RuntimeException runtimeException;

                try {
                    runtimeException = runtimeExceptionClass.newInstance();
                } catch (Exception ee) {
                    return new RuntimeException(e);
                }

                runtimeException.initCause(e);
                return runtimeException;
            }
        }
    }

    /**
     * 抛出Throwable，但不需要声明<code>throws Throwable</code>。
     */
    public static void throwExceptionOrError(Throwable t) throws Exception {
        if (t instanceof Exception) {
            throw (Exception) t;
        } else if (t instanceof Error) {
            throw (Error) t;
        } else {
            throw new RuntimeException(t); // unreachable code
        }
    }

    /**
     * 抛出Throwable，但不需要声明<code>throws Throwable</code>。
     */
    public static void throwRuntimeExceptionOrError(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        } else if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else {
            throw new RuntimeException(t);
        }
    }

    /**
     * 取得异常的stacktrace字符串。
     * 
     * @param throwable 异常
     * @return stacktrace字符串
     */
    public static String getStackTrace(Throwable throwable) {
        StringWriter buffer = new StringWriter();
        PrintWriter out = new PrintWriter(buffer);

        throwable.printStackTrace(out);
        out.flush();

        return buffer.toString();
    }

    public static final Entities HTML40_COMMENT;

    static {
        HTML40_COMMENT = new Entities(Entities.HTML40_MODIFIED);
        HTML40_COMMENT.addEntity("#45", 45);
    }

    /**
     * 取得异常的stacktrace字符串，可用于填写在HTML comment中。
     * <ul>
     * <li>先对stacktrace进行HTML escape。</li>
     * <li>然后除去double dash（--）。</li>
     * </ul>
     * 
     * @param throwable 异常
     * @return stacktrace字符串
     */
    public static String getStackTraceForHtmlComment(Throwable throwable) {
        return escapeEntities(HTML40_COMMENT, getStackTrace(throwable));
    }
}
