package Util;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import model.Project;

import java.util.List;
import java.util.Optional;

public class AverageIndexAkka extends AbstractBehavior<AverageIndexAkka.Command> {

    public AverageIndexAkka(ActorContext<Command> context) {
        super(context);
    }

    //protocols
    public static final class RecordListOfProject implements Command {
        final long requestId;
        final List<Project> projects;

        public RecordListOfProject(long requestId, List<Project> projects) {
            this.requestId = requestId;
            this.projects = projects;
        }
    }

    public static final class CalculateAvergaeIndex implements Command {
        final long requestId;
        final ActorRef<RespondAverageIndex> replyTo;

        public CalculateAvergaeIndex(long requestId, ActorRef<RespondAverageIndex> replyTo) {
            this.requestId = requestId;
            this.replyTo = replyTo;
        }
    }

    public static final class RespondAverageIndex {
        final long requestId;
        final Double averageIndex;

        public RespondAverageIndex(long requestId, Double averageIndex) {
            this.requestId = requestId;
            this.averageIndex = averageIndex;
        }
    }

    //states
    private Optional<List<Project>> projects = Optional.empty();

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(AverageIndexAkka.RecordListOfProject.class, this::onRecordProjects)
                .onMessage(AverageIndexAkka.CalculateAvergaeIndex.class, this::onCalculateAverageIndex)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onCalculateAverageIndex(M r) {
        Optional<Integer> sum = this.projects.get().stream().map(p -> p.getReadabilityIndex()).reduce(Integer::sum);
        double average = sum.get() / this.projects.get().size();
        r.replyTo.tell(new RespondAverageIndex(r.requestId, average));
        return this;
    }

    private Behavior<Command> onRecordProjects(RecordListOfProject r) {
        this.projects = Optional.of(r.projects);
        return this;
    }

    public interface Command {
    }

    private Behavior<AverageIndexAkka.Command> onPostStop() {
        return Behaviors.stopped();
    }
}
