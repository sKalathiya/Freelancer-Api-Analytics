package Util;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import model.Project;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DescriptionAkka extends AbstractBehavior<DescriptionAkka.Command> {

    public DescriptionAkka(ActorContext<Command> context) {
        super(context);
    }

    //protocols
    public static final class RecordProject implements Command {

        final long requestId;
        final Project project;
        final ActorRef<ProjectRecorded> replyTo;

        public RecordProject(long requestId, Project project, ActorRef<ProjectRecorded> replyTo) {
            this.requestId = requestId;
            this.project = project;
            this.replyTo = replyTo;
        }
    }

    public static final class ProjectRecorded {
        final long requestId;

        public ProjectRecorded(long requestId) {
            this.requestId = requestId;
        }
    }

    public static final class calculateReadabiityIndex implements Command{
        final long requestId;
        final ActorRef<RespondReadabilityIndex> replyTo;

        public calculateReadabiityIndex(long requestId, ActorRef<RespondReadabilityIndex> replyTo) {
            this.requestId = requestId;
            this.replyTo = replyTo;
        }
    }

    public static final class RespondReadabilityIndex{
        final long requestId;
        final Project project;

        public RespondReadabilityIndex(long requestId, Project project ) {
            this.requestId = requestId;
            this.project = project;
        }
    }


    public static Behavior<DescriptionAkka.Command> create() {
        return Behaviors.setup(context -> new DescriptionAkka(context));
    }

    //data holders
    private Optional<Project> project = Optional.empty();



    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(RecordProject.class,this::onRecordProject)
                .onMessage(calculateReadabiityIndex.class, this::onCalculateReadabilityIndex)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onCalculateReadabilityIndex(calculateReadabiityIndex r) {
        Project p = this.project.get();
        double sentences = p.getDesc().split("[.!?:;]+").length;
        String[] w = p.getDesc().split("[ .!?;:\\s]+");
        double words = w.length;
        double syllables = 0;
        Pattern pattern = Pattern.compile("[aeiouyAEIOUY]+");
        for (int i = 0; i < w.length; i++) {
            Matcher matcher = pattern.matcher(w[i]);
            while (matcher.find()) {

                if (w[i].length() <= 3) {
                    syllables++;
                    break;
                }
                if (matcher.group().equals("e")) {
                    if (matcher.end() == w[i].length()) {
                        continue;
                    }
                    if (matcher.end() == (w[i].length() - 1)) {
                        if (!(w[i].endsWith("es") || w[i].endsWith("ed"))) {
                            syllables++;
                        }
                        continue;
                    }
                }
                syllables++;
            }
        }


        int index = (int) (206.835 - (84.6 * (syllables / words)) - (1.015 * (words / sentences)));
        int fkgl = (int) (-15.59 + (11.8 * (syllables / words)) + (0.39 * (words / sentences)));
        p.setReadabilityIndex(index);
        p.setFkglIndex(fkgl);
        p.setEducationLevel(getIndexLevel(index));
        this.project = Optional.of(p);
        r.replyTo.tell(new RespondReadabilityIndex(r.requestId,this.project.get()));
        return  this;
    }

    private String getIndexLevel(int fleschIndex) {

        String educationLevel;
        if (fleschIndex > 100) {
            educationLevel = "4th grader";
        } else if (fleschIndex > 91) {
            educationLevel = "5th grader";
        } else if (fleschIndex > 81) {
            educationLevel = "6th grader";
        } else if (fleschIndex > 71) {
            educationLevel = "7th grader";
        } else if (fleschIndex > 61) {
            educationLevel = "8th grader";
        } else if (fleschIndex > 51) {
            educationLevel = "9th grader";
        } else if (fleschIndex > 41) {
            educationLevel = "high school graduate";
        } else if (fleschIndex > 31) {
            educationLevel = "Some college";
        } else if (fleschIndex > 0) {
            educationLevel = "college graduate";
        } else {
            educationLevel = "law School graduate";
        }
        return educationLevel;
    }

    private Behavior<Command> onRecordProject(RecordProject r) {
        this.project = Optional.of(r.project);
        r.replyTo.tell(new ProjectRecorded(r.requestId));
        return this;
    }

    public interface Command {}

    private Behavior<Command> onPostStop() {
        return Behaviors.stopped();
    }
}
