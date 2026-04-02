# Iteration III — Diagram Blueprints (PlantUML)

Copy each code block and paste it directly into https://www.plantuml.com/plantuml/uml/
(or use the PlantUML VS Code extension, or the desktop JAR).
Export as PNG/SVG for submission.

---

## 1. Updated Use-Case Diagram

```plantuml
@startuml
left to right direction
skinparam packageStyle rectangle
skinparam usecase {
  BackgroundColor White
  BorderColor Black
}

actor User as U

rectangle "Task Management System" {

  package "Task Lifecycle" {
    usecase "Create Task" as UC1
    usecase "Complete Task" as UC2
    usecase "Cancel Task" as UC3
    usecase "Reopen Task" as UC4
    usecase "Add Subtask" as UC5
    usecase "View / Search Tasks" as UC6
    usecase "View Task History" as UC7
  }

  package "Project Management" {
    usecase "Create Project" as UC8
    usecase "List Projects" as UC9
  }

  package "Collaborator Management" {
    usecase "Add Collaborator to Project" as UC10
    usecase "Assign Collaborator to Task" as UC11
    usecase "List Collaborators" as UC12
    usecase "List Overloaded Collaborators" as UC13
  }

  package "Data Exchange" {
    usecase "Import from CSV" as UC14
    usecase "Export to CSV" as UC15
    usecase "Export to iCalendar" as UC16
  }
}

U --> UC1
U --> UC2
U --> UC3
U --> UC4
U --> UC5
U --> UC6
U --> UC7
U --> UC8
U --> UC9
U --> UC10
U --> UC11
U --> UC12
U --> UC13
U --> UC14
U --> UC15
U --> UC16

UC16 ..> UC6 : <<include>>
UC11 ..> UC10 : <<include>>

note right of UC13
  Lists collaborators whose
  open task count exceeds
  their category limit
end note

note right of UC16
  Exports only tasks
  with a due date (.ics)
end note
@enduml
```

---

## 2. Updated UML Class Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
skinparam linetype ortho

package "taskmanager.domain" {

  class Task {
    - id : int
    - title : String
    - description : String
    - creationDate : String
    - dueDate : String
    - priority : Priority
    - status : Status
    - recurrence : Recurrence
    --
    + hasDueDate() : boolean
    + getSubtasks() : List<Subtask>
    + getTags() : List<String>
  }

  class Subtask {
    - id : int
    - title : String
    - status : Status
    - parentTaskId : int
  }

  class Project {
    - id : int
    - name : String
    - description : String
  }

  class Collaborator {
    - id : int
    - name : String
    - category : CollaboratorCategory
    - projectId : int
  }

  enum CollaboratorCategory {
    JUNIOR
    INTERMEDIATE
    SENIOR
    --
    + getOpenTaskLimit() : int
  }

  enum Priority {
    LOW
    MEDIUM
    HIGH
  }

  enum Status {
    OPEN
    COMPLETED
    CANCELLED
  }

  enum Recurrence {
    NONE
    DAILY
    WEEKLY
    MONTHLY
  }

  class TaskService {
    + validateAddSubtask(parent : Task) : void
    + validateNewTaskWithoutDueDate(all : List<Task>, t : Task) : void
    + validateAssignCollaborator(c : Collaborator, tasks : List<Task>) : void
    + filterExportable(tasks : List<Task>) : List<Task>
    + filterOpenDueInRange(tasks : List<Task>, from : String, to : String) : List<Task>
  }

  class CollaboratorService {
    + findOverloaded(collaborators : List<Collaborator>, allTasks : List<Task>) : List<CollaboratorLoad>
  }

  class "CollaboratorService::CollaboratorLoad" as CollaboratorLoad {
    + collaborator : Collaborator
    + openCount : int
  }

  Task "1" *-- "0..20" Subtask : contains >
  Task "0..*" --> "0..1" Project : belongs to >
  Task "0..*" --> "0..1" Collaborator : assigned to >
  Collaborator "0..*" --> "1" Project : defined under >
  Collaborator --> CollaboratorCategory
  CollaboratorService --> CollaboratorLoad : produces
}

package "taskmanager.persistence" {

  class Database {
    - {static} connection : Connection
    + {static} getConnection() : Connection
    + {static} close() : void
  }

  class TaskRepository {
    + save(t : Task) : Task
    + findAll() : List<Task>
    + findByProject(projectId : int) : List<Task>
    + findByCollaborator(collaboratorId : int) : List<Task>
    + findById(id : int) : Optional<Task>
    + search(keyword : String) : List<Task>
  }

  class ProjectRepository {
    + save(p : Project) : Project
    + findByName(name : String) : Optional<Project>
    + findById(id : int) : Optional<Project>
    + findAll() : List<Project>
  }

  class CollaboratorRepository {
    + save(c : Collaborator) : Collaborator
    + findById(id : int) : Optional<Collaborator>
    + findAll() : List<Collaborator>
    + findByProjectId(projectId : int) : List<Collaborator>
  }

  class SubtaskRepository {
    + save(s : Subtask) : Subtask
    + findByTaskId(taskId : int) : List<Subtask>
  }

  TaskRepository --> Database : uses >
  ProjectRepository --> Database : uses >
  CollaboratorRepository --> Database : uses >
  SubtaskRepository --> Database : uses >

  TaskRepository --> ProjectRepository : uses >
  TaskRepository --> CollaboratorRepository : uses >
  TaskRepository --> SubtaskRepository : uses >
}

package "taskmanager.ical" {

  interface ICalGateway {
    + exportToIcs(tasks : List<Task>, filePath : String) : void
  }

  class ICalGatewayImpl {
    - buildDescription(task : Task) : String
    - mapPriority(task : Task) : int
    - mapStatus(task : Task) : Status
    + exportToIcs(tasks : List<Task>, filePath : String) : void
  }

  class "iCal4j\n(external library)" as ical4j <<external>> {
    Calendar
    VEvent
    CalendarOutputter
  }

  ICalGatewayImpl ..|> ICalGateway
  ICalGatewayImpl ..> ical4j : <<uses>>
}

package "taskmanager.app" {

  class Main <<boundary>> {
    - taskRepo : TaskRepository
    - projectRepo : ProjectRepository
    - collabRepo : CollaboratorRepository
    - subtaskRepo : SubtaskRepository
    - taskService : TaskService
    - collabService : CollaboratorService
    - icalGateway : ICalGateway
    --
    + {static} main(args : String[]) : void
  }

  Main --> TaskRepository
  Main --> ProjectRepository
  Main --> CollaboratorRepository
  Main --> SubtaskRepository
  Main --> TaskService
  Main --> CollaboratorService
  Main --> ICalGateway
}

@enduml
```

---

## 3. Sequence Diagram — Export Filtered Tasks to iCalendar

**Scenario:** User picks option 7, chooses scope "3" (filtered — open tasks
due this week), enters a date range, and exports to `tasks.ics`.

```plantuml
@startuml
skinparam sequenceArrowThickness 1.5
skinparam sequenceParticipantBorderColor Black
skinparam sequenceLifeLineBorderColor Gray

actor User
participant "Main\n(CLI)" as Main
participant "TaskRepository" as TaskRepo
participant "TaskService" as TaskSvc
participant "ICalGatewayImpl" as Gateway
participant "iCal4j" as ical4j

User -> Main : select option 7\n(Export to iCalendar)
activate Main

Main -> User : display scope menu\n(1=single / 2=project / 3=filtered)
User -> Main : scope = 3\nfrom = "2026-04-01"\nto   = "2026-04-07"

Main -> TaskRepo : findAll()
activate TaskRepo
TaskRepo --> Main : List<Task> allTasks
deactivate TaskRepo

Main -> TaskSvc : filterOpenDueInRange(allTasks, from, to)
activate TaskSvc
TaskSvc --> Main : List<Task> filtered
deactivate TaskSvc

Main -> TaskSvc : filterExportable(filtered)
activate TaskSvc
note right : removes tasks\nwithout a due date
TaskSvc --> Main : List<Task> exportable
deactivate TaskSvc

Main -> User : "Output file [tasks.ics]:"
User -> Main : "tasks.ics"

Main -> Gateway : exportToIcs(exportable, "tasks.ics")
activate Gateway

Gateway -> ical4j : new Calendar()
activate ical4j
ical4j --> Gateway : calendar
deactivate ical4j

loop for each task in exportable
  Gateway -> ical4j : new VEvent(dueDate, title)
  activate ical4j
  ical4j --> Gateway : event
  deactivate ical4j
  Gateway -> ical4j : event.add(Description)\n  includes subtask summary
  Gateway -> ical4j : event.add(Priority)
  Gateway -> ical4j : event.add(Status)
  Gateway -> ical4j : calendar.getComponents().add(event)
end

Gateway -> ical4j : CalendarOutputter(false)\n  .output(calendar, FileOutputStream)
note right of Gateway : writes tasks.ics\nto disk

deactivate Gateway

Main --> User : "Exported N task(s) to tasks.ics."
deactivate Main
@enduml
```

---

## Tips for submission

- Paste each block (including `@startuml` / `@enduml`) into https://www.plantuml.com/plantuml/uml/
- Export as **PNG** (or SVG for higher quality)
- Save the PNGs inside `Iteration3/` alongside the code
- Suggested filenames:
  - `Iteration3/use-case-diagram.png`
  - `Iteration3/class-diagram.png`
  - `Iteration3/sequence-diagram-ical-export.png`

## Key change reflected vs. earlier blueprint

The `Collaborator` class now has `projectId : int` and an explicit association
`Collaborator "0..*" --> "1" Project : defined under` — this reflects the fix
that made collaborators project-scoped per the spec.
