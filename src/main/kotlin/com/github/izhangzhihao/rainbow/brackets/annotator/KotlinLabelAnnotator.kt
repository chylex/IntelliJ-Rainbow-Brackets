package com.github.izhangzhihao.rainbow.brackets.annotator

import com.github.izhangzhihao.rainbow.brackets.RainbowHighlighter
import com.github.izhangzhihao.rainbow.brackets.RainbowInfo
import com.github.izhangzhihao.rainbow.brackets.settings.RainbowSettings
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLabelReferenceExpression
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import java.awt.Font

class KotlinLabelAnnotator : Annotator {
	override fun annotate(element: PsiElement, holder: AnnotationHolder) {
		if (!RainbowSettings.instance.isRainbowifyKotlinLabel) {
			return
		}
		
		val target: PsiElement
		var refElement: PsiElement?
		when (element) {
			is KtLabelReferenceExpression -> {
				if ((element.lastChild as? LeafPsiElement)?.elementType == KtTokens.AT) {
					return
				}
				
				target = element
				refElement = try {
					element.reference?.resolve()
				} catch (e: Throwable) {
					null
				}
				
				refElement = when (refElement) {
					is KtBlockExpression,
					is KtFunctionLiteral,
						          -> refElement
					
					is KtFunction -> refElement.lastChild.takeIf { it is KtBlockExpression }
					is KtClass    -> refElement.lastChild.takeIf { it is KtClassBody }
					is KtCallExpression,
					is KtLambdaExpression,
						          -> PsiTreeUtil.findChildOfType(refElement, KtFunctionLiteral::class.java)
					
					else          -> null
				}
			}
			
			is KtLabeledExpression        -> {
				target = element.firstChild.firstChild.takeIf { it is KtLabelReferenceExpression } ?: return
				refElement = element.lastChild.let {
					when (it) {
						is KtBlockExpression,
						is KtFunctionLiteral,
							 -> it
						
						is KtCallExpression,
						is KtLambdaExpression,
							 -> PsiTreeUtil.findChildOfType(it, KtFunctionLiteral::class.java)
						
						else -> null
					}
				} ?: return
			}
			
			else                          -> return
		}
		
		refElement
			.let { RainbowInfo.RAINBOW_INFO_KEY[it]?.color ?: RainbowHighlighter.DEFAULT_KOTLIN_LABEL_COLOR }
			.let {
				holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
					.range(target)
					.textAttributes(
						com.github.izhangzhihao.rainbow.brackets.util.create(
							"rainbow-kotlin-label",
							TextAttributes(it, null, null, EffectType.BOXED, Font.PLAIN)
						)
					)
					.create()
			}
	}
	
}
