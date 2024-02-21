package sootup.examples;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.model.SourceType;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.core.types.ReferenceType;
import sootup.core.views.View;
import sootup.java.bytecode.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.views.JavaView;
import sootup.core.jimple.common.stmt.AbstractDefinitionStmt;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.JNewExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JGotoStmt;
import sootup.analysis.intraprocedural.AbstractFlowAnalysis;
import sootup.core.graph.StmtGraph;

public class BasicSetup {

  public static void main(String[] args) {
    // constant declaration
    String listClassName = ListConstants.LIST_CLASS_NAME;
    String iteratorMethodName = ListConstants.ITERATOR_METHOD_NAME;
    String getMethodName = ListConstants.GET_METHOD_NAME;
    String isEmptyMethodName = ListConstants.IS_EMPTY_METHOD_NAME;
    String iteratorClassName = ListConstants.ITERATOR_CLASS_NAME;
    String objectClassName = ListConstants.OBJECT_CLASS_NAME;
    List<String> emptyList = ListConstants.EMPTY_LIST;
    List<String> singletonIntList = ListConstants.SINGLETON_INT_LIST;

    AnalysisInputLocation inputLocation = PathBasedAnalysisInputLocation.create(
        Paths.get("src/test/resources/Basicsetup/binary"), SourceType.Application);

    // Create a view for project, which allows us to retrieve classes
    View view = new JavaView(inputLocation);

    // Create a signature for the class we want to analyze
    ClassType classType = view.getIdentifierFactory().getClassType("ListJava");

    // Create a signature for the method we want to analyze
    MethodSignature methodSignature = view.getIdentifierFactory()
        .getMethodSignature(
            classType, "main", "void", Collections.singletonList("java.lang.String[]"));

    if (!view.getClass(classType).isPresent()) {
      System.out.println("Class not found!");
      return;
    }

    // Retrieve the specified class from the project.
    SootClass sootClass = view.getClass(classType).get();

    // Retrieve method
    view.getMethod(methodSignature);

    if (!sootClass.getMethod(methodSignature.getSubSignature()).isPresent()) {
      System.out.println("Method not found!");
      return; // Exit if the method is not found
    }

    // Retrieve the specified method from the class.
    SootMethod sootMethod = sootClass.getMethod(methodSignature.getSubSignature()).get();

    List<Value> TempNames = new ArrayList<>();
    Map<Value, Boolean> variableMap = new HashMap<>();
    List<Stmt> arrayListUnsafeStmt = new ArrayList<>();
    int arrayUnsafeUsageCount = 0;
    int arraySafeUsageCount = 0;

    MethodSignature arrayListIterator = view.getIdentifierFactory().getMethodSignature(
        listClassName, iteratorMethodName, iteratorClassName, emptyList);

    MethodSignature arrayListGet = view.getIdentifierFactory().getMethodSignature(
        listClassName, getMethodName, objectClassName, singletonIntList);

    MethodSignature arrayListIsEmpty = view.getIdentifierFactory().getMethodSignature(
        listClassName, isEmptyMethodName, "boolean", emptyList);

    ReferenceType arrayListType = (ReferenceType) view.getIdentifierFactory()
        .getClassType("java.util.ArrayList");

    // iterate over the sootMethod Jimple statements
    for (Stmt stmt : sootMethod.getBody().getStmts()) {
      if (stmt.containsInvokeExpr()) {
        AbstractInvokeExpr invokeExpr = (AbstractInvokeExpr) stmt.getInvokeExpr();
        // check if the invoke expression is a call to the get method of an ArrayList
        if (invokeExpr.getMethodSignature().equals(arrayListGet)) {
          List<Value> InvokeExprUses = invokeExpr.getUses();
          // iterate over the uses and check if it is part of the list of variables
          for (Value use : InvokeExprUses) {
            if (variableMap.containsKey(use)) {
              if (!variableMap.get(use)) {
                arrayUnsafeUsageCount++;
                arrayListUnsafeStmt.add(stmt);
              } else {
                variableMap.put(use, false);
              }
            }
          }
        }

        // check if the invoke expression is a call to the isEmpty method of an
        // ArrayList
        if (invokeExpr.getMethodSignature().equals(arrayListIsEmpty)) {
          List<Value> InvokeExprUses = invokeExpr.getUses();
          for (Value use : InvokeExprUses) {
            if (variableMap.containsKey(use)) {
              arraySafeUsageCount++;
              variableMap.put(use, true);
            }
          }
        }

        // check if the invoke expression is a call to the iterator method of an
        // ArrayList
        if (invokeExpr.getMethodSignature().equals(arrayListIterator)) {
          List<Value> InvokeExprUses = invokeExpr.getUses();
          // iterate over the uses and check if it is part of the list of variables
          for (Value use : InvokeExprUses) {
            if (variableMap.containsKey(use)) {
              if (!variableMap.get(use)) {
                arrayUnsafeUsageCount++;
                arrayListUnsafeStmt.add(stmt);
              } else {
                variableMap.put(use, false);
              }
            }
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
    }
    System.out.println("Array Unsafe Usage: " + arrayUnsafeUsageCount);
    System.out.println("Array Safe Usage: " + arraySafeUsageCount);

    // iterate and print the stmt for unsafe ArrayList usage:
    System.out.println("Unsafe ArrayList Usage:");
    for (Stmt stmt : arrayListUnsafeStmt) {
      System.out.println(stmt);
    }
  }
}
