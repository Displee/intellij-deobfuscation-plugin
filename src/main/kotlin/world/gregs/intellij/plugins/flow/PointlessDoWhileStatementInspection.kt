package world.gregs.intellij.plugins.flow

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.psi.PsiDoWhileStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiKeyword.FALSE
import com.intellij.psi.PsiStatement
import com.intellij.psi.util.PsiUtil
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

            override fun visitDoWhileStatement(statement: PsiDoWhileStatement) {
                val condition = PsiUtil.skipParenthesizedExprDown(statement.condition)
                //Only loops with false conditions are be pointless
                if(condition?.text.equals(FALSE)) {
                    println("${statement.condition} ${statement.whileKeyword} ${statement.lParenth} ${statement.rParenth}")
                    println(statement.body)
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

            override fun doFix(project: Project, descriptor: ProblemDescriptor) {
            }
        }
    }
}