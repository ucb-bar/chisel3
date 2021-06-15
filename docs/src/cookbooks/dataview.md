---
layout: docs
title:  "DataView Cookbook"
section: "chisel3"
---

# DataView Cookbook

* [How do I connect a subset of Bundle fields?](#how-do-i-connect-a-subset-of-bundle-fields)
    * [How do I view a Bundle as a parent type (superclass)?](#how-do-i-view-a-bundle-as-a-parent-type-superclass)
    * [How do I view a Bundle as a parent type when the parent type is abstract (like a trait)?](#how-do-i-view-a-bundle-as-a-parent-type-when-the-parent-type-is-abstract-like-a-trait)

## How do I connect a subset of Bundle fields?

Chisel 3 requires types to match exactly for connections.
DataView provides a mechanism for "viewing" one `Bundle` object as if it were the type of another,
which allows them to be connected.

### How do I view a Bundle as a parent type (superclass)?

For viewing `Bundles` as the type of the parent, it is as simple as using `viewAs` and providing a
template object of the parent type:

```scala mdoc:silent:reset
import chisel3._
import chisel3.experimental.dataview._

class Foo extends Bundle {
  val foo = UInt(8.W)
}
class Bar extends Foo {
  val bar = UInt(8.W)
}
class MyModule extends Module {
  val foo = IO(Input(new Foo))
  val bar = IO(Output(new Bar))
  bar.viewAs(new Foo) := foo // bar.foo := foo.foo
  bar.bar := 123.U           // all fields need to be connected
}
```
```scala mdoc:verilog
chisel3.stage.ChiselStage.emitVerilog(new MyModule)
```

### How do I view a Bundle as a parent type when the parent type is abstract (like a trait)?

When trying to view a Bundle as a parent `trait`, you may see an error like the following:

```scala mdoc:fail:reset
import chisel3._
import chisel3.experimental.dataview._

trait Super extends Bundle {
  def bitwidth: Int
  val a = UInt(bitwidth.W)
}
class Foo(val bitwidth: Int) extends Super {
  val foo = UInt(8.W)
}
class Bar(val bitwidth: Int) extends Super {
  val bar = UInt(8.W)
}
class MyModule extends Module {
  val foo = IO(Input(new Foo(8)))
  val bar = IO(Output(new Bar(8)))
  bar.viewAs(new Super) := foo.viewAs(new Super)
}
```

The problem is that `viewAs` requires an object to use as a type template (so that it can be cloned),
but `traits` are abstract and cannot be instantiated.
The solution is to create an instance of an _anonymous class_ and use that object as the argument to `viewAs`.
We can do this like so:

```scala mdoc:silent:reset
import chisel3._
import chisel3.experimental.dataview._

trait Super extends Bundle {
  def bitwidth: Int
  val a = UInt(bitwidth.W)
}
class Foo(val bitwidth: Int) extends Super {
  val foo = UInt(8.W)
}
class Bar(val bitwidth: Int) extends Super {
  val bar = UInt(8.W)
}
class MyModule extends Module {
  val foo = IO(Input(new Foo(8)))
  val bar = IO(Output(new Bar(8)))
  val tpe = new Super { // Adding curly braces creates an anonymous class
    def bitwidth = 8 // We must implement any abstract methods
  }
  bar.viewAs(new Super) := foo.viewAs(new Super)
}
```
By adding curly braces after the name of the trait, we're telling Scala to create a new concrete
subclass of the trait, and create an instance of it.
As indicated in the comment, abstract methods must still be implemented.
This is the same that happens when one writes `new Bundle {}`,
the curly braces create a new concrete subclass; however, because `Bundle` has no abstract methods,
the contents of the body can be empty.