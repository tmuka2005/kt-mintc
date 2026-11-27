package com.t_mukai.mint.syntax

import com.t_mukai.mint.parser.Parser
import com.t_mukai.mint.parser.map

@Suppress("UnusedReceiverParameter")
public companion val Parser.ofDesignation:
  Parser<Designation, IdentifierWarning>
  get() = designation

// region internal

private val designation: Parser<Designation, IdentifierWarning> =
  Parser.ofIdentifier.map(Designation::Name)

// endregion
