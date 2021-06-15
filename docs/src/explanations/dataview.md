---
layout: docs
title:  "DataView"
section: "chisel3"
---

# DataView

_New in Chisel 3.5_

```scala mdoc:invisible
import chisel3._
```

## Introduction

DataView is a mechanism for "viewing" Scala objects as a subtype of `chisel3.Data`.
Often, this is useful for viewing one subtype of `chisel3.Data`, as another.

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
class AWBundle extends Bundle {
  val AWVALID = Input(Bool())
  val AWREADY = Output(Bool())
  val AWID = Input(UInt(4.W))
  val AWADDR = Input(UInt(20.W))
  val AWLEN = Input(UInt(2.W))
  val AWSIZE = Input(UInt(2.W))
  // ...
}
```
Expressing something that matches a standard Verilog interface is important when instantiating Verilog
modules in a Chisel design as `BlackBoxes`.
Generally though, Chisel developers prefer to use composition via utilities like `Decoupled` rather
than a flat handling of `ready` and `valid` as in the above.

## Other Use Cases

## Advanced Details

### Type Classes in Scala

#### Implicit Resolution

### DataProduct