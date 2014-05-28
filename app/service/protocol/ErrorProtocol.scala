package service.protocol

/*
 * Errors
 */


case class DatabaseError(message: String) extends Error(message)


case class PermissionDeniedError(message: String) extends Error(message)