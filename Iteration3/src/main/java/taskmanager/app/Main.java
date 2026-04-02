package taskmanager.app;

import taskmanager.domain.*;
import taskmanager.ical.ICalGateway;
import taskmanager.ical.ICalGatewayImpl;
import taskmanager.persistence.*;

import java.io.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CLI entry point for the Task Management System.
 *
 * Presentation layer — all user I/O is here; domain logic stays in domain
 * services and domain entities.
 */
public class Main {

    private static final Scanner sc = new Scanner(System.in);

    private static TaskRepository        taskRepo;
    private static ProjectRepository     projectRepo;
    private static CollaboratorRepository collabRepo;
    private static SubtaskRepository     subtaskRepo;
    private static TaskService           taskService;
    private static CollaboratorService   collabService;
    private static ICalGateway           icalGateway;

    public static void main(String[] args) throws Exception {
        projectRepo   = new ProjectRepository();
        collabRepo    = new CollaboratorRepository();
        subtaskRepo   = new SubtaskRepository();
        taskRepo      = new TaskRepository(projectRepo, collabRepo, subtaskRepo);
        taskService   = new TaskService();
        collabService = new CollaboratorService();
        icalGateway   = new ICalGatewayImpl();

        System.out.println("Task Management System — SOEN342");
        System.out.println("=================================");

        boolean running = true;
        while (running) {
            printMenu();
            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1"  -> doSearch();
                    case "2"  -> doAddTask();
                    case "3"  -> doChangeTaskStatus();
                    case "4"  -> doAddSubtask();
                    case "5"  -> doManageProjects();
                    case "6"  -> doManageCollaborators();
                    case "7"  -> doExportIcal();
                    case "8"  -> doListOverloaded();
                    case "9"  -> doImportCSV();
                    case "10" -> doExportCSV();
                    case "0"  -> running = false;
                    default   -> System.out.println("Unknown option.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        System.out.println("Goodbye.");
        Database.close();
    }

    // -------------------------------------------------------------------------
    // Menu
    // -------------------------------------------------------------------------

    private static void printMenu() {
        System.out.println();
        System.out.println(" 1) Search / view tasks");
        System.out.println(" 2) Add task");
        System.out.println(" 3) Complete / Cancel / Reopen task");
        System.out.println(" 4) Add subtask");
        System.out.println(" 5) Manage projects");
        System.out.println(" 6) Manage collaborators");
        System.out.println(" 7) Export to iCalendar (.ics)");
        System.out.println(" 8) List overloaded collaborators");
        System.out.println(" 9) Import from CSV");
        System.out.println("10) Export to CSV");
        System.out.println(" 0) Quit");
        System.out.print("> ");
    }

    // -------------------------------------------------------------------------
    // 1 — Search / view tasks
    // -------------------------------------------------------------------------

    private static void doSearch() throws SQLException {
        System.out.print("Keyword (blank = all open tasks): ");
        String kw = sc.nextLine().trim();
        List<Task> results;
        if (kw.isEmpty()) {
            results = taskRepo.findAll().stream()
                .filter(t -> t.getStatus() == Status.OPEN)
                .collect(Collectors.toList());
        } else {
            results = taskRepo.search(kw);
        }

        if (results.isEmpty()) {
            System.out.println("No tasks found.");
            return;
        }
        for (int i = 0; i < results.size(); i++) {
            System.out.printf("  %3d. %s%n", i + 1, results.get(i));
            results.get(i).getSubtasks()
                .forEach(s -> System.out.println("        -> " + s));
        }
    }

    // -------------------------------------------------------------------------
    // 2 — Add task
    // -------------------------------------------------------------------------

    private static void doAddTask() throws SQLException {
        System.out.print("Title: ");
        String title = sc.nextLine().trim();
        if (title.isEmpty()) { System.out.println("Title cannot be empty."); return; }

        System.out.print("Description (optional): ");
        String desc = sc.nextLine().trim();

        System.out.print("Priority [low/medium/high, default=medium]: ");
        Priority priority = Priority.fromString(sc.nextLine().trim());

        System.out.print("Due date yyyy-MM-dd (optional, blank = none): ");
        String dueDate = sc.nextLine().trim();

        System.out.print("Recurrence [none/daily/weekly/monthly, default=none]: ");
        Recurrence recurrence = Recurrence.fromString(sc.nextLine().trim());

        Project project = promptProject();

        Task task = new Task(-1, title, desc, LocalDate.now().toString(),
                             dueDate, priority, Status.OPEN, recurrence, project, null);

        // OCL guard: open tasks without due date <= 50
        List<Task> allTasks = taskRepo.findAll();
        try {
            taskService.validateNewTaskWithoutDueDate(allTasks, task);
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
            return;
        }

        taskRepo.save(task);
        System.out.println("Added: " + task);
    }

    // -------------------------------------------------------------------------
    // 3 — Complete / Cancel / Reopen task
    // -------------------------------------------------------------------------

    private static void doChangeTaskStatus() throws SQLException {
        List<Task> all = taskRepo.findAll();
        if (all.isEmpty()) { System.out.println("No tasks."); return; }

        printNumberedList(all);
        System.out.print("Select task: ");
        int idx = readIndex(all.size());
        if (idx < 0) return;

        Task t = all.get(idx);

        if (t.getStatus() == Status.OPEN) {
            System.out.print("Action [complete/cancel]: ");
            String action = sc.nextLine().trim().toLowerCase();
            if (action.startsWith("comp")) {
                t.setStatus(Status.COMPLETED);
                taskRepo.save(t);
                System.out.println("Completed: " + t.getTitle());
            } else if (action.startsWith("canc")) {
                t.setStatus(Status.CANCELLED);
                taskRepo.save(t);
                System.out.println("Cancelled: " + t.getTitle());
            } else {
                System.out.println("Unknown action. Use 'complete' or 'cancel'.");
            }
        } else {
            // Task is COMPLETED or CANCELLED — offer reopen
            System.out.print("Task is " + t.getStatus() + ". Reopen it? [y/n]: ");
            if (sc.nextLine().trim().equalsIgnoreCase("y")) {
                t.setStatus(Status.OPEN);
                taskRepo.save(t);
                System.out.println("Reopened: " + t.getTitle());
            }
        }
    }

    // -------------------------------------------------------------------------
    // 4 — Add subtask
    // -------------------------------------------------------------------------

    private static void doAddSubtask() throws SQLException {
        List<Task> tasks = taskRepo.findAll().stream()
            .filter(t -> t.getStatus() == Status.OPEN)
            .collect(Collectors.toList());
        if (tasks.isEmpty()) { System.out.println("No open tasks."); return; }

        for (int i = 0; i < tasks.size(); i++) {
            System.out.printf("  %3d. %s  (subtasks: %d)%n",
                i + 1, tasks.get(i), tasks.get(i).getSubtasks().size());
        }
        System.out.print("Select parent task: ");
        int idx = readIndex(tasks.size());
        if (idx < 0) return;

        Task parent = tasks.get(idx);
        // OCL guard: subtasks <= 20
        try {
            taskService.validateAddSubtask(parent);
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
            return;
        }

        System.out.print("Subtask title: ");
        String title = sc.nextLine().trim();
        if (title.isEmpty()) { System.out.println("Title cannot be empty."); return; }

        subtaskRepo.save(new Subtask(title, parent.getId()));
        System.out.println("Subtask added to \"" + parent.getTitle() + "\".");
    }

    // -------------------------------------------------------------------------
    // 5 — Manage projects
    // -------------------------------------------------------------------------

    private static void doManageProjects() throws SQLException {
        System.out.println("  a) Add project   l) List projects");
        System.out.print("> ");
        String choice = sc.nextLine().trim().toLowerCase();

        if (choice.equals("a")) {
            System.out.print("Name: ");
            String name = sc.nextLine().trim();
            if (name.isEmpty()) { System.out.println("Name cannot be empty."); return; }
            if (projectRepo.findByName(name).isPresent()) {
                System.out.println("Project \"" + name + "\" already exists.");
                return;
            }
            System.out.print("Description (optional): ");
            String desc = sc.nextLine().trim();
            Project p = projectRepo.save(new Project(name, desc));
            System.out.println("Created: " + p.getName());

        } else if (choice.equals("l")) {
            List<Project> projects = projectRepo.findAll();
            if (projects.isEmpty()) { System.out.println("No projects."); return; }
            projects.forEach(p -> System.out.println(
                "  - " + p.getName() +
                (p.getDescription().isEmpty() ? "" : ": " + p.getDescription())));
        }
    }

    // -------------------------------------------------------------------------
    // 6 — Manage collaborators
    // -------------------------------------------------------------------------

    private static void doManageCollaborators() throws SQLException {
        System.out.println("  a) Add collaborator   s) Assign to task   l) List");
        System.out.print("> ");
        String choice = sc.nextLine().trim().toLowerCase();

        switch (choice) {
            case "a" -> {
                // Collaborators must be defined under a project (spec: "defined under a project")
                List<Project> projects = projectRepo.findAll();
                if (projects.isEmpty()) {
                    System.out.println("No projects exist. Create a project first.");
                    return;
                }
                System.out.println("Select the project this collaborator belongs to:");
                for (int i = 0; i < projects.size(); i++)
                    System.out.printf("  %3d. %s%n", i + 1, projects.get(i));
                System.out.print("> ");
                int pIdx = readIndex(projects.size());
                if (pIdx < 0) return;
                Project project = projects.get(pIdx);

                System.out.print("Name: ");
                String name = sc.nextLine().trim();
                if (name.isEmpty()) { System.out.println("Name cannot be empty."); return; }
                System.out.print("Category [junior/intermediate/senior]: ");
                String catStr = sc.nextLine().trim().toUpperCase();
                CollaboratorCategory cat;
                try { cat = CollaboratorCategory.valueOf(catStr); }
                catch (IllegalArgumentException e) {
                    System.out.println("Unknown category. Use junior, intermediate, or senior.");
                    return;
                }
                Collaborator c = collabRepo.save(new Collaborator(name, cat, project.getId()));
                System.out.println("Added " + c + " to project \"" + project.getName() + "\".");
            }
            case "s" -> doAssignCollaborator();
            case "l" -> {
                // List collaborators grouped by project
                List<Project> projects = projectRepo.findAll();
                if (projects.isEmpty()) { System.out.println("No collaborators."); return; }
                boolean any = false;
                for (Project p : projects) {
                    List<Collaborator> pc = collabRepo.findByProjectId(p.getId());
                    if (!pc.isEmpty()) {
                        System.out.println("  [" + p.getName() + "]");
                        pc.forEach(c -> System.out.println("    - " + c));
                        any = true;
                    }
                }
                if (!any) System.out.println("No collaborators.");
            }
        }
    }

    private static void doAssignCollaborator() throws SQLException {
        // Only project tasks may have collaborators (spec: "A project task may have collaborators")
        List<Task> tasks = taskRepo.findAll().stream()
            .filter(t -> t.getStatus() == Status.OPEN && t.getProject() != null)
            .collect(Collectors.toList());
        if (tasks.isEmpty()) {
            System.out.println("No open project tasks. Assign the task to a project first.");
            return;
        }

        printNumberedList(tasks);
        System.out.print("Select task: ");
        int tIdx = readIndex(tasks.size());
        if (tIdx < 0) return;

        Task task = tasks.get(tIdx);

        // Only show collaborators belonging to the task's project
        List<Collaborator> collabs = collabRepo.findByProjectId(task.getProject().getId());
        if (collabs.isEmpty()) {
            System.out.println("No collaborators in project \"" + task.getProject().getName() +
                               "\". Add one via option 6 > a first.");
            return;
        }

        for (int i = 0; i < collabs.size(); i++)
            System.out.printf("  %3d. %s%n", i + 1, collabs.get(i));
        System.out.print("Select collaborator: ");
        int cIdx = readIndex(collabs.size());
        if (cIdx < 0) return;

        Collaborator collab = collabs.get(cIdx);

        // OCL guard: no overload
        List<Task> collabTasks = taskRepo.findByCollaborator(collab.getId());
        try {
            taskService.validateAssignCollaborator(collab, collabTasks);
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
            return;
        }

        // OCL guard: subtask limit (auto-subtask counts toward the 20-subtask cap)
        // Re-load task with its current subtasks for an accurate count
        task = taskRepo.findById(task.getId()).orElse(task);
        try {
            taskService.validateAddSubtask(task);
        } catch (IllegalStateException e) {
            System.out.println("Cannot assign: " + e.getMessage());
            return;
        }

        task.setCollaborator(collab);
        taskRepo.save(task);

        // Automatically create a subtask linking this task to the collaborator (spec requirement)
        Subtask linked = new Subtask("Assigned to " + collab.getName(), task.getId());
        subtaskRepo.save(linked);

        System.out.println("Assigned " + collab.getName() + " to \"" + task.getTitle() +
                           "\" and created subtask \"" + linked.getTitle() + "\".");
    }

    // -------------------------------------------------------------------------
    // 7 — Export to iCalendar
    // -------------------------------------------------------------------------

    private static void doExportIcal() throws Exception {
        System.out.println("Export scope:");
        System.out.println("  1) Single task");
        System.out.println("  2) All tasks in a project");
        System.out.println("  3) Filtered — open tasks due within a date range");
        System.out.print("> ");
        String scope = sc.nextLine().trim();

        List<Task> candidates;
        switch (scope) {
            case "1" -> {
                List<Task> all = taskRepo.findAll();
                if (all.isEmpty()) { System.out.println("No tasks."); return; }
                printNumberedList(all);
                System.out.print("Select task: ");
                int idx = readIndex(all.size());
                if (idx < 0) return;
                candidates = List.of(all.get(idx));
            }
            case "2" -> {
                List<Project> projects = projectRepo.findAll();
                if (projects.isEmpty()) { System.out.println("No projects."); return; }
                for (int i = 0; i < projects.size(); i++)
                    System.out.printf("  %3d. %s%n", i + 1, projects.get(i));
                System.out.print("Select project: ");
                int idx = readIndex(projects.size());
                if (idx < 0) return;
                candidates = taskRepo.findByProject(projects.get(idx).getId());
            }
            case "3" -> {
                System.out.print("From date (yyyy-MM-dd): ");
                String from = sc.nextLine().trim();
                System.out.print("To date   (yyyy-MM-dd): ");
                String to = sc.nextLine().trim();
                candidates = taskService.filterOpenDueInRange(taskRepo.findAll(), from, to);
            }
            default -> { System.out.println("Invalid choice."); return; }
        }

        List<Task> exportable = taskService.filterExportable(candidates);
        if (exportable.isEmpty()) {
            System.out.println("No tasks with a due date to export.");
            return;
        }

        System.out.print("Output file [tasks.ics]: ");
        String path = sc.nextLine().trim();
        if (path.isEmpty()) path = "tasks.ics";

        icalGateway.exportToIcs(exportable, path);
        System.out.println("Exported " + exportable.size() + " task(s) to " + path + ".");
    }

    // -------------------------------------------------------------------------
    // 8 — List overloaded collaborators
    // -------------------------------------------------------------------------

    private static void doListOverloaded() throws SQLException {
        List<Collaborator> collabs   = collabRepo.findAll();
        List<Task>         allTasks  = taskRepo.findAll();
        List<CollaboratorService.CollaboratorLoad> overloaded =
            collabService.findOverloaded(collabs, allTasks);

        if (overloaded.isEmpty()) {
            System.out.println("No overloaded collaborators.");
        } else {
            System.out.println("Overloaded collaborators (" + overloaded.size() + "):");
            overloaded.forEach(cl -> System.out.println("  - " + cl));
        }
    }

    // -------------------------------------------------------------------------
    // 9 — Import from CSV
    // -------------------------------------------------------------------------

    private static void doImportCSV() throws SQLException {
        System.out.print("File path: ");
        String path = sc.nextLine().trim();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String header = br.readLine();
            if (header == null) { System.out.println("Empty file."); return; }

            // Build lookup map for efficient upsert: "title|dueDate" -> Task
            Map<String, Task> existing = new HashMap<>();
            for (Task t : taskRepo.findAll()) {
                existing.put(t.getTitle().toLowerCase() + "|" + t.getDueDate(), t);
            }

            int added = 0, updated = 0;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] cols = line.split(",", -1);
                if (cols.length < 5) {
                    System.out.println("Skipping malformed row: " + line);
                    continue;
                }
                String    title     = cols[0].trim();
                String    desc      = cols[1].trim();
                Status    status    = Status.fromString(cols[2].trim());
                Priority  priority  = Priority.fromString(cols[3].trim());
                String    dueDate   = cols[4].trim();
                String    projName  = cols.length > 5 ? cols[5].trim() : "";
                Recurrence rec      = cols.length > 6
                    ? Recurrence.fromString(cols[6].trim()) : Recurrence.NONE;

                Project project = null;
                if (!projName.isEmpty()) {
                    project = projectRepo.findByName(projName).orElseGet(() -> {
                        try { return projectRepo.save(new Project(projName, "")); }
                        catch (SQLException e) { throw new RuntimeException(e); }
                    });
                }

                String key = title.toLowerCase() + "|" + dueDate;
                if (existing.containsKey(key)) {
                    Task t = existing.get(key);
                    t.setStatus(status);
                    taskRepo.save(t);
                    updated++;
                } else {
                    Task t = new Task(-1, title, desc, LocalDate.now().toString(),
                                     dueDate, priority, status, rec, project, null);
                    taskRepo.save(t);
                    added++;
                }
            }
            System.out.println("Import complete: " + added + " added, " + updated + " updated.");

        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + path);
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 10 — Export to CSV
    // -------------------------------------------------------------------------

    private static void doExportCSV() throws SQLException {
        List<Task> tasks = taskRepo.findAll();
        if (tasks.isEmpty()) { System.out.println("No tasks to export."); return; }

        System.out.print("Output file [tasks.csv]: ");
        String path = sc.nextLine().trim();
        if (path.isEmpty()) path = "tasks.csv";

        try (PrintWriter pw = new PrintWriter(new FileWriter(path))) {
            pw.println("TaskName,Description,Status,Priority,DueDate," +
                       "ProjectName,Recurrence,Collaborator,CollaboratorCategory");
            for (Task t : tasks) {
                pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    csv(t.getTitle()),
                    csv(t.getDescription()),
                    t.getStatus(),
                    t.getPriority(),
                    t.getDueDate(),
                    t.getProject()      != null ? csv(t.getProject().getName())      : "",
                    t.getRecurrence(),
                    t.getCollaborator() != null ? csv(t.getCollaborator().getName()) : "",
                    t.getCollaborator() != null ? t.getCollaborator().getCategory()  : ""
                );
            }
            System.out.println("Exported " + tasks.size() + " task(s) to " + path + ".");
        } catch (IOException e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Utility helpers
    // -------------------------------------------------------------------------

    /** Prompts the user to select or create a project; returns null if skipped. */
    private static Project promptProject() throws SQLException {
        System.out.print("Project name (optional, blank = no project): ");
        String projName = sc.nextLine().trim();
        if (projName.isEmpty()) return null;

        Optional<Project> existing = projectRepo.findByName(projName);
        if (existing.isPresent()) return existing.get();

        System.out.print("Project \"" + projName + "\" not found. Create it? [y/n]: ");
        if (!sc.nextLine().trim().equalsIgnoreCase("y")) return null;

        System.out.print("Description (optional): ");
        String projDesc = sc.nextLine().trim();
        return projectRepo.save(new Project(projName, projDesc));
    }

    private static void printNumberedList(List<?> items) {
        for (int i = 0; i < items.size(); i++)
            System.out.printf("  %3d. %s%n", i + 1, items.get(i));
    }

    /**
     * Reads a 1-based index from stdin and converts it to a 0-based index.
     * Returns -1 if the input is invalid.
     */
    private static int readIndex(int max) {
        try {
            int idx = Integer.parseInt(sc.nextLine().trim()) - 1;
            if (idx < 0 || idx >= max) { System.out.println("Invalid selection."); return -1; }
            return idx;
        } catch (NumberFormatException e) {
            System.out.println("Please enter a number.");
            return -1;
        }
    }

    /** Escapes commas in CSV values by replacing them with semicolons. */
    private static String csv(String s) {
        return s == null ? "" : s.replace(",", ";");
    }
}
