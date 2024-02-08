package sootup.examples;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.model.SourceType;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.core.views.View;
import sootup.java.bytecode.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.views.JavaView;

public class BasicSetup {

  public static void main(String[] args) {
    AnalysisInputLocation inputLocation =
        PathBasedAnalysisInputLocation.create(
            Paths.get("src/test/resources/Basicsetup/binary"), SourceType.Application);

    // Create a view for project, which allows us to retrieve classes
    View view = new JavaView(inputLocation);

    // Create a signature for the class we want to analyze
    ClassType classType = view.getIdentifierFactory().getClassType("ListJava");

    // Create a signature for the method we want to analyze
    MethodSignature methodSignature =
        view.getIdentifierFactory()
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

    // Read Jimple code of method
    List<String> listVariableNames = new ArrayList<>();
    Set<String> declaredLists = new HashSet<>();
    boolean isEmptyCheckDone = false;
    int unsafeOperationsCount = 0;

    // iterate over the sootMethod Jimple statements
    for (Stmt stmt : sootMethod.getBody().getStmts()) {
      List<Value> usesAndDefs = stmt.getUsesAndDefs();
      System.out.println(usesAndDefs);
      // for storing list names
      if (usesAndDefs.size() > 1) {
        if (usesAndDefs.get(1).toString().contains("new java.util.ArrayList")) {
          declaredLists.add(usesAndDefs.get(0).toString());
        }
        // Check for variable assignments that are known lists
        else if (declaredLists.contains(usesAndDefs.get(1).toString())) {
          listVariableNames.add(usesAndDefs.get(0).toString());
        }

        if (listVariableNames.contains(usesAndDefs.get(usesAndDefs.size() - 1).toString())) {
          // Check if this is an emptiness check
          if (usesAndDefs.get(1).toString().contains("isEmpty()")) {
            isEmptyCheckDone = true;
          }
          // Check for list access
          else if (usesAndDefs.get(1).toString().contains("get(")
              || usesAndDefs.get(1).toString().contains("iterator()")) {
            if (!isEmptyCheckDone) {
              // If isEmptyCheckDone is false, then it's an unsafe operation
              unsafeOperationsCount++;
            }
            // Reset the flag after a list access
            isEmptyCheckDone = false;
          }
        }
      }
    }

    // vairable that are list
    System.out.println("List variable names: " + listVariableNames);
    // number of unsafe operations
    System.out.println("Number of unsafe list operations: " + unsafeOperationsCount);
  }
}
