package taskmanager.domain;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Domain service for collaborator-related queries, in particular detecting
 * overloaded collaborators as required by the Iteration III menu item.
 */
public class CollaboratorService {

    /**
     * Returns a load report for every collaborator whose number of open assigned
     * tasks exceeds their category limit.
     *
     * OCL: context Collaborator inv NoOverload:
     *   Task.allInstances()
     *     ->select(t | t.collaborator = self and t.status = Status::OPEN)
     *     ->size() <= self.category.openTaskLimit
     *
     * This method surfaces existing violations (e.g. caused by reopening tasks
     * or changing category limits) rather than preventing them.
     */
    public List<CollaboratorLoad> findOverloaded(List<Collaborator> collaborators,
                                                  List<Task> allTasks) {
        return collaborators.stream()
            .map(c -> {
                long open = allTasks.stream()
                    .filter(t -> t.getCollaborator() != null
                              && t.getCollaborator().getId() == c.getId()
                              && t.getStatus() == Status.OPEN)
                    .count();
                return new CollaboratorLoad(c, (int) open);
            })
            .filter(cl -> cl.openCount > cl.collaborator.getCategory().getOpenTaskLimit())
            .collect(Collectors.toList());
    }

    /** Value object carrying a collaborator and their current open-task count. */
    public static class CollaboratorLoad {
        public final Collaborator collaborator;
        public final int openCount;

        public CollaboratorLoad(Collaborator collaborator, int openCount) {
            this.collaborator = collaborator;
            this.openCount = openCount;
        }

        @Override
        public String toString() {
            return collaborator.getName() +
                   " (" + collaborator.getCategory() +
                   ", limit=" + collaborator.getCategory().getOpenTaskLimit() +
                   ", open=" + openCount + ")";
        }
    }
}
