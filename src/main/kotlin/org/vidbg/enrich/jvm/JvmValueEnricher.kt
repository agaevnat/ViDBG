package org.vidbg.enrich.jvm

import com.intellij.debugger.engine.JavaDebugProcess
import com.intellij.debugger.engine.JavaValue
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiModifier
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.XStackFrame
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

    override fun typeOf(value: XValue): String? =
        (value as? JavaValue)?.descriptor?.type?.name()

    override fun globalsOf(project: Project, frame: XStackFrame, onDone: (List<Pair<String, XValue>>) -> Unit) {
        val position = frame.sourcePosition
        val evaluator = frame.evaluator

        if (position == null || evaluator == null) {
            onDone(emptyList())
            return
        }

        // Every static field declared anywhere in this source file — not just the current
        // method's own class — evaluated the same way a user would type it in Evaluate Expression.
        // PSI access needs a read action; sessionPaused (our caller's caller) runs on the
        // debugger's own command thread, not the EDT, so nothing here can assume otherwise.
        val targets = ReadAction.computeBlocking<List<Pair<String, String>>, RuntimeException> {
            val psiFile = PsiManager.getInstance(project).findFile(position.file) as? PsiJavaFile
                ?: return@computeBlocking emptyList()
            // Java's implicit-class syntax (JEP 445/477) has no name the evaluator can resolve
            // as a type — "Main.counter" fails with "cannot find local variable 'Main'" — so a
            // field belonging to the frame's own (possibly-implicit) class is evaluated
            // unqualified, exactly as if typed at that point in the source. Only fields from
            // other classes declared in the file need the "ClassName." prefix.
            val currentClass = psiFile.findElementAt(position.offset)?.let {
                PsiTreeUtil.getParentOfType(it, PsiClass::class.java)
            }

            psiFile.classes.flatMap { it.andInnerClasses() }.flatMap { psiClass ->
                val isCurrent = psiClass == currentClass
                val qualifiedName = psiClass.qualifiedName
                if (!isCurrent && qualifiedName == null) return@flatMap emptyList()

                psiClass.fields
                    .filter { it.hasModifierProperty(PsiModifier.STATIC) }
                    .map { field ->
                        val expression = if (isCurrent) field.name else "$qualifiedName.${field.name}"
                        expression to "${psiClass.name}.${field.name}"
                    }
            }
        }

        if (targets.isEmpty()) {
            onDone(emptyList())
            return
        }

        val results = arrayOfNulls<Pair<String, XValue>>(targets.size)
        var remaining = targets.size

        targets.forEachIndexed { index, (expression, displayName) ->
            evaluator.evaluate(expression, object : XDebuggerEvaluator.XEvaluationCallback {
                override fun evaluated(value: XValue) {
                    results[index] = displayName to value
                    if (--remaining == 0) onDone(results.filterNotNull())
                }

                override fun errorOccurred(errorMessage: String) {
                    if (--remaining == 0) onDone(results.filterNotNull())
                }
            }, position)
        }
    }
}

private fun PsiClass.andInnerClasses(): List<PsiClass> = listOf(this) + innerClasses.flatMap { it.andInnerClasses() }
