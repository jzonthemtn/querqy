package querqy.rewrite.commonrules.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static querqy.QuerqyMatchers.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;
import querqy.rewrite.commonrules.model.DeleteInstruction;
import querqy.rewrite.commonrules.model.Input;
import querqy.rewrite.commonrules.model.Instruction;
import querqy.rewrite.commonrules.model.Instructions;

public class DeleteInstructionTest extends AbstractCommonRulesTest {
    
    final static Map<String, Object> EMPTY_CONTEXT = Collections.emptyMap();

   @Test
   public void testThatNothingIsDeletedIfWeWouldEndUpWithAnEmptyQuery() {

      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
      DeleteInstruction delete = new DeleteInstruction(Arrays.asList(mkTerm("a")));
      builder.addRule(new Input(Arrays.asList(mkTerm("a")), false, false), new Instructions(Arrays.asList((Instruction) delete)));
      RulesCollection rules = builder.build();
      CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

      ExpandedQuery query = makeQuery("a");
      Query rewritten = rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

      assertThat(rewritten,
            bq(
                    dmq(
                            term("a")
                       )
            ));

   }

   @Test
   public void testThatTermIsRemovedIfThereIsAnotherTermInTheSameDMQ() throws Exception {
      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
      DeleteInstruction delete = new DeleteInstruction(Arrays.asList(mkTerm("a")));
      builder.addRule(new Input(Arrays.asList(mkTerm("a")), false, false), new Instructions(Arrays.asList((Instruction) delete)));
      RulesCollection rules = builder.build();
      CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

      ExpandedQuery expandedQuery = makeQuery("a");
      Query query = expandedQuery.getUserQuery();

      DisjunctionMaxQuery dmq = query.getClauses(DisjunctionMaxQuery.class).get(0);
      querqy.model.Term termB = new querqy.model.Term(dmq, null, "b");
      dmq.addClause(termB);

      Query rewritten = rewriter.rewrite(expandedQuery, EMPTY_CONTEXT).getUserQuery();

      assertThat(rewritten,
            bq(
                    dmq(
                            term("b")
                       )
            ));
   }

   @Test
   public void testThatTermIsRemovedOnceIfItExistsTwiceInSameDMQAndNoOtherTermExistsInQuery() throws Exception {
      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
      DeleteInstruction delete = new DeleteInstruction(Arrays.asList(mkTerm("a")));
      builder.addRule(new Input(Arrays.asList(mkTerm("a")), false, false), new Instructions(Arrays.asList((Instruction) delete)));
      RulesCollection rules = builder.build();
      CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

      ExpandedQuery expandedQuery = makeQuery("a");
      Query query = expandedQuery.getUserQuery();

      DisjunctionMaxQuery dmq = query.getClauses(DisjunctionMaxQuery.class).get(0);

      querqy.model.Term termB = new querqy.model.Term(dmq, null, "a");
      dmq.addClause(termB);

      Query rewritten = rewriter.rewrite(expandedQuery, EMPTY_CONTEXT).getUserQuery();

      assertThat(rewritten,
            bq(
            dmq(
            term("a")
            )
            ));
   }

   @Test
   public void testThatTermIsRemovedIfThereASecondDMQWithoutTheTerm() throws Exception {
      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
      DeleteInstruction delete = new DeleteInstruction(Arrays.asList(mkTerm("a")));
      builder.addRule(new Input(Arrays.asList(mkTerm("a")), false, false), new Instructions(Arrays.asList((Instruction) delete)));
      RulesCollection rules = builder.build();
      CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

      Query rewritten = rewriter.rewrite(makeQuery("a b"), EMPTY_CONTEXT).getUserQuery();

      assertThat(rewritten,
            bq(
            dmq(
            term("b")
            )
            ));
   }

   @Test
   public void testThatTermIsNotRemovedOnceIfThereASecondDMQWithTheSameTermAndNoOtherTermExists() throws Exception {
      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
      DeleteInstruction delete = new DeleteInstruction(Arrays.asList(mkTerm("a")));
      builder.addRule(new Input(Arrays.asList(mkTerm("a")), false, false), new Instructions(Arrays.asList((Instruction) delete)));
      RulesCollection rules = builder.build();
      CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

      Query rewritten = rewriter.rewrite(makeQuery("a a"), EMPTY_CONTEXT).getUserQuery();

      assertThat(rewritten,
            bq(
            dmq(
            term("a")
            )
            ));
   }

}