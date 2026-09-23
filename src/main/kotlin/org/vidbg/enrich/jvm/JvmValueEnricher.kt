package org.vidbg.enrich.jvm

import com.intellij.debugger.engine.JavaDebugProcess
import com.intellij.debugger.engine.JavaValue
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.frame.XValue
import com.sun.jdi.ObjectReference
import org.vidbg.enrich.ObjectIdentity
import org.vidbg.enrich.ValueEnricher

class JvmValueEnricher : ValueEnricher {
    override fun isApplicable(process: XDebugProcess): Boolean =
        process is JavaDebugProcess

    override fun identityOf(value: XValue): ObjectIdentity? {
        val jdiValue = (value as? JavaValue)?.descriptor?.value as? ObjectReference ?: return null
        return ObjectIdentity(jdiValue.uniqueID(), jdiValue.referenceType().name())
    }
}
