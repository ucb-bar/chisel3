---
layout: docs
title:  "DataView"
section: "chisel3"
---

# DataView

_New in Chisel 3.5_

```scala mdoc:invisible
import chisel3._
import chisel3.stage.ChiselStage.emitVerilog
```

## Introduction

DataView is a mechanism for "viewing" Scala objects as a subtype of `chisel3.Data`.
Often, this is useful for viewing one subtype of `chisel3.Data`, as another.
One can think about a `DataView` as a mapping from a _Target_ type `T` to a _View_ type `V`.
This is similar to a cast (eg. `.asTypeOf`) with a few differences:
1. Views are mutable—connections to the view will occur on the target
2. Whereas casts are _structural_ (a reinterpretation of the underling bits), a DataView is a customizable mapping
3. Views can be _partial_, not every field in the target must be included in the mapping

## A Motivating Example (AXI4)

[AXI4](https://en.wikipedia.org/wiki/Advanced_eXtensible_Interface) is a common interface in digital
design.
A typical Verilog peripheral using AXI4 will define a write channel as something like:
```verilog
module my_module(
  // Write Channel
  input        AXI_AWVALID,
  output       AXI_AWREADY,
  input [3:0]  AXI_AWID,
  input [19:0] AXI_AWADDR,
  input [1:0]  AXI_AWLEN,
  input [1:0]  AXI_AWSIZE,
  // ...
);
```

This would correspond to the following Chisel Bundle:

```scala mdoc
class VerilogAXIBundle extends Bundle {
  val AWVALID = Output(Bool())
  val AWREADY = Input(Bool())
  val AWID = Output(UInt(4.W))
  val AWADDR = Output(UInt(20.W))
  val AWLEN = Output(UInt(2.W))
  val AWSIZE = Output(UInt(2.W))
  // The rest of AW and other AXI channels here
}

// Instantiated as
class my_module extends RawModule {
  val AXI = IO(new VerilogAXIBundle)
}
```

Expressing something that matches a standard Verilog interface is important when instantiating Verilog
modules in a Chisel design as `BlackBoxes`.
Generally though, Chisel developers prefer to use composition via utilities like `Decoupled` rather
than a flat handling of `ready` and `valid` as in the above.
A more "Chisel-y" implementation of this interface might look like:

```scala mdoc
// Note that both the AW and AR channels look similar and could use the same Bundle definition
class AXIAddressChannel extends Bundle {
  val id = UInt(4.W)
  val addr = UInt(20.W)
  val len = UInt(2.W)
  val size = UInt(2.W)
  // ...
}
import chisel3.util.Decoupled
// We can compose the various AXI channels together
class AXIBundle extends Bundle {
  val aw = Decoupled(new AXIAddressChannel)
  // val ar = new AXIAddressChannel
  // ... Other channels here ...
}
// Instantiated as
class MyModule extends RawModule {
  val axi = IO(new AXIBundle)
}
```

Of course, this would result in very different looking Verilog:

```scala mdoc:verilog
emitVerilog(new MyModule {
  override def desiredName = "MyModule"
  axi := DontCare // Just to generate Verilog in this stub
})
```

So how can we use our more structured types while maintaining expected Verilog interfaces?
Meet DataView:

```scala mdoc
import chisel3.experimental.dataview._

// We recommend putting DataViews in a companion object of one of the involved types
object AXIBundle {
  // Don't be afraid of the use of implicits, we will discuss this pattern in more detail later
  implicit val axiView = DataView[VerilogAXIBundle, AXIBundle](
    _.AWVALID -> _.aw.valid,
    _.AWREADY -> _.aw.ready,
    _.AWID -> _.aw.bits.id,
    _.AWADDR -> _.aw.bits.addr,
    _.AWLEN -> _.aw.bits.len,
    _.AWSIZE -> _.aw.bits.size,
    // ...
  )
}
```

This `DataView` is a _bidirectional_ mapping between our flat, Verilog-style AXI Bundle and our more
compositional, Chisel-style AXI Bundle.
It allows us to define our ports to match the expected Verilog interface, while manipulating it as if
it were the more structured type:

```scala mdoc
class AXIStub extends RawModule {
  val AXI = IO(new VerilogAXIBundle)
  val view = AXI.viewAs(new AXIBundle)

  // We can now manipulate `AXI` via `view`
  view.aw.bits := 0.U.asTypeOf(new AXIAddressChannel) // zero everything out by default
  view.aw.valid := true.B
  when (view.aw.ready) {
    view.aw.bits.id := 5.U
    view.aw.bits.addr := 1234.U
    // We can still manipulate AXI as well
    AXI.AWLEN := 1.U
  }
}
```

This will generate Verilog that matches the standard naming convention:

```scala mdoc:verilog
emitVerilog(new AXIStub)
```

## Other Use Cases

## Advanced Details

### Type Classes in Scala

#### Implicit Resolution

### DataProduct