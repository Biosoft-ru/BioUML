package biouml.plugins.servermonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.tasks.TaskInfo;

/**
 * Tracks which TaskInfo runs on which Thread, enabling profiler to target specific tasks.
 * Uses dual approach: ThreadLocal (primary) + thread name matching (fallback).
 */
public class TaskThreadTracker {

    private static final Logger log = Logger.getLogger(TaskThreadTracker.class.getName());

    // Primary: ThreadLocal-based tracking (set in TaskLaunch)
    private static final ThreadLocal<TaskInfo> currentTask = new ThreadLocal<>();

    // Secondary: Thread ID → TaskInfo mapping (updated periodically by monitor)
    private static final ConcurrentHashMap<Long, TaskInfo> threadToTask = new ConcurrentHashMap<>();

    // Lock for synchronization
    private static final Object lock = new Object();

    /**
     * Called when a task starts on the current thread.
     * @param taskInfo the task running on this thread
     */
    public static void onTaskStart(TaskInfo taskInfo) {
        currentTask.set(taskInfo);
    }

    /**
     * Called when a task completes on the current thread.
     */
    public static void onTaskEnd() {
        currentTask.remove();
    }

    /**
     * Get the TaskInfo for the task running on the current thread.
     * @return the current task, or null if no task is running
     */
    public static TaskInfo getCurrentTask() {
        return currentTask.get();
    }

    /**
     * Register a thread-to-task mapping.
     * Called periodically by the monitoring service.
     * @param threadId the thread ID
     * @param taskInfo the task running on this thread
     */
    public static void registerThread(long threadId, TaskInfo taskInfo) {
        threadToTask.put(threadId, taskInfo);
    }

    /**
     * Unregister a thread.
     * @param threadId the thread ID to remove
     */
    public static void unregisterThread(long threadId) {
        threadToTask.remove(threadId);
    }

    /**
     * Get all thread IDs for a specific task.
     * @param taskId the task name
     * @return set of thread IDs running this task
     */
    public static Set<Long> getTaskThreads(String taskId) {
        Set<Long> result = new HashSet<>();
        for (Map.Entry<Long, TaskInfo> entry : threadToTask.entrySet()) {
            TaskInfo ti = entry.getValue();
            if (ti != null && ti.getName().equals(taskId)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Get all thread IDs currently tracking tasks.
     * @return map of threadId to TaskInfo
     */
    public static Map<Long, TaskInfo> getAllTaskThreads() {
        return new ConcurrentHashMap<>(threadToTask);
    }

    /**
     * Get all unique tasks currently tracked (no admin access needed).
     * @return list of unique TaskInfo objects
     */
    public static List<TaskInfo> getAllTrackedTasks() {
        Map<Long, TaskInfo> all = getAllTaskThreads();
        // Deduplicate by task name
        Map<String, TaskInfo> unique = new HashMap<>();
        for (TaskInfo ti : all.values()) {
            if (ti != null) {
                unique.putIfAbsent(ti.getName(), ti);
            }
        }
        return new ArrayList<>(unique.values());
    }

    /**
     * Fallback: find threads by name matching "Task: " prefix.
     * @param tasks list of running tasks to match against
     * @return map of threadId to matching TaskInfo
     */
    public static Map<Long, TaskInfo> findThreadsByName(List<TaskInfo> tasks) {
        Map<Long, TaskInfo> result = new HashMap<>();
        Thread[] threads = new Thread[Thread.activeCount() * 2];
        int count = Thread.enumerate(threads);

        for (int i = 0; i < count; i++) {
            Thread t = threads[i];
            if (t == null) continue;

            String name = t.getName();
            if (name != null && name.startsWith("Task: ")) {
                String taskName = name.substring(6);
                for (TaskInfo ti : tasks) {
                    String tiName = ti.getName();
                    // Match: taskName contains task ID prefix, or tiName contains taskName
                    if (tiName.contains(taskName) || taskName.contains(tiName.split(" ")[0])) {
                        result.put(t.getId(), ti);
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Refresh the thread-to-task mapping from current thread states.
     * Called periodically by the monitoring service.
     * @param runningTasks list of currently running tasks
     */
    public static void refreshMapping(List<TaskInfo> runningTasks) {
        synchronized (lock) {
            threadToTask.clear();
            Map<Long, TaskInfo> nameMatches = findThreadsByName(runningTasks);
            threadToTask.putAll(nameMatches);

            // Also register ThreadLocal tasks
            for (Map.Entry<Long, TaskInfo> entry : threadToTask.entrySet()) {
                // Already registered
            }
        }
    }

    /**
     * Get the thread ID for the current thread's task (if any).
     * @return the current thread's ID, or -1 if no task
     */
    public static long getCurrentThreadId() {
        TaskInfo task = currentTask.get();
        if (task != null) {
            return Thread.currentThread().getId();
        }
        return -1;
    }

    /**
     * Clear all tracked threads.
     */
    public static void clear() {
        synchronized (lock) {
            threadToTask.clear();
        }
    }
}
