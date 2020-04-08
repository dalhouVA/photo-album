package dto

import java.util.UUID

case class ImageDTO(id: Option[UUID], name: String, uri: String)
