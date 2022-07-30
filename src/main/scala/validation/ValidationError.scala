package io.github.cakelier
package validation

/** An error that can happen while validating a [[RemoteConfiguration]].
  *
  * This entity represents a possible error that can happen while validating a [[RemoteConfiguration]], either read from a file or
  * directly created via code. This means that the kind of instances of this error can refer to missing, wrongly formatted or
  * invalid values for a given field. Each error has a human-readable message that can be displayed to the user. All instances of
  * this trait are contained into its companion object.
  */
private[cakelier] sealed trait ValidationError {

  /** Returns the human-readable message associated with this error to display to the user. */
  val message: String
}

/** Companion object to the [[ValidationError]] trait, containing its instances. */
private[cakelier] object ValidationError {

  /** Error that gets raised when the name of a [[RemoteConfiguration]] found in a configuration file is associated with an
    * invalid configuration.
    *
    * The operation of parsing a configuration file starts from reading the names of all HOCON configurations in a file and then
    * checking if a given name is associated to a configuration or not, so a value that follows the convention for this format. If
    * not, this error gets raised.
    *
    * @param configuration
    *   the name of the offending [[RemoteConfiguration]]
    */
  final case class InvalidRemoteKey(configuration: String) extends ValidationError {

    override val message: String = s"The remote configuration named '$configuration' was not a valid configuration."
  }

  object MissingOrInvalidHostValue extends ValidationError {

    override val message: String = "The host value is missing or not a valid string."
  }

  /** Error that gets raised when the host value is missing from the [[RemoteConfiguration]].
    *
    * A [[RemoteConfiguration]], for being valid, needs that at least the hostname and the user have been specified. If the first
    * has not been specified, either because the field was missing from the configuration file or because the code creating the
    * [[RemoteConfiguration]] did not specify the value, this error gets raised.
    */
  object MissingHostValue extends ValidationError {

    override val message: String = "The host value is missing."
  }

  object InvalidHostValue extends ValidationError {

    override val message: String = "The host value must be a valid hostname or a valid IP address."
  }

  object MissingOrInvalidUserValue extends ValidationError {

    override val message: String = "The user value is missing or is not a valid string."
  }

  /** Error that gets raised when the user value is missing from the [[RemoteConfiguration]].
    *
    * A [[RemoteConfiguration]], for being valid, needs that at least the hostname and the user have been specified. If the last
    * has not been specified, either because the field was missing from the configuration file or because the code creating the
    * [[RemoteConfiguration]] did not specify the value, this error gets raised.
    */
  object MissingUserValue extends ValidationError {

    override val message: String = "The user value is missing."
  }

  object InvalidUserValue extends ValidationError {

    override val message: String = "The user value must be a valid username."
  }

  /** Error that gets raised when the port value is specified with an invalid format.
    *
    * A [[RemoteConfiguration]], for being valid, needs that, if the port value is specified, this one is an integer between 1 and
    * 65535. This is because the ports range for any host allows only those values. If this is not the case, this error gets
    * raised.
    */
  object InvalidPortValue extends ValidationError {

    override val message: String = "The port value must be a integer between 1 and 65535."
  }

  /** Error that gets raised when any string field value is specified with an incorrect format.
    *
    * A [[RemoteConfiguration]], for being valid, needs that, if a given string field value is specified, this one is in fact a
    * string. If this is not the case, this error gets raised.
    *
    * @param field
    *   the name of the offending field in the [[RemoteConfiguration]]
    */
  final case class InvalidStringFieldValue(field: String) extends ValidationError {

    override val message: String = s"The $field value must be a string."
  }

  /** Error that gets raised when the private key file value is specified with an incorrect format.
    *
    * A [[RemoteConfiguration]], for being valid, needs that, if the private key file value is specified, this one is a path to a
    * file in the local filesystem which actually exists and is readable. If not, this error gets raised.
    */
  object InvalidPrivateKeyFileValue extends ValidationError {

    override val message: String =
      "The private key file value must be a path in the filesystem to an existing, readable file."
  }
}
