package sootup.examples.arrayListAnalysis;

import sootup.examples.ArrayListFact;
import sootup.examples.ForwardAnalysis;
import sootup.examples.VulnerabilityReporter;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.AbstractInstanceInvokeExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.java.core.JavaSootMethod;
import sootup.core.jimple.common.stmt.AbstractDefinitionStmt;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.JNewExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JGotoStmt;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public class ArrayListAnalysis extends ForwardAnalysis<Set<ArrayListFact>> {

  public ArrayListAnalysis(@Nonnull JavaSootMethod method, @Nonnull VulnerabilityReporter reporter) {
    super(method, reporter);
  }

  @Override
  protected void flowThrough(@Nonnull Set<ArrayListFact> in, @Nonnull Stmt stmt, @Nonnull Set<ArrayListFact> out) {
    copy(in, out);

    // Example for handling ArrayList.get() without prior isEmpty() or size() checks
    if (stmt instanceof JInvokeStmt) {
      JInvokeStmt invokeStmt = (JInvokeStmt) stmt;
      if (invokeStmt.getInvokeExpr() instanceof JVirtualInvokeExpr) {
        JVirtualInvokeExpr invokeExpr = (JVirtualInvokeExpr) invokeStmt.getInvokeExpr();
        Value base = invokeExpr.getBase();
        String methodName = invokeExpr.getMethodSignature().getName();

        if (methodName.equals("get")) {
          // If 'get' is called, check if there was a prior 'isEmpty' or 'size' check
          boolean safeAccess = in.stream().anyMatch(fact -> fact.containsAlias(base) && fact.isChecked());
          if (!safeAccess) {
            reporter.reportVulnerability(method.getSignature(), stmt);
          }
        } else if (methodName.equals("isEmpty") || methodName.equals("size")) {
          // Mark access to the ArrayList as safe if 'isEmpty' or 'size' is called
          updateState(out, base, ArrayListFact.ArrayListState.Checked);
        }
      }
    } else if (stmt instanceof JGotoStmt) {
      continue;
    } else if (stmt.branches()) {
      continue;
    } else if (stmt instanceof JAssignStmt) {
      AbstractDefinitionStmt defstmt = (AbstractDefinitionStmt) stmt;

      // iterate over TempNames to check if the right operand is stored in TempNames
      for (Value temp : TempNames) {
        if (defstmt.getRightOp().equals(temp)) {
          variableMap.put(defstmt.getLeftOp(), false);
        }
      }

      // check if the right operand is a new array expression
      if (defstmt.getRightOp() instanceof JNewExpr) {
        JNewExpr newExpr = (JNewExpr) defstmt.getRightOp();
        if (newExpr.getType().equals(arrayListType)) {
          TempNames.add(defstmt.getLeftOp());
        }
      }
    }

    // Copy other relevant operations from your original analysis here...
  }

  private void updateState(Set<ArrayListFact> facts, Value arrayListValue, ArrayListFact.ArrayListState newState) {
    ArrayListFact relatedFact = facts.stream()
        .filter(fact -> fact.containsAlias(arrayListValue))
        .findFirst()
        .orElse(null);

    if (relatedFact != null) {
      relatedFact.updateState(newState);
    } else {
      ArrayListFact newFact = new ArrayListFact(newState);
      newFact.addAlias(arrayListValue);
      facts.add(newFact);
    }
  }

  @Nonnull
  @Override
  protected Set<ArrayListFact> newInitialFlow() {
    return new HashSet<>();
  }

  @Override
  protected void copy(@Nonnull Set<ArrayListFact> source, @Nonnull Set<ArrayListFact> dest) {
    dest.clear();
    source.forEach(fact -> dest.add(new ArrayListFact(fact)));
  }

  @Override
  protected void merge(@Nonnull Set<ArrayListFact> in1, @Nonnull Set<ArrayListFact> in2,
      @Nonnull Set<ArrayListFact> out) {
    out.clear();
    out.addAll(in1);
    out.addAll(in2);
  }
}