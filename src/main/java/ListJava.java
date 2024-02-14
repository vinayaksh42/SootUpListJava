import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

public class ListJava {
    public ListJava() {
    }

    public static void main(String[] var0) {
        List<String> myList = new ArrayList<>();
        myList.add("Hello");
        myList.add("World");

        // Scenario 1: Accessing list without checking if it's empty
        System.out.println(myList.get(0));

        // Scenario 2: Accessing list after checking it's not empty
        if (!myList.isEmpty()) {
            System.out.println(myList.get(1));
        }

        // Scenario 3: Potentially problematic loop (if list were empty)
        for (String item : myList) {
            System.out.println(item);
        }

        // Scenario 4: Safely iterating over the list
        if (!myList.isEmpty()) {
            for (String item : myList) {
                System.out.println(item);
            }
        }

        // Scenario 5: Clearing the list and then trying to access it
        myList.clear();
        if (!myList.isEmpty()) {
            System.out.println(myList.get(0));
        }

        // Scenario 6:
        List<String> myList2 = new ArrayList<>();
        if (!myList2.isEmpty()) {
            System.out.println(myList2.get(0));
        }

        // Scenario 7:
        System.out.println(myList2.get(1));

        // Scenario 8:
        Iterator<String> iterator = myList.iterator();
        while (iterator.hasNext()) {
            String item = iterator.next();
            System.out.println(item);
        }

        // Scenario 9:
        if (!myList.isEmpty()) {
            Iterator<String> iterator1 = myList.iterator();
            while (iterator1.hasNext()) {
                String item = iterator1.next();
                System.out.println(item);
            }
        }

    }
}
