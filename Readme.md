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
    List<Value> listVariableNames = new ArrayList<>();
    List<Value> TempNames = new ArrayList<>();
    boolean isArraySafe = false;
    int arrayUnsafeUsage = 0;

    MethodSignature arrayListIterator = view.getIdentifierFactory().getMethodSignature("java.util.List",
        "iterator",
        "java.util.Iterator", Collections.emptyList());
    MethodSignature arrayListGet = view.getIdentifierFactory().getMethodSignature("java.util.List", "get",
        "java.lang.Object", Collections.singletonList("int"));
    MethodSignature arrayListIsEmpty = view.getIdentifierFactory().getMethodSignature("java.util.List", "isEmpty",
        "boolean", Collections.emptyList());
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
            if (listVariableNames.contains(use)) {
              if (!isArraySafe) {
                arrayUnsafeUsage++;
              } else {
                isArraySafe = false;
              }
            }
          }
        }

        // check if the invoke expression is a call to the isEmpty method of an
        // ArrayList
        if (invokeExpr.getMethodSignature().equals(arrayListIsEmpty)) {
          List<Value> InvokeExprUses = invokeExpr.getUses();
          for (Value use : InvokeExprUses) {
            if (listVariableNames.contains(use)) {
              isArraySafe = true;
            }
          }
        }

        // check if the invoke expression is a call to the iterator method of an
        // ArrayList
        if (invokeExpr.getMethodSignature().equals(arrayListIterator)) {
          List<Value> InvokeExprUses = invokeExpr.getUses();
          // iterate over the uses and check if it is part of the list of variables
          for (Value use : InvokeExprUses) {
            if (listVariableNames.contains(use)) {
              if (!isArraySafe) {
                arrayUnsafeUsage++;
              } else {
                isArraySafe = false;
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
            listVariableNames.add(defstmt.getLeftOp());
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
    System.out.println("Array Unsafe Usage: " + arrayUnsafeUsage);
```
In the above code:
1. Iteration occurs over Stmt of the Jimple representation.
2. We look for `JAssignStmt` as it signifies existence of a JAVA assignment statement. We then check if it is a `JNewExpr`, If it is we check if it is a `java.util.list` ReferenceType expression.
3. The stored ArrayList variable name is used to check for usage of ArrayList.
4. We check for `invokeExpr`, if it is a invoke expression we check if it is a get, isEmpty or iterator method on the ArrayList variable name stored earlier. We have different `if` condition for comparing and checking if it is of a particular `MethodSignature`
5. Track of the different ArrayList is kept and the usage of ArrayList.
6. If a `get` or `iterator` is used on ArrayList without a preceding `isEmpty` check it is marked as unsafe usage.
7. Finally, the number of unsafe usage of ArrayList is reported back:
```zsh
Array Unsafe Usage: 4
Array Safe Usage: 5
Unsafe ArrayList Usage:
$stack8 = interfaceinvoke myList.<java.util.List: java.lang.Object get(int)>(0)
myList2 = interfaceinvoke myList.<java.util.List: java.util.Iterator iterator()>()
$stack17 = interfaceinvoke myList2.<java.util.List: java.lang.Object get(int)>(1)
item = interfaceinvoke myList.<java.util.List: java.util.Iterator iterator()>()
```

## Jimple representation Example:
```bash
{
    java.lang.String[] var0;
    unknown $stack10, $stack11, $stack12, $stack13, $stack14, $stack15, $stack16, $stack17, $stack18, $stack19, $stack20, $stack21, $stack22, $stack23, $stack24, $stack25, $stack26, $stack27, $stack28, $stack29, $stack30, $stack31, $stack32, $stack33, $stack34, $stack35, $stack36, $stack37, $stack38, $stack39, $stack6, $stack7, $stack8, $stack9, item, iterator, myList;


    var0 := @parameter0: java.lang.String[];
    $stack6 = new java.util.ArrayList;
    specialinvoke $stack6.<java.util.ArrayList: void <init>()>();
    myList = $stack6;
    interfaceinvoke myList.<java.util.List: boolean add(java.lang.Object)>("Hello");
    interfaceinvoke myList.<java.util.List: boolean add(java.lang.Object)>("World");
    $stack7 = <java.lang.System: java.io.PrintStream out>;
    $stack8 = interfaceinvoke myList.<java.util.List: java.lang.Object get(int)>(0);
    $stack9 = (java.lang.String) $stack8;
    virtualinvoke $stack7.<java.io.PrintStream: void println(java.lang.String)>($stack9);
    $stack10 = interfaceinvoke myList.<java.util.List: boolean isEmpty()>();

    if $stack10 != 0 goto label01;
    $stack37 = <java.lang.System: java.io.PrintStream out>;
    $stack38 = interfaceinvoke myList.<java.util.List: java.lang.Object get(int)>(1);
    $stack39 = (java.lang.String) $stack38;
    virtualinvoke $stack37.<java.io.PrintStream: void println(java.lang.String)>($stack39);

  label01:
    iterator = interfaceinvoke myList.<java.util.List: java.util.Iterator iterator()>();

    goto label03;

  label02:
    $stack12 = interfaceinvoke iterator.<java.util.Iterator: java.lang.Object next()>();
    item = (java.lang.String) $stack12;
    $stack13 = <java.lang.System: java.io.PrintStream out>;
    virtualinvoke $stack13.<java.io.PrintStream: void println(java.lang.String)>(item);

  label03:
    $stack11 = interfaceinvoke iterator.<java.util.Iterator: boolean hasNext()>();

    if $stack11 != 0 goto label02;
    $stack14 = interfaceinvoke myList.<java.util.List: boolean isEmpty()>();

    if $stack14 != 0 goto label06;
    iterator = interfaceinvoke myList.<java.util.List: java.util.Iterator iterator()>();

    goto label05;

  label04:
    $stack35 = interfaceinvoke iterator.<java.util.Iterator: java.lang.Object next()>();
    item = (java.lang.String) $stack35;
    $stack36 = <java.lang.System: java.io.PrintStream out>;
    virtualinvoke $stack36.<java.io.PrintStream: void println(java.lang.String)>(item);

  label05:
    $stack34 = interfaceinvoke iterator.<java.util.Iterator: boolean hasNext()>();

    if $stack34 != 0 goto label04;

  label06:
    interfaceinvoke myList.<java.util.List: void clear()>();
    $stack15 = interfaceinvoke myList.<java.util.List: boolean isEmpty()>();

    if $stack15 != 0 goto label07;
    $stack31 = <java.lang.System: java.io.PrintStream out>;
    $stack32 = interfaceinvoke myList.<java.util.List: java.lang.Object get(int)>(0);
    $stack33 = (java.lang.String) $stack32;
    virtualinvoke $stack31.<java.io.PrintStream: void println(java.lang.String)>($stack33);

  label07:
    $stack16 = new java.util.ArrayList;
    specialinvoke $stack16.<java.util.ArrayList: void <init>()>();
    item = $stack16;
    $stack17 = interfaceinvoke item.<java.util.List: boolean isEmpty()>();

    if $stack17 != 0 goto label08;
    $stack28 = <java.lang.System: java.io.PrintStream out>;
    $stack29 = interfaceinvoke item.<java.util.List: java.lang.Object get(int)>(0);
    $stack30 = (java.lang.String) $stack29;
    virtualinvoke $stack28.<java.io.PrintStream: void println(java.lang.String)>($stack30);

  label08:
    $stack18 = <java.lang.System: java.io.PrintStream out>;
    $stack19 = interfaceinvoke item.<java.util.List: java.lang.Object get(int)>(1);
    $stack20 = (java.lang.String) $stack19;
    virtualinvoke $stack18.<java.io.PrintStream: void println(java.lang.String)>($stack20);
    iterator = interfaceinvoke myList.<java.util.List: java.util.Iterator iterator()>();

    goto label10;

  label09:
    $stack22 = interfaceinvoke iterator.<java.util.Iterator: java.lang.Object next()>();
    item = (java.lang.String) $stack22;
    $stack23 = <java.lang.System: java.io.PrintStream out>;
    virtualinvoke $stack23.<java.io.PrintStream: void println(java.lang.String)>(item);

  label10:
    $stack21 = interfaceinvoke iterator.<java.util.Iterator: boolean hasNext()>();

    if $stack21 != 0 goto label09;
    $stack24 = interfaceinvoke myList.<java.util.List: boolean isEmpty()>();

    if $stack24 != 0 goto label13;
    item = interfaceinvoke myList.<java.util.List: java.util.Iterator iterator()>();

    goto label12;

  label11:
    $stack26 = interfaceinvoke item.<java.util.Iterator: java.lang.Object next()>();
    item = (java.lang.String) $stack26;
    $stack27 = <java.lang.System: java.io.PrintStream out>;
    virtualinvoke $stack27.<java.io.PrintStream: void println(java.lang.String)>(item);

  label12:
    $stack25 = interfaceinvoke item.<java.util.Iterator: boolean hasNext()>();

    if $stack25 != 0 goto label11;

  label13:
    return;
}
```
## Jimple representation (Stmt)
```bash
var0 := @parameter0: java.lang.String[]
$stack6 = new java.util.ArrayList
specialinvoke $stack6.<java.util.ArrayList: void <init>()>()
myList = $stack6
interfaceinvoke myList.<java.util.List: boolean add(java.lang.Object)>("Hello")
interfaceinvoke myList.<java.util.List: boolean add(java.lang.Object)>("World")
$stack7 = <java.lang.System: java.io.PrintStream out>
$stack8 = interfaceinvoke myList.<java.util.List: java.lang.Object get(int)>(0)
$stack9 = (java.lang.String) $stack8
virtualinvoke $stack7.<java.io.PrintStream: void println(java.lang.String)>($stack9)
$stack10 = interfaceinvoke myList.<java.util.List: boolean isEmpty()>()
if $stack10 != 0
$stack37 = <java.lang.System: java.io.PrintStream out>
$stack38 = interfaceinvoke myList.<java.util.List: java.lang.Object get(int)>(1)
$stack39 = (java.lang.String) $stack38
virtualinvoke $stack37.<java.io.PrintStream: void println(java.lang.String)>($stack39)
iterator = interfaceinvoke myList.<java.util.List: java.util.Iterator iterator()>()
goto
$stack12 = interfaceinvoke iterator.<java.util.Iterator: java.lang.Object next()>()
item = (java.lang.String) $stack12
$stack13 = <java.lang.System: java.io.PrintStream out>
virtualinvoke $stack13.<java.io.PrintStream: void println(java.lang.String)>(item)
$stack11 = interfaceinvoke iterator.<java.util.Iterator: boolean hasNext()>()
if $stack11 != 0
$stack14 = interfaceinvoke myList.<java.util.List: boolean isEmpty()>()
if $stack14 != 0
iterator = interfaceinvoke myList.<java.util.List: java.util.Iterator iterator()>()
goto
$stack35 = interfaceinvoke iterator.<java.util.Iterator: java.lang.Object next()>()
item = (java.lang.String) $stack35
$stack36 = <java.lang.System: java.io.PrintStream out>
virtualinvoke $stack36.<java.io.PrintStream: void println(java.lang.String)>(item)
$stack34 = interfaceinvoke iterator.<java.util.Iterator: boolean hasNext()>()
if $stack34 != 0
interfaceinvoke myList.<java.util.List: void clear()>()
$stack15 = interfaceinvoke myList.<java.util.List: boolean isEmpty()>()
if $stack15 != 0
$stack31 = <java.lang.System: java.io.PrintStream out>
$stack32 = interfaceinvoke myList.<java.util.List: java.lang.Object get(int)>(0)
$stack33 = (java.lang.String) $stack32
virtualinvoke $stack31.<java.io.PrintStream: void println(java.lang.String)>($stack33)
$stack16 = new java.util.ArrayList
specialinvoke $stack16.<java.util.ArrayList: void <init>()>()
item = $stack16
$stack17 = interfaceinvoke item.<java.util.List: boolean isEmpty()>()
if $stack17 != 0
$stack28 = <java.lang.System: java.io.PrintStream out>
$stack29 = interfaceinvoke item.<java.util.List: java.lang.Object get(int)>(0)
$stack30 = (java.lang.String) $stack29
virtualinvoke $stack28.<java.io.PrintStream: void println(java.lang.String)>($stack30)
$stack18 = <java.lang.System: java.io.PrintStream out>
$stack19 = interfaceinvoke item.<java.util.List: java.lang.Object get(int)>(1)
$stack20 = (java.lang.String) $stack19
virtualinvoke $stack18.<java.io.PrintStream: void println(java.lang.String)>($stack20)
iterator = interfaceinvoke myList.<java.util.List: java.util.Iterator iterator()>()
goto
$stack22 = interfaceinvoke iterator.<java.util.Iterator: java.lang.Object next()>()
item = (java.lang.String) $stack22
$stack23 = <java.lang.System: java.io.PrintStream out>
virtualinvoke $stack23.<java.io.PrintStream: void println(java.lang.String)>(item)
$stack21 = interfaceinvoke iterator.<java.util.Iterator: boolean hasNext()>()
if $stack21 != 0
$stack24 = interfaceinvoke myList.<java.util.List: boolean isEmpty()>()
if $stack24 != 0
item = interfaceinvoke myList.<java.util.List: java.util.Iterator iterator()>()
goto
$stack26 = interfaceinvoke item.<java.util.Iterator: java.lang.Object next()>()
item = (java.lang.String) $stack26
$stack27 = <java.lang.System: java.io.PrintStream out>
virtualinvoke $stack27.<java.io.PrintStream: void println(java.lang.String)>(item)
$stack25 = interfaceinvoke item.<java.util.Iterator: boolean hasNext()>()
if $stack25 != 0
return
```
