# SOEN342
DEMO LINK: https://youtu.be/BCFs5Yypems
|Student| Student ID|
| :--- | :---: | 
|Omar Ghazaly|40280795|
|Abderrahmane Bensassi-Nour |40317017|
|Mahdi Rahman| 40282926|

## Iteration I 
- Abderrahmane Bensassi-Nour : All of the SSD's
- Omar Ghazaly : Operation contracts and critical scenario
- Mahdi Rahman : Use case diagram, domain model, refining SSD's
## Iteration II
- Abderrahmane Bensassi-Nour : Verifying work done
- Omar Ghazaly : System interaction diagrams
- Mahdi Rahman : Updated past diagrams, POC code

## Iteration III
- Abderrahmane Bensassi-Nour : OCL constraints, updated use-case diagram
- Omar Ghazaly : iCal export sequence diagram, updated class diagram
- Mahdi Rahman : Full implementation (domain, persistence, iCal gateway, CLI), README

---

## Building and Running (Iteration III)

### Prerequisites
- Java 17 or later
- Maven 3.8 or later

### Build

```bash
cd Iteration3
mvn package -q
```

This produces `Iteration3/target/task-manager.jar` (a fat jar with all dependencies bundled).

### Run

```bash
java -jar Iteration3/target/task-manager.jar
```

The database file `taskmanager.db` is created in the current working directory on first run and persists across sessions.

### Sample workflow

```
 1) Search / view tasks           — list open tasks or search by keyword
 2) Add task                      — title, description, priority, due date, project
 4) Add subtask                   — attach a subtask to an open task (max 20)
 5) Manage projects               — create or list projects
 6) Manage collaborators          — add collaborators, assign them to tasks
 7) Export to iCalendar (.ics)    — export a single task, a project, or a filtered set
 8) List overloaded collaborators — shows any collaborator exceeding their category limit
 9) Import from CSV               — import/upsert tasks from a CSV file
10) Export to CSV                 — dump all tasks to CSV
```

### iCalendar export

Menu option 7 writes a standard `.ics` file importable by Google Calendar, Apple Calendar, or Outlook. Only tasks **with a due date** are exported. Subtasks are included as a summary inside the parent event's description, not as separate entries.

### Collaborator limits

| Category     | Open-task limit |
| ------------ | --------------- |
| Senior       | 2               |
| Intermediate | 5               |
| Junior       | 10              |

Assigning a task to a collaborator who is at their limit is blocked with an error. Option 8 lists any collaborators that became overloaded after the fact (e.g., due to a reopened task).

### OCL constraints

Formal OCL specifications are in `Iteration3/constraints.ocl`. Each constraint is also enforced at runtime in `TaskService` and `CollaboratorService`.

### Project structure (Iteration 3)

```
Iteration3/
├── pom.xml
├── constraints.ocl
└── src/main/java/taskmanager/
    ├── domain/          — entities, enums, domain services (OCL enforcement)
    ├── persistence/     — SQLite repositories (Database, TaskRepository, …)
    ├── ical/            — ICalGateway interface + iCal4j implementation
    └── app/             — Main.java (CLI presentation layer)
```
