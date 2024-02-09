## Problem Statement:
Detecting if ArrayList is being used with or without emptiness check.
For example:
1. This is an unsafe usage of ArrayList:
```java
System.out.println(myList.get(0));
```
2. This is a safe usage of ArrayList:
```java
if (!myList.isEmpty()) {
  System.out.println(myList.get(1));
}
```

Any list usage that occurs after the emptiness check of the ArrayList is marked as Safe usage and any usage without the emptiness check is marked as unsafe usage.

## Solution explanation:
### Creating a Jimple representation of code using [SootUp](https://soot-oss.github.io/SootUp/latest/):
```java
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
```
In the above code:
1. The location of Binary files is specified 
2. The specific class and method are retrieved
3. the specified method from the class is stored in Jimple representation

### Analysing the Jimple representation for ArrayList usage:
```java
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
```
In the above code:
1. Iteration occurs over Stmt of the Jimple representation.
2. ArrayList variable name is stored for future reference, it is observed that all ArrayList declarations contain `new java.util.ArrayList`. Shown in the below output:
```zsh
[$stack4, new java.util.ArrayList]
[$stack4, specialinvoke $stack4.<java.util.ArrayList: void <init>()>()]
[myList, $stack4]
```
3. The stored ArrayList variable name is used to check for usage of ArrayList.
4. The ArrayList usage occurs in the following way in Jimple representation:
```zsh
// get method on ArrayList
[$stack6, interfaceinvoke myList.<java.util.List: java.lang.Object get(int)>(0), 0, myList]

// isEmpty check on ArrayList
[$stack8, interfaceinvoke myList.<java.util.List: boolean isEmpty()>(), myList]

// loop iteration on ArrayList
[l3, interfaceinvoke myList.<java.util.List: java.util.Iterator iterator()>(), myList]
```
5. Track of the different ArrayList is kept and the usage of ArrayList.
6. If a `get` or `iterator` is used on ArrayList without a preceding `isEmpty` check it is marked as unsafe usage.
7. Finally, the number of unsafe usage of ArrayList is reported back.
