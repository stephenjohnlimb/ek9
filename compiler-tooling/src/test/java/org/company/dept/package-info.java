/**
 * This package info annotation mechanism is used to enable the EK9 tooling
 * to interrogate a jar/package and then find all the constructs within.
 * It can then forumulate the EK9 'extern' signatures of the module and all the constructs
 * and their methods/functions and properties.
 */
@Ek9Module("defines extern module org.company.dept")
package org.company.dept;

import org.ek9tooling.Ek9Module;
