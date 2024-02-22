package sootup;

import sootup.examples.VulnerabilityReporter;
import java.util.*;
import java.nio.file.*;

import sootup.examples.arrayListAnalysis.ArrayListAnalysis;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SootMethod;
import sootup.core.types.ClassType;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;
import sootup.core.model.SootClass;
import sootup.core.model.SourceType;
import sootup.core.signatures.MethodSignature;
import sootup.core.views.View;
import sootup.java.bytecode.inputlocation.PathBasedAnalysisInputLocation;

public abstract class ArrayListFlowAnalysis {

  protected JavaView view;
  protected static VulnerabilityReporter reporter;

  public static void main(String[] args) {
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

    // Retrieve the specified class from the project.
    SootClass sootClass = view.getClass(classType).get();

    // Retrieve method
    view.getMethod(methodSignature);

    // Retrieve the specified method from the class.
    SootMethod sootMethod = sootClass.getMethod(methodSignature.getSubSignature()).get();

    JavaSootMethod method = (JavaSootMethod) (sootMethod);
    ArrayListAnalysis analysis = new ArrayListAnalysis(method, reporter);
    analysis.execute();
  }
}