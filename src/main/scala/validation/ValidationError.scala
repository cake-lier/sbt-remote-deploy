package io.github.cakelier
package validation

sealed trait ValidationError {

  val message: String
}

final case class MissingRemoteKey(name: String) extends ValidationError {

  override val message: String = s"The remote named $name was not found, an error must have occurred."
}

final case class MissingHostValue(name: String) extends ValidationError {

  override val message: String = s"The host value is missing from the $name remote configuration."
}

final case class MissingUserValue(name: String) extends ValidationError {

  override val message: String = s"The user value is missing from the $name remote configuration."
}

final case class WrongPortFormat(name: String) extends ValidationError {

  override val message: String =
    s"The format of the port value is wrong in $name remote configuration. It must be a positive integer between 1 and 65535."
}

final case class WrongStringOptionalFieldFormat(name: String, field: String) extends ValidationError {

  override val message: String = s"The format of the field $field is wrong in $name configuration. It must be a string."
}

final case class WrongPrivateKeyFileFormat(name: String) extends ValidationError {

  override val message: String =
    s"The format of the private key file field is wrong in $name configuration. It must be a path in the filesystem."
}
