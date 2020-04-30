package dto

import java.util.UUID

case class OutImageDTO(id: UUID, name: String, storedUri: String, visibility: Boolean)
