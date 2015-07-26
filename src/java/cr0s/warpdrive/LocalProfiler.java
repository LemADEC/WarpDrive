package cr0s.warpdrive;

import java.util.Stack;

public class LocalProfiler
{
    private static class StackElement
    {
        public long start;
        public long internal;
        public String name;
    }

    private static Stack<StackElement> stack = new Stack<StackElement>();

    public static void start(String name)
    {
        StackElement e = new StackElement();
        e.start = System.nanoTime();
        e.internal = 0;
        e.name = name;
        stack.push(e);
    }

    public static void stop()
    {
        if (stack.isEmpty())
        {
            return;
        }

        StackElement e = stack.pop();
        long end = System.nanoTime();
        long dt = end - e.start;

        if (!stack.isEmpty())
        {
            StackElement e2 = stack.peek();
            e2.internal += dt;
        }

        long self = (dt - e.internal) / 1000; // in microseconds
        long total = dt / 1000;
        WarpDrive.print("[PROF] {" + e.name + "} self: " + (self / 1000F) + "ms, total: " + (total / 1000F) + "ms");
    }
}
