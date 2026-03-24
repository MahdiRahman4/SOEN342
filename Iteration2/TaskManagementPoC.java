import java.io.*;
import java.util.*;

// TODO: split into multiple files eventually
public class TaskManagementPoC {

    enum Priority { LOW, MEDIUM, HIGH }
    enum Status { OPEN, COMPLETED }
    // TODO: actually hook up recurrence logic, right now it just gets stored
    enum Recurrence { NONE, DAILY, WEEKLY, MONTHLY }

    static class Task {
        String title;
        String description;
        String dueDate;
        Priority priority;
        Status status;
        Recurrence recurrence;
        String project;

        Task(String title, String description, Priority priority, String dueDate) {
            this.title = title;
            this.description = description;
            this.priority = priority;
            this.dueDate = dueDate;
            this.status = Status.OPEN;
            this.recurrence = Recurrence.NONE;
            this.project = "";
        }

        @Override
        public String toString() {
            String s = "[" + status + "] " + title + " (" + priority + ") - due: " + dueDate;
            if (!project.isEmpty())
                s += " [" + project + "]";
            return s;
        }
    }

    // TODO: switch to db
    static List<Task> tasks = new ArrayList<>();

    static List<Task> search(String keyword) {
        List<Task> out = new ArrayList<>();
        String kw = keyword.trim().toLowerCase();

        for (Task t : tasks) {
            if (kw.isEmpty()) {
                if (t.status == Status.OPEN)
                    out.add(t);
            } else {
                if (t.title.toLowerCase().contains(kw)
                        || t.description.toLowerCase().contains(kw)
                        || t.project.toLowerCase().contains(kw)
                        || t.priority.name().toLowerCase().contains(kw)
                        || t.status.name().toLowerCase().contains(kw)
                        || t.dueDate.contains(kw)) {
                    out.add(t);
                }
            }
        }

        // sort by due date, nulls/empty will just go to the top which is fine
        out.sort(Comparator.comparing(t -> t.dueDate));
        return out;
    }

    static void exportCSV(String path) throws IOException {
        FileWriter fw = new FileWriter(path);
        fw.write("TaskName,Description,Status,Priority,DueDate,ProjectName,Recurrence\n");
        for (Task t : tasks) {
            // FIXME: commas in title/desc will break this, good enough for now
            fw.write(t.title + "," + t.description + "," + t.status + ","
                    + t.priority + "," + t.dueDate + "," + t.project + "," + t.recurrence + "\n");
        }
        fw.close();
    }

    static int importCSV(String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String header = br.readLine(); 
        if (header == null) { br.close(); return 0; }

        int added = 0;
        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            String[] cols = line.split(",", -1);
            if (cols.length < 5) {
                System.out.println("skipping malformed line: " + line);
                continue;
            }

            String title   = cols[0].trim();
            String desc    = cols[1].trim();
            String dueDate = cols[4].trim();

            Status status = Status.OPEN;
            try { status = Status.valueOf(cols[2].trim().toUpperCase()); }
            catch (IllegalArgumentException e) { /* leave as OPEN */ }

            Priority priority = Priority.MEDIUM;
            try { priority = Priority.valueOf(cols[3].trim().toUpperCase()); }
            catch (IllegalArgumentException e) { /* leave as MEDIUM */ }

            Recurrence rec = Recurrence.NONE;
            if (cols.length > 6) {
                try { rec = Recurrence.valueOf(cols[6].trim().toUpperCase()); }
                catch (IllegalArgumentException e) { /* leave as NONE */ }
            }

            // if task with same name+date exists just update its status
            boolean found = false;
            for (Task t : tasks) {
                if (t.title.equalsIgnoreCase(title) && t.dueDate.equals(dueDate)) {
                    t.status = status;
                    found = true;
                    break;
                }
            }
            if (found) continue;

            Task t = new Task(title, desc, priority, dueDate);
            t.status = status;
            t.project = cols.length > 5 ? cols[5].trim() : "";
            t.recurrence = rec;
            tasks.add(t);
            added++;
        }

        br.close();
        return added;
    }

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) throws IOException {
        System.out.println("Task Manager");
        System.out.println("------------");

        while (true) {
            System.out.println();
            System.out.println("1) view/search tasks");
            System.out.println("2) add task");
            System.out.println("3) complete task");
            System.out.println("4) import csv");
            System.out.println("5) export csv");
            System.out.println("6) quit");
            System.out.print("> ");

            String input = sc.nextLine().trim();
            switch (input) {
                case "1": doSearch(); break;
                case "2": doAdd(); break;
                case "3": doComplete(); break;
                case "4": doImport(); break;
                case "5": doExport(); break;
                case "6": System.out.println("bye"); return;
                default:  System.out.println("huh?");
            }
        }
    }

    static void doSearch() {
        System.out.print("search (enter to show open tasks): ");
        List<Task> results = search(sc.nextLine());
        if (results.isEmpty()) {
            System.out.println("nothing found");
            return;
        }
        for (int i = 0; i < results.size(); i++) {
            System.out.println("  " + (i+1) + ". " + results.get(i));
        }
    }

    static void doAdd() {
        System.out.print("title: ");
        String title = sc.nextLine().trim();
        if (title.isEmpty()) { System.out.println("title can't be empty"); return; }

        System.out.print("description: ");
        String desc = sc.nextLine().trim();

        System.out.print("priority (low/medium/high) [medium]: ");
        String pStr = sc.nextLine().trim().toUpperCase();
        Priority p = Priority.MEDIUM;
        if (!pStr.isEmpty()) {
            try { p = Priority.valueOf(pStr); }
            catch (IllegalArgumentException e) { System.out.println("didn't recognize that, defaulting to MEDIUM"); }
        }

        System.out.print("due date (yyyy-MM-dd): ");
        String due = sc.nextLine().trim();

        System.out.print("project (optional): ");
        String proj = sc.nextLine().trim();

        Task t = new Task(title, desc, p, due);
        t.project = proj;
        tasks.add(t);
        System.out.println("added: " + t);
    }

    static void doComplete() {
        List<Task> open = search("");
        if (open.isEmpty()) { System.out.println("no open tasks"); return; }

        for (int i = 0; i < open.size(); i++)
            System.out.println("  " + (i+1) + ". " + open.get(i));

        System.out.print("which one? ");
        try {
            int idx = Integer.parseInt(sc.nextLine().trim()) - 1;
            if (idx < 0 || idx >= open.size()) { System.out.println("invalid"); return; }
            open.get(idx).status = Status.COMPLETED;
            System.out.println("done: " + open.get(idx).title);
        } catch (NumberFormatException e) {
            System.out.println("that's not a number");
        }
    }

    static void doImport() {
        System.out.print("file path: ");
        String path = sc.nextLine().trim();
        try {
            int n = importCSV(path);
            System.out.println("imported " + n + " task(s)");
        } catch (FileNotFoundException e) {
            System.out.println("can't find file: " + path);
        } catch (IOException e) {
            System.out.println("something went wrong: " + e.getMessage());
        }
    }

    static void doExport() {
        if (tasks.isEmpty()) { System.out.println("nothing to export"); return; }

        System.out.print("filename [tasks.csv]: ");
        String path = sc.nextLine().trim();
        if (path.isEmpty()) path = "tasks.csv";

        try {
            exportCSV(path);
            System.out.println("exported " + tasks.size() + " task(s) to " + path);
        } catch (IOException e) {
            System.out.println("export failed: " + e.getMessage());
        }
    }
}