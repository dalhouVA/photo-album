package dto

import java.util.UUID

import core.Album

case class OutImageDTO(id: UUID, name: String, storedUri: String, visibility: Boolean)
