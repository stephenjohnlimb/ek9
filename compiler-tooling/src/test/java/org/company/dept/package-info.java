/**
 * This package info annotation mechanism is used to enable the EK9 tooling
 * to interrogate a jar/package and then find all the constructs within.
 * It can then formulate the EK9 'extern' signatures of the module and all the constructs
 * and their methods/functions and properties.
 */
@Ek9Module("defines extern module org.company.dept")
@Ek9Package("""
    publicAccess <- true
    version <- 3.2.1-0
    description <- "Example of handy tools"
    license <- "MIT"
    
    tags <- [
      "general",
      "tools"
      ]""")
package org.company.dept;

import org.ek9tooling.Ek9Module;
import org.ek9tooling.Ek9Package;
