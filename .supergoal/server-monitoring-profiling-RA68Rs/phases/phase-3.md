SUPERGOAL_PHASE_START
Phase: 3 of 6 — Thread Tracking Integration
Task: Track which TaskInfo runs on which Thread for profiler targeting

## Work Description

Implement the thread tracking system that maps running tasks to their underlying JVM threads:

### 1. TaskThreadTracker class
```java
public class TaskThreadTracker {
    // Primary: ThreadLocal-based tracking (set in TaskLaunch)
    private static final ThreadLocal<TaskInfo> currentTask = new ThreadLocal<>();
    
    // Secondary: Thread ID → TaskInfo mapping (updated periodically)
    private final ConcurrentHashMap<Long, TaskInfo> threadToTask = new ConcurrentHashMap<>();
    
    // Lock for synchronization
    private final Object lock = new Object();
    
    // Called by TaskLaunch when a task starts
    public static void onTaskStart(TaskInfo taskInfo) {
        currentTask.set(taskInfo);
    }
    
    // Called by TaskLaunch when a task completes
    public static void onTaskEnd() {
        currentTask.remove();
    }
    
    // Get the current task on this thread
    public static TaskInfo getCurrentTask() {
        return currentTask.get();
    }
    
    // Register a thread-to-task mapping (called periodically by monitor)
    public void registerThread(long threadId, TaskInfo taskInfo) {
        threadToTask.put(threadId, taskInfo);
    }
    
    // Unregister a thread
    public void unregisterThread(long threadId) {
        threadToTask.remove(threadId);
    }
    
    // Get all thread IDs for a specific task
    public Set<Long> getTaskThreads(String taskId) {
        Set<Long> result = new HashSet<>();
        for (Map.Entry<Long, TaskInfo> entry : threadToTask.entrySet()) {
            if (entry.getValue() != null && entry.getValue().getName().equals(taskId)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
    
    // Get all thread IDs currently tracking tasks
    public Map<Long, TaskInfo> getAllTaskThreads() {
        return new ConcurrentHashMap<>(threadToTask);
    }
    
    // Fallback: find threads by name matching "Task: " prefix
    public Map<Long, TaskInfo> findThreadsByName(List<TaskInfo> tasks) {
        Map<Long, TaskInfo> result = new HashMap<>();
        Thread[] threads = new Thread[Thread.activeCount() * 2];
        Thread.enumerate(threads);
        for (Thread t : threads) {
            if (t == null) continue;
            String name = t.getName();
            if (name.startsWith("Task: ")) {
                String taskName = name.substring(6);
                for (TaskInfo ti : tasks) {
                    if (ti.getName().contains(taskName) || taskName.contains(ti.getName().split(" ")[0])) {
                        result.put(t.getId(), ti);
                        break;
                    }
                }
            }
        }
        return result;
    }
    
    // Refresh the mapping from current thread states
    public void refreshMapping(List<TaskInfo> runningTasks) {
        // Clear and rebuild from Thread.enumerate() + name matching
        synchronized (lock) {
            threadToTask.clear();
            Map<Long, TaskInfo> nameMatches = findThreadsByName(runningTasks);
            threadToTask.putAll(nameMatches);
        }
    }
}
```

### 2. TaskLaunch integration
Modify `ru.biosoft.access.task.TaskPool.TaskLaunch.run()` to call:
```java
// At start of run():
TaskThreadTracker.onTaskStart(taskInfo);  // Need to pass TaskInfo

// At end of run() (in finally block):
TaskThreadTracker.onTaskEnd();
```

Note: TaskLaunch currently only has access to `Task task`, not `TaskInfo`. We need to:
- Add a way to get TaskInfo from Task (either via a transient property on Task, or via the TaskManager's currentTasks map)
- The simplest approach: in TaskLaunch.run(), look up TaskInfo from TaskManager.getInstance().getCurrentTaskForThread() which uses the ThreadLocal

### 3. Integration with TaskManager
Add to TaskManager:
```java
// Get the TaskInfo for the task running on the current thread
public TaskInfo getCurrentTaskForThread() {
    return TaskThreadTracker.getCurrentTask();
}
```

### 4. Periodic refresh
The MonitoringService (Phase 4) will call `TaskThreadTracker.refreshMapping()` every check interval to update the thread-to-task mapping from current thread states.

## Acceptance Criteria
1. TaskThreadTracker class exists with onTaskStart(), onTaskEnd(), getCurrentTask() methods
2. ThreadLocal<TaskInfo> currentTask field present for primary tracking
3. ConcurrentHashMap<Long, TaskInfo> threadToTask field present for secondary tracking
4. getTaskThreads(String taskId) returns correct Set<Long> of thread IDs
5. getAllTaskThreads() returns a copy of the thread-to-task map
6. findThreadsByName() correctly matches "Task: " prefixed thread names to tasks
7. refreshMapping() clears and rebuilds the mapping from Thread.enumerate()
8. TaskManager.getCurrentTaskForThread() delegates to TaskThreadTracker.getCurrentTask()
9. All shared state is thread-safe (ConcurrentHashMap, synchronized blocks)
10. No memory leaks: onTaskEnd() removes ThreadLocal, refreshMapping() cleans stale entries
11. Compilation succeeds without errors

## Evidence Required
- TaskThreadTracker class with all methods visible
- ThreadLocal and ConcurrentHashMap usage visible
- TaskManager.getCurrentTaskForThread() method visible
- refreshMapping() implementation showing Thread.enumerate() usage

## Mandatory commands
mvn compile -pl plugconfig/biouml.plugins.servermonitor -am -DskipTests

## Depends on phases
1

[Agent will print SUPERGOAL_PHASE_VERIFY and SUPERGOAL_PHASE_DONE here during execution]
