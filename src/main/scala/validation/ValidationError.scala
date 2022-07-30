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

  /** Error that gets raised when the name of a [[RemoteConfiguration]] found in a configuration file is associated with a
    * configuration specified with an invalid format.
    *
    * The parsing operation of a configuration file starts from reading the names of all HOCON configurations in a file and then
    * checking if a given name is associated to a configuration or not, so to a value that follows the convention for this format.
    * If not, this error gets raised.
    *
    * @param configuration
    *   the name of the offending [[RemoteConfiguration]]
    */
  final case class InvalidRemoteKey(configuration: String) extends ValidationError {

    override val message: String = s"The remote configuration named '$configuration' was not a valid configuration."
  }

  /** Error that gets raised when the host value is specified with an invalid format.
    *
    * A [[RemoteConfiguration]], for being valid, needs that the specified hostname is a valid hostname or IP address. This is
    * because the SSH protocol requires a valid hostname or IP address to which connect. If this is not the case, this error gets
    * raised.
    */
  object InvalidHostValue extends ValidationError {

    override val message: String = "The host value must be a valid hostname or a valid IP address."
  }

  /** Error that gets raised when the user value is specified with an invalid format.
    *
    * A [[RemoteConfiguration]], for being valid, needs that the specified username is valid. This is because the SSH protocol
    * requires a valid username for the remote location to which connect. If this is not the case, this error gets raised.
    */
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

  /** Error that gets raised when the private key file value is specified with an invalid format.
    *
    * A [[RemoteConfiguration]], for being valid, needs that, if the private key file value is specified, this one is a path to a
    * file in the local filesystem which actually exists and is readable. This is because this file will be read later on and
    * these conditions are necessary for this to be done. If this is not the case, this error gets raised.
    */
  object InvalidPrivateKeyFileValue extends ValidationError {

    override val message: String =
      "The private key file value must be a path in the filesystem to an existing, readable file."
  }

  /** Error that gets raised when the "verify identity" field value is specified with an incorrect format.
    *
    * A configuration file containing a [[RemoteConfiguration]], for being valid, needs that, if a "verify identity" field value
    * is specified, this one is a boolean. If this is not the case, this error gets raised.
    */
  object InvalidVerifyIdentityValue extends ValidationError {

    override val message: String = "The \"verify identity\" parameter must be a boolean."
  }

  /** Error that gets raised when the fingerprint value is specified with an invalid format.
    *
    * A [[RemoteConfiguration]], for being valid, needs that, if the fingerprint value is specified, this one is in an MD5
    * colon-delimited format (16 hexadecimal octets, delimited by a colon), or in a Base64 encoded format for SHA-1 or SHA-256
    * fingerprints. The MD5 format can be prefixed, but it is not mandatory, with the string "MD5:", while the SHA-1 and SHA-256
    * formatted fingerprints must be prefixed respectively with "SHA1:" and "SHA256:" strings. The terminal "=" symbols are
    * optional. This is because these three are the only supported formats for encoding fingerprints. If neither of those formats
    * are used, this error gets raised.
    */
  object InvalidFingerprintValue extends ValidationError {

    override val message: String =
      """
        |The fingerprint value must be a string in an MD5 colon-delimited format (16 hexadecimal octets, delimited by a colon), 
        |or in a Base64 encoded format for SHA-1 or SHA-256 fingerprints. The MD5 format can be prefixed, but it is not 
        |mandatory, with the string "MD5:", while the SHA-1 and SHA-256 formatted fingerprints must be prefixed 
        |respectively with "SHA1:" and "SHA256:" strings. The terminal "=" symbols are optional.
        |""".stripMargin
  }

  /** Error that gets raised when a field value is missing or invalid in the configuration file.
    *
    * A configuration file containing a [[RemoteConfiguration]], for being valid, needs that some fields must be present and
    * associated with a value which is of type string or of a type that can be converted to a string. If this is not the case,
    * this error gets raised.
    *
    * @param field
    *   the name of the offending field in the [[RemoteConfiguration]] file
    */
  final case class MissingOrInvalidStringFieldValue(field: String) extends ValidationError {

    override val message: String = s"The $field value is missing or not a valid string."
  }

  /** Error that gets raised when a field value is missing from the [[RemoteConfiguration]].
    *
    * A [[RemoteConfiguration]], for being valid, needs that some values must have been specified. If this is not the case because
    * the code creating the [[RemoteConfiguration]] did not specify those values, this error gets raised.
    *
    * @param field
    *   the name of the offending field in the [[RemoteConfiguration]]
    */
  final case class MissingFieldValue(field: String) extends ValidationError {

    override val message: String = s"The $field value is missing."
  }

  /** Error that gets raised when any string field value is specified with an incorrect format.
    *
    * A configuration file containing a [[RemoteConfiguration]], for being valid, needs that, if a given string field value is
    * specified, this one is in fact a string. If this is not the case, this error gets raised.
    *
    * @param field
    *   the name of the offending field in the [[RemoteConfiguration]] file
    */
  final case class InvalidStringFieldValue(field: String) extends ValidationError {

    override val message: String = s"The $field value is not a valid string."
  }
}
