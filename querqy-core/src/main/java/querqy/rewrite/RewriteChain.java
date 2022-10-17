/**
 * 
 */
package querqy.rewrite;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.model.logging.RewriteChainLogging;
import querqy.model.logging.RewriteLoggingConfig;
import querqy.model.logging.RewriterLogging;
import querqy.model.rewriting.RewriteChainOutput;
import querqy.model.rewriting.RewriterOutput;

/**
 * The chain of rewriters to manipulate a {@link Query}.
 * 
 * @author rene
 *
 */
public class RewriteChain {

    final List<RewriterFactory> factories;

    @Deprecated
    final Map<String, RewriterFactory> factoriesByName;

    public RewriteChain() {
        this(Collections.emptyList());
    }

    public RewriteChain(final List<RewriterFactory> factories) {
        this.factories = factories;
        factoriesByName = new HashMap<>(factories.size());
        factories.forEach(factory -> {
            final String rewriterId = factory.getRewriterId();
            if (rewriterId == null || rewriterId.trim().isEmpty()) {
                throw new IllegalArgumentException("Missing rewriter id for factory: " + factory.getClass().getName());
            }
            if (factoriesByName.put(rewriterId, factory) != null) {
                throw new IllegalArgumentException("Duplicate rewriter id: " + rewriterId);
            }
        });
    }

    public RewriteChainOutput rewrite(final ExpandedQuery query,
                                      final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        final RewritingExecutor executor = new RewritingExecutor(factories, searchEngineRequestAdapter, query);
        return executor.rewrite();
    }

    @Deprecated
    public List<RewriterFactory> getRewriterFactories() {
        return factories;
    }

    @Deprecated
    public RewriterFactory getFactory(final String rewriterId) {
        return factoriesByName.get(rewriterId);
    }

    private static class RewritingExecutor {

        private final List<RewriterFactory> rewriterFactories;

        private final SearchEngineRequestAdapter searchEngineRequestAdapter;
        private final RewriteLoggingConfig rewriteLoggingConfig;

        private ExpandedQuery expandedQuery;

        private final RewriteChainLogging.RewriteChainLoggingBuilder rewriteChainLoggingBuilder = RewriteChainLogging.builder();

        public RewritingExecutor(
                final List<RewriterFactory> rewriterFactories,
                final SearchEngineRequestAdapter searchEngineRequestAdapter,
                final ExpandedQuery expandedQuery
        ) {
            this.rewriterFactories = rewriterFactories;

            this.searchEngineRequestAdapter = searchEngineRequestAdapter;
            this.rewriteLoggingConfig = searchEngineRequestAdapter.getRewriteLoggingConfig();

            this.expandedQuery = expandedQuery;
        }

        public RewriteChainOutput rewrite() {
            for (final RewriterFactory factory : rewriterFactories) {
                final RewriterOutput rewriterOutput = applyFactory(factory);
                parseLogging(factory.getRewriterId(), rewriterOutput.getRewriterLogging());
                expandedQuery = rewriterOutput.getExpandedQuery();
            }

            return buildOutput();
        }

        private RewriterOutput applyFactory(final RewriterFactory factory) {
            final QueryRewriter rewriter = factory.createRewriter(expandedQuery, searchEngineRequestAdapter);
            return rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);
        }

        private void parseLogging(final String factoryId, final RewriterLogging rewriterLogging) {
            final Set<String> includedIds = rewriteLoggingConfig.getIncludedRewriters();

            if (rewriteLoggingConfig.isActive()) {
                if (includedIds.isEmpty() || includedIds.contains(factoryId)) {
                    addLogging(factoryId, rewriterLogging);
                }
            }
        }

        private void addLogging(final String factoryId, final RewriterLogging rewriterLogging) {
            if (rewriteLoggingConfig.hasDetails()) {
                rewriteChainLoggingBuilder.add(factoryId, rewriterLogging.getActionLoggings());

            } else {
                rewriteChainLoggingBuilder.add(factoryId);
            }
        }

        private RewriteChainOutput buildOutput() {
            return RewriteChainOutput.builder()
                    .expandedQuery(expandedQuery)
                    .rewriteLogging(rewriteChainLoggingBuilder.build())
                    .build();
        }
    }
}
