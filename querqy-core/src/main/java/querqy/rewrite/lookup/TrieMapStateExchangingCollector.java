package querqy.rewrite.lookup;

import querqy.CompoundCharSequence;
import querqy.model.Term;
import querqy.rewrite.commonrules.model.TermMatch;
import querqy.rewrite.commonrules.model.TermMatches;
import querqy.rewrite.lookup.model.Match;
import querqy.rewrite.lookup.model.Sequence;
import querqy.trie.State;
import querqy.trie.States;
import querqy.trie.TrieMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class TrieMapStateExchangingCollector<T> implements StateExchangingCollector<State<T>, T> {

    private final TrieMap<T> trieMap;
    private final boolean ignoreCase;

    private final List<Match<T>> matches = new ArrayList<>();

    private TrieMapStateExchangingCollector(final TrieMap<T> trieMap, final boolean ignoreCase) {
        this.trieMap = trieMap;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public Optional<State<T>> evaluateTerm(final Term term) {
        // TODO: why with field?
        final CharSequence lookupCharSequence = term.toCharSequenceWithField(ignoreCase);
        final States<T> states = trieMap.get(lookupCharSequence);

        if (hasMatch(states)) {
            MatchExtractor.of(term, states)
                    .extractMatches()
                    .forEach(matches::add);
        }

        return getLookupState(states);
    }

    @Override
    public Optional<State<T>> evaluateNextTerm(final Sequence<State<T>> sequence, final Term term) {
        final CharSequence lookupCharSequence = new CompoundCharSequence(
                null, " ", term.toCharSequenceWithField(ignoreCase));

        final States<T> states = trieMap.get(lookupCharSequence, sequence.getState());

        if (hasMatch(states)) {
            MatchExtractor.of(sequence, term, states)
                    .extractMatches()
                    .forEach(matches::add);
        }

        return getLookupState(states);
    }

    @Override
    public List<Match<T>> getMatches() {
        return Collections.unmodifiableList(matches);
    }

    private boolean hasMatch(final States<T> states) {
        return states.getStateForCompleteSequence().isFinal() || states.getPrefixes() != null;
    }

    private Optional<State<T>> getLookupState(final States<T> states) {
        final State<T> stateExactMatch = states.getStateForCompleteSequence();
        return stateExactMatch.isKnown() ? Optional.of(stateExactMatch) : Optional.empty();
    }


    public static <T> TrieMapStateExchangingCollector<T> of(final TrieMap<T> trieMap, final boolean ignoreCase) {
        return new TrieMapStateExchangingCollector<>(trieMap, ignoreCase);
    }

    public static <T> TrieMapStateExchangingCollectorBuilder<T> builder() {
        return new TrieMapStateExchangingCollectorBuilder<>();
    }

    public static class TrieMapStateExchangingCollectorBuilder<T> {

        private TrieMap<T> trieMap;
        private boolean ignoreCase;

        public TrieMapStateExchangingCollectorBuilder<T> trieMap(final TrieMap<T> trieMap) {
            this.trieMap = trieMap;
            return this;
        }

        public TrieMapStateExchangingCollectorBuilder<T> ignoreCase(final boolean ignoreCase) {
            this.ignoreCase = ignoreCase;
            return this;
        }

        public TrieMapStateExchangingCollector<T> build() {
            return new TrieMapStateExchangingCollector<>(trieMap, ignoreCase);
        }
    }

    private static class MatchExtractor<T> {

        private final Sequence<State<T>> sequence;
        private final Term term;
        private final States<T> states;

        private final Stream.Builder<Match<T>> matches = Stream.builder();

        private MatchExtractor(final Sequence<State<T>> sequence, final Term term, final States<T> states) {
            this.sequence = sequence;
            this.term = term;
            this.states = states;
        }

        public Stream<Match<T>> extractMatches() {
            addCompleteMatch();
            addPrefixMatches();
            return matches.build();
        }

        private void addCompleteMatch() {
            final State<T> stateExactMatch = states.getStateForCompleteSequence();
            if (stateExactMatch.isFinal()) {
                matches.add(Match.of(createTermMatches(-1), stateExactMatch.getValue()));
            }
        }

        private TermMatches createTermMatches(final int matchIndex) {
            return Stream
                    .concat(
                            streamFromSequenceTerms()
                                    .map(TermMatch::new),
                            Stream.of(term)
                                    .map(term -> createLastTermMatch(matchIndex))
                    )
                    .filter(TermMatch::isExpandable)
                    .collect(Collectors.toCollection(TermMatches::new));
        }

        private TermMatch createLastTermMatch(final int matchIndex) {
            return matchIndex < 0
                    ? new TermMatch(term)
                    : new TermMatch(term, true, matchIndex);
        }

        private Stream<Term> streamFromSequenceTerms() {
            return sequence == null ? Stream.empty() : sequence.getTerms().stream();
        }

        private void addPrefixMatches() {
            for (final State<T> prefixState : getPrefixStates()) {
                final T value = prefixState.getValue();

                if (value != null) {
                    matches.add(Match.of(createTermMatches(prefixState.getIndex()), value));
                }
            }
        }

        private List<State<T>> getPrefixStates() {
            final List<State<T>> prefixMatches = states.getPrefixes();
            return prefixMatches == null ? List.of() : prefixMatches;
        }

        public static <T> MatchExtractor<T> of(final Term term, final States<T> states) {
            return of(null, term, states);
        }

        public static <T> MatchExtractor<T> of(
                final Sequence<State<T>> sequence, final Term term, final States<T> states) {
            return new MatchExtractor<>(sequence, term, states);
        }
    }
}