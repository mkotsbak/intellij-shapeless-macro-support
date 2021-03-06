package com.jacoby6000.shapeless.injector

import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.SyntheticMembersInjector

/**
  * @author Jacob Barber
  * @since  5/12/15
  */
class ShapelessApplyProductInjector extends SyntheticMembersInjector {
  override def injectFunctions(source: ScTypeDefinition): Seq[String] = {
    source.parents.find(_.getText == "shapeless.ProductArgs") match {
      case Some(_) =>
        val productMethod = "(.*)Product".r
        source.members.flatMap( method =>
          method.getName match {
            case productMethod(productName) => Seq( (method, productName) )
            case _ => Seq.empty
          }
        ).flatMap {
          case (method: ScFunctionDefinition, productName) if method.body.isDefined =>
            val expr = method.body.get
            val methods = (0 to 100) map { n =>
              val typeParams = (0 to n).map("T" + _)
              val regParams = (0 to n).map("v" + _)

              val typeParamsString = if (typeParams.isEmpty) "" else typeParams.mkString("[", ",", "]")
              val regParamsString = typeParams.zip(typeParams).map { case (name, tpe) => s"$name: $tpe" }.mkString("(",",",")")

              val returnType = expr.getType().getOrNothing.canonicalText
              val signature = s"def $productName$typeParamsString$regParamsString: $returnType"

              val body = s"${productName}Product(${regParams.mkString(" :: ")} :: HNil)"

              s"$signature = $body"
            }

            methods foreach println
            methods.toSeq
          case _ => Seq.empty
        }

      case None => Seq.empty
    }
  }
}
