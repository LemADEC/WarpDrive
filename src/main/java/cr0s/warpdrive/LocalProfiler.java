package cr0s.warpdrive;

import cr0s.warpdrive.config.WarpDriveConfig;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class LocalProfiler {
	
	private static final long CALL_STATS_PERIOD_MS = 300000L;
	
	private static final AtomicLong timerStats = new AtomicLong(System.currentTimeMillis() + CALL_STATS_PERIOD_MS);
	private static final Map<String, Integer> stats = new ConcurrentHashMap<>(32);
	
	// Call stats
	public static void updateCallStat(final String usage) {
		if  (!WarpDriveConfig.LOGGING_PROFILING_THREAD_SAFETY) {
			return;
		}
		
		final StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
		final String threadName = Thread.currentThread().getName();
		final StringBuilder stringBuilderKey = new StringBuilder(threadName).append(" ").append(usage);
		if ( !threadName.equals("Server thread")
		  && !threadName.equals("Client thread") ) {
			int depth = 0;
			for (StackTraceElement stackTraceElement : stacktrace) {
				if (depth++ > 2 && depth < 30) {
					stringBuilderKey.append("\n  ").append(stackTraceElement.getClassName());
					stringBuilderKey.append(".").append(stackTraceElement.getMethodName());
					// stringBuilderKey.append("\t").append(stackTraceElement.getFileName());
					stringBuilderKey.append(":").append(stackTraceElement.getLineNumber());
				}
			}
		}
		final String key = stringBuilderKey.toString();
		final Integer value = stats.get(key);
		stats.put(key, value != null ? value + 1 : 1);
		final long now = System.currentTimeMillis();
		if (timerStats.get() <= now) {
			timerStats.set(now + CALL_STATS_PERIOD_MS);
			printCallStats();
		}
	}
	
	private static void printCallStats() {
		WarpDrive.logger.info("Dumping chunk stats:");
		for (Entry<String, Integer> entryStat : stats.entrySet()) {
			WarpDrive.logger.info(String.format("%10d x %s",
			                                    entryStat.getValue(),
			                                    entryStat.getKey()));
		}
	}
	
	// Crude CPU time profiling
	private static class StackElement {
		public long start;
		public long internal;
		public String name;
	}
	
	private static final Stack<StackElement> stack = new Stack<>();
	
	public static void start(final String name) {
		if (WarpDriveConfig.LOGGING_PROFILING_CPU_USAGE) {
			StackElement stackElement = new StackElement();
			stackElement.start = System.nanoTime();
			stackElement.internal = 0;
			stackElement.name = name;
			stack.push(stackElement);
		}
	}
	
	public static void stop() {
		if (stack.isEmpty()) {
			return;
		}
		
		final StackElement stackElement = stack.pop();
		final long end = System.nanoTime();
		final long timeElapsed = end - stackElement.start;

		if (!stack.isEmpty()) {
			StackElement nextStackElement = stack.peek();
			nextStackElement.internal += timeElapsed;
		}
		
		// convert to microseconds
		final long self = (timeElapsed - stackElement.internal) / 1000;
		final long total = timeElapsed / 1000;
		if (total == self) {
			WarpDrive.logger.info(String.format("Profiling %s: %f ms", 
			                                    stackElement.name, (self / 1000.0F) ));
		} else {
			WarpDrive.logger.info(String.format("Profiling %s: %f ms, total; %f ms", 
			                                    stackElement.name, (self / 1000.0F), total / 1000.0F ));
		}
	}
}
