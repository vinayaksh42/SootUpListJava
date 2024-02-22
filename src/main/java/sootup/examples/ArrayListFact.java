package sootup.examples;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import sootup.core.jimple.basic.Value;

/**
 * The Class ArrayListStateFact records the state of an ArrayList object.
 */
public class ArrayListFact {

  public enum ArrayListState {
    Unchecked, Checked
  }

  /** The aliases point to the same ArrayList object. */
  private final Set<Value> aliases;

  /** The state of the ArrayList object. */
  private ArrayListState state;

  public ArrayListFact(@Nonnull ArrayListFact asf) {
    this(new HashSet<>(asf.aliases), asf.state);
  }

  public ArrayListFact(@Nonnull ArrayListState state) {
    this(new HashSet<>(), state);
  }

  public ArrayListFact(@Nonnull Set<Value> aliases, @Nonnull ArrayListState state) {
    this.aliases = aliases;
    this.state = state;
  }

  public void updateState(@Nonnull ArrayListState state) {
    this.state = state;
  }

  public void addAlias(@Nonnull Value alias) {
    this.aliases.add(alias);
  }

  public boolean isChecked() {
    return state == ArrayListState.Checked;
  }

  public boolean containsAlias(@Nonnull Value value) {
    return aliases.stream().anyMatch(alias -> alias.equivTo(value));
  }

  @Nonnull
  public ArrayListState getState() {
    return this.state;
  }

  @Override
  @Nonnull
  public String toString() {
    return "(" + aliases + ", " + state + ")";
  }
}