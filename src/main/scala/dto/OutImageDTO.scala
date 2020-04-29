package dto

import java.util.UUID

import components.Album

case class OutImageDTO(id: UUID, name: String, storedUri: String, visibility: Boolean)
