package world.gregs.intellij.plugins.flow

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.psi.*
import com.intellij.psi.PsiKeyword.FALSE
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import com.siyeh.ig.style.ControlFlowStatementVisitorBase
import com.siyeh.ig.style.SingleStatementInBlockInspection
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NotNull
import world.gregs.intellij.plugins.DeobfuscateToolBundle

class PointlessDoWhileStatementInspection : SingleStatementInBlockInspection() {

    override fun getDisplayName(): String {
        return DeobfuscateToolBundle.message("pointless.do.while.statement.display.name")
    }

    public override fun buildErrorString(vararg infos: Any): String {
        return DeobfuscateToolBundle.message("pointless.do.while.statement.problem.descriptor")
    }

    override fun isEnabledByDefault(): Boolean {
        return true
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : ControlFlowStatementVisitorBase() {

            override fun visitLabeledStatement(statement: PsiLabeledStatement) {
                super.visitLabeledStatement(statement)
                val child = statement.lastChild
                if (child is PsiDoWhileStatement && child.condition?.text.equals(FALSE)) {
                    registerStatementError(statement)
                }
            }

            override fun visitDoWhileStatement(statement: PsiDoWhileStatement) {
                super.visitDoWhileStatement(statement)
                if (statement.parent !is PsiLabeledStatement && statement.condition?.text.equals(FALSE)) {
                    registerStatementError(statement)
                }
            }

            override fun isApplicable(body: PsiStatement?): Boolean {
                return false
            }

            override fun getOmittedBodyBounds(body: PsiStatement?): Pair<PsiElement, PsiElement>? {
                return null
            }

        }
    }

    override fun buildFix(vararg infos: Any?): InspectionGadgetsFix? {
        return object : InspectionGadgetsFix() {
            @Nls
            @NotNull
            override fun getName(): String {
                return DeobfuscateToolBundle.message("pointless.do.while.statement.quickfix")
            }

            @Nls
            @NotNull
            override fun getFamilyName(): String {
                return DeobfuscateToolBundle.message("pointless.do.while.statement.family.quickfix")
            }

            fun recursive(statement: PsiElement, list: MutableList<PsiBreakStatement>) {
                statement.children.forEach {
                    if (it is PsiBreakStatement) {
                        list.add(it)
                    }
                    recursive(it, list)
                }
            }

            override fun doFix(project: Project, descriptor: ProblemDescriptor) {
                val statement: PsiStatement = PsiTreeUtil.getNonStrictParentOfType(descriptor.startElement, PsiLabeledStatement::class.java)
                        ?: PsiTreeUtil.getNonStrictParentOfType(descriptor.startElement, PsiDoWhileStatement::class.java)
                        ?: return
                val labeled = statement is PsiLabeledStatement
                val loop = (if (labeled) statement.lastChild else statement) as PsiDoWhileStatement
                val label = (statement as? PsiLabeledStatement)?.labelIdentifier
                var parents = statement.parent.children

                //TODO insert break with label at end of loop body if doesn't exist?

                // Find block after
                val preceding = parents.copyOfRange(parents.indexOf(statement) + 2, parents.size - 2).reversed()// +/- 2 to ignore spaces
                // Find all breaks using outer loop
                var breaks = mutableListOf<PsiBreakStatement>()
                recursive(loop, breaks)
                breaks = if(label != null) breaks.filter { it.children.any { child -> (child as? PsiReferenceExpression)?.text == label.text } }.toMutableList() else breaks.filter { it.findExitedElement() == loop }.toMutableList()

                // Replace all occurrences with block after
                breaks.forEach { b ->
                    val parent = b.parent
                    preceding.forEach { before ->
                        parent.addAfter(before, b)
                    }
                    b.delete()
                }

                // Move body outside of loop
                val bodyParts = (loop.body as? PsiBlockStatement)?.codeBlock?.children!!
                val scope = statement.parent
                var hack = 0
                bodyParts.reversed().forEachIndexed { index, psiElement ->
                    if (index != 0 && index != bodyParts.size - 1) {
                        scope.addAfter(psiElement, statement)
                        hack++
                    }
                }
                // Remove body parenthesis
                bodyParts.first().delete()
                bodyParts.last().delete()
                // Remove do while loop
                parents = statement.parent.children
                statement.parent.deleteChildRange(parents[parents.indexOf(statement) + hack + 1], parents[parents.size - 3])// -3 for parenthesis and space either side
                statement.delete()
                CodeStyleManager.getInstance(project).reformat(scope)
            }
        }
    }
}