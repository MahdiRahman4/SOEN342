package taskmanager.ical;

import taskmanager.domain.Task;

import java.io.IOException;
import java.util.List;

/**
 * Gateway interface that decouples the domain layer from the iCal4j library.
 *
 * The domain calls this interface; only the implementation knows about iCal4j.
 * This fulfills the Gateway pattern required by Iteration III.
 *
 * Rules enforced by the implementation (per specification):
 *   - Tasks without a due date are silently skipped.
 *   - Subtasks are summarised inside the parent VEVENT DESCRIPTION;
 *     they are NOT exported as separate calendar entries.
 */
public interface ICalGateway {

    /**
     * Exports the given tasks to an iCalendar (.ics) file at {@code filePath}.
     *
     * @param tasks    candidate tasks; those without a due date are skipped
     * @param filePath destination file path (e.g. "tasks.ics")
     * @throws IOException if the file cannot be written
     */
    void exportToIcs(List<Task> tasks, String filePath) throws IOException;
}
