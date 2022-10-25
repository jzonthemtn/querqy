package querqy.model.logging;

import java.util.LinkedList;
import java.util.List;

public class RewriterLogging {

    private final boolean hasAppliedRewriting;
    private final List<ActionLogging> actionLoggings;

    private RewriterLogging(boolean hasAppliedRewriting, final List<ActionLogging> actionLoggings) {
        this.hasAppliedRewriting = hasAppliedRewriting;
        this.actionLoggings = actionLoggings;
    }

    public boolean hasAppliedRewriting() {
        return hasAppliedRewriting;
    }

    public List<ActionLogging> getActionLoggings() {
        return actionLoggings;
    }

    public static RewriterLoggingBuilder builder() {
        return new RewriterLoggingBuilder();
    }

    public static class RewriterLoggingBuilder {

        private boolean hasAppliedRewriting;
        private List<ActionLogging> actionLogging;

        public RewriterLoggingBuilder hasAppliedRewriting(final boolean hasAppliedRewriting) {
            this.hasAppliedRewriting = hasAppliedRewriting;
            return this;
        }

        public RewriterLoggingBuilder addActionLogging(final ActionLogging actionLogging) {
            if (this.actionLogging == null) {
                this.actionLogging = new LinkedList<>();
            }

            this.actionLogging.add(actionLogging);
            return this;
        }

        public RewriterLogging build() {
            if (actionLogging == null) {
                actionLogging = List.of();
            }

            return new RewriterLogging(hasAppliedRewriting, actionLogging);
        }
    }
}