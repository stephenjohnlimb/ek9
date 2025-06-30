/**
 * This package info annotation mechanism is used to enable the EK9 tooling
 * to interrogate the jar for classes that must be exposed as the standard library for ek9.
 * <p>
 *   Note that many of the exposed operators start with an underscore, or in some cases are
 *   words like 'clear' etc. So for Java this is normally considered bad naming, but I don't want any
 *   method naming conflicts. So, in EK9 the underscore for naming is prohibited, which means I can create
 *   any method name with underscores and guarantee no collisions.
 * </p>
 */
@Ek9Module("defines extern module org.ek9.lang")
@Ek9Package("""
    publicAccess <- true
    version <- 0.0.1-0
    description <- "Core EK9 standard library (lang)"
    license <- "MIT"
    """)
package org.ek9.lang;

import org.ek9tooling.Ek9Module;
import org.ek9tooling.Ek9Package;
