// SPDX-License-Identifier: Apache-2.0

package chisel3.experimental.dataview

import chisel3.{Data, RawModule, getRecursiveFields}

import scala.annotation.implicitNotFound

/** Typeclass interface for getting elements of type [[Data]]
  *
  * This is needed for validating [[DataView]]s targeting type `A`.
  * Can be thought of as "can be the Target of a DataView".
  * @tparam A Type that has elements of type [[Data]]
  */
@implicitNotFound("Could not find implicit value for DataProduct[${A}].\nPlease see <docs link>")
trait DataProduct[-A] {
  /** Provides [[Data]] elements within some containing object
    *
    * @param a Containing object
    * @param path Hierarchical path to current signal (for error reporting)
    * @return Data elements and associated String paths (Strings for error reporting only!)
    */
  def dataIterator(a: A, path: String): Iterator[(Data, String)]

  /** Test whether the given element exists within A
    * @note Implementers may want to override if iterating on all `Data` is expensive for `A` and `A`
    *       will primarily be used in `PartialDataViews`
    * @param a Containing object
    * @param elt The element to test
    * @return True if `elt` is contained in `a` as determined by a `==` test
    */
  def contains(a: A, elt: Data): Boolean = dataIterator(a, "").exists(_._1 == elt)
}

object DataProduct {
  implicit val dataDataProduct: DataProduct[Data] = new DataProduct[Data] {
    def dataIterator(a: Data, path: String): Iterator[(Data, String)] =
      getRecursiveFields(a, path).iterator
  }

  implicit val userModuleDataProduct: DataProduct[RawModule] = new DataProduct[RawModule] {
    def dataIterator(a: RawModule, path: String): Iterator[(Data, String)] = ???
    // Overridden for performance
    override def contains(a: RawModule, elt: Data): Boolean = {
      elt._id > a._id && elt._id <= a._lastId
    }
  }
}