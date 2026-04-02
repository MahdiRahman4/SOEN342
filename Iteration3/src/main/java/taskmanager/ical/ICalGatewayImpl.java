package taskmanager.ical;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.util.RandomUidGenerator;
import taskmanager.domain.Subtask;
import taskmanager.domain.Task;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * iCal4j-backed implementation of the ICalGateway.
 *
 * This class is the only place in the codebase that imports iCal4j types,
 * keeping the rest of the domain and persistence layers library-agnostic.
 *
 * Each eligible task (one with a due date) becomes a VEVENT:
 *   SUMMARY    = task title
 *   DESCRIPTION = task description + project name + status + priority
 *                 + subtask summary (not separate entries per spec)
 *   DTSTART    = due date (all-day DATE value)
 *   PRIORITY   = mapped from LOW/MEDIUM/HIGH to iCalendar scale 9/5/1
 *   STATUS     = CONFIRMED (open/completed) or CANCELLED
 */
public class ICalGatewayImpl implements ICalGateway {

    @Override
    public void exportToIcs(List<Task> tasks, String filePath) throws IOException {
        // Suppress ical4j timezone registry noise in CLI output
        System.setProperty("net.fortuna.ical4j.timezone.cache.impl",
                           "net.fortuna.ical4j.util.MapTimeZoneCache");

        Calendar calendar = new Calendar();
        calendar.getProperties().add(new ProdId("-//TaskManager//SOEN342//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);

        RandomUidGenerator uidGen = new RandomUidGenerator();

        for (Task task : tasks) {
            if (!task.hasDueDate()) continue;   // guard: silently skip per spec

            try {
                // Convert "yyyy-MM-dd" to ical4j's "yyyyMMdd" DATE string
                String icalDateStr = task.getDueDate().replace("-", "");
                Date dueDate = new Date(icalDateStr);

                VEvent event = new VEvent(dueDate, task.getTitle());
                event.getProperties().add(uidGen.generateUid());
                event.getProperties().add(new Description(buildDescription(task)));

                // iCalendar PRIORITY: 1 (highest) – 9 (lowest)
                int icalPriority = mapPriority(task);
                event.getProperties().add(new net.fortuna.ical4j.model.property.Priority(icalPriority));

                // iCalendar STATUS: CONFIRMED or CANCELLED
                net.fortuna.ical4j.model.property.Status icalStatus = mapStatus(task);
                event.getProperties().add(icalStatus);

                calendar.getComponents().add(event);

            } catch (ParseException e) {
                System.err.println("[ical] Skipping task \"" + task.getTitle() +
                    "\": unparseable due date \"" + task.getDueDate() + "\"");
            }
        }

        // validating=false: skips ical4j's internal validation so the method
        // only throws IOException, matching the ICalGateway interface contract.
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            new CalendarOutputter(false).output(calendar, fos);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Builds the DESCRIPTION field for a VEVENT.
     * Includes task description, project, status, priority, and a subtask
     * summary — subtasks are NOT exported as separate calendar entries.
     */
    private String buildDescription(Task task) {
        StringBuilder sb = new StringBuilder();

        if (!task.getDescription().isEmpty()) {
            sb.append(task.getDescription()).append("\n");
        }
        if (task.getProject() != null) {
            sb.append("Project: ").append(task.getProject().getName()).append("\n");
        }
        sb.append("Status: ").append(task.getStatus()).append("\n");
        sb.append("Priority: ").append(task.getPriority()).append("\n");

        List<Subtask> subtasks = task.getSubtasks();
        if (!subtasks.isEmpty()) {
            long doneCount = subtasks.stream()
                .filter(s -> s.getStatus() == taskmanager.domain.Status.COMPLETED)
                .count();
            sb.append("Subtasks: ").append(doneCount).append("/")
              .append(subtasks.size()).append(" completed\n");
            for (Subtask st : subtasks) {
                sb.append("  - [").append(st.getStatus()).append("] ")
                  .append(st.getTitle()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private int mapPriority(Task task) {
        taskmanager.domain.Priority p = task.getPriority();
        if (p == taskmanager.domain.Priority.HIGH)   return 1;
        if (p == taskmanager.domain.Priority.LOW)    return 9;
        return 5; // MEDIUM
    }

    private net.fortuna.ical4j.model.property.Status mapStatus(Task task) {
        if (task.getStatus() == taskmanager.domain.Status.CANCELLED) {
            return net.fortuna.ical4j.model.property.Status.VEVENT_CANCELLED;
        }
        return net.fortuna.ical4j.model.property.Status.VEVENT_CONFIRMED;
    }
}
