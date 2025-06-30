package org.company.dept;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * This is an example of a parameterised generic function.
 * Use org.ek9lang.compiler.support.DecoratedName to generate the name.
 * i.e. With arguments like this.
 * <pre>
 *  EventHandler org.company.dept::EventHandler org.company.dept::StringExample
 * </pre>
 * <p>
 *   When developing these Java classes by hand it is important to effectively 'override'
 *   the methods in the generic class with the appropriate new types.
 *   For example ._call(Object event) becomes .call(StringExample event)
 * </p>
 */
@Ek9ParameterisedType("""
    EventHandler of StringExample""")
public class _EventHandler_11E6531A327FE91D99A367865892C73DC765E025F22152CDCA60F75DCE18006C {

  private final EventHandler implementation;

  public _EventHandler_11E6531A327FE91D99A367865892C73DC765E025F22152CDCA60F75DCE18006C() {
    //Default constructor - not exposed in any way to EK9
    this.implementation = new EventHandler();
  }

  //Note that the EK9 compiler will call this method.
  public void _call(StringExample event) {
    //But it would be quite possible to implement this in anyway the Java developer sees fit
    //as long as they have this method and apply the logic that was defined in the EventHandler
    //_call method.
    implementation._call(event);
  }
}