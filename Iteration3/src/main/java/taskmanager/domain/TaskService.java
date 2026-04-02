package taskmanager.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Domain service enforcing the four OCL business rules and providing
 * task-filtering utilities for iCal export.
 */
public class TaskService {

    private static final int MAX_SUBTASKS_PER_TASK = 20;
    private static final int MAX_OPEN_NO_DUE_DATE  = 50;

    /**
     * OCL: context Task inv MaxSubtasks:
     *   self.subtasks->size() <= 20
     *
     * Call before persisting a new subtask.
     */
    public void validateAddSubtask(Task parent) {
        if (parent.getSubtasks().size() >= MAX_SUBTASKS_PER_TASK) {
            throw new IllegalStateException(
                "OCL violation [MaxSubtasks]: task \"" + parent.getTitle() +
                "\" already has " + parent.getSubtasks().size() +
                " sub-tasks (limit " + MAX_SUBTASKS_PER_TASK + ").");
        }
    }

    /**
     * OCL: context Task inv OpenNoDueDateLimit:
     *   Task.allInstances()
     *     ->select(t | t.status = Status::OPEN and t.dueDate.oclIsUndefined())
     *     ->size() <= 50
     *
     * Call before persisting a new task that has no due date.
     */
    public void validateNewTaskWithoutDueDate(List<Task> allTasks, Task newTask) {
        if (newTask.hasDueDate()) return;
        long count = allTasks.stream()
            .filter(t -> t.getStatus() == Status.OPEN && !t.hasDueDate())
            .count();
        if (count >= MAX_OPEN_NO_DUE_DATE) {
            throw new IllegalStateException(
                "OCL violation [OpenNoDueDateLimit]: the system already has " + count +
                " open tasks without a due date (limit " + MAX_OPEN_NO_DUE_DATE + ").");
        }
    }

    /**
     * OCL: context Collaborator inv NoOverload:
     *   Task.allInstances()
     *     ->select(t | t.collaborator = self and t.status = Status::OPEN)
     *     ->size() <= self.category.openTaskLimit
     *
     * Call before assigning a collaborator to a task. {@code collabTasks} must
     * contain all tasks currently assigned to {@code collaborator}.
     */
    public void validateAssignCollaborator(Collaborator collaborator, List<Task> collabTasks) {
        long openCount = collabTasks.stream()
            .filter(t -> t.getStatus() == Status.OPEN)
            .count();
        if (openCount >= collaborator.getCategory().getOpenTaskLimit()) {
            throw new IllegalStateException(
                "OCL violation [NoOverload]: collaborator \"" + collaborator.getName() +
                "\" (" + collaborator.getCategory() + ") is at the open-task limit (" +
                collaborator.getCategory().getOpenTaskLimit() + "). They must complete " +
                "at least one task before being assigned a new one.");
        }
    }

    /**
     * Returns only tasks that are eligible for iCal export, i.e. those
     * that have a due date. Tasks without a due date are silently excluded
     * per the assignment specification.
     */
    public List<Task> filterExportable(List<Task> tasks) {
        return tasks.stream()
            .filter(Task::hasDueDate)
            .collect(Collectors.toList());
    }

    /**
     * Returns open tasks whose due date falls within [fromDate, toDate] inclusive.
     * Both dates must be in yyyy-MM-dd format.
     */
    public List<Task> filterOpenDueInRange(List<Task> tasks, String fromDate, String toDate) {
        LocalDate from = LocalDate.parse(fromDate);
        LocalDate to   = LocalDate.parse(toDate);
        return tasks.stream()
            .filter(t -> t.getStatus() == Status.OPEN && t.hasDueDate())
            .filter(t -> {
                LocalDate due = LocalDate.parse(t.getDueDate());
                return !due.isBefore(from) && !due.isAfter(to);
            })
            .collect(Collectors.toList());
    }
}
