package generator

import java.util.UUID

trait Generator {

  def id:UUID

}

class UUIDGenerator extends Generator{
  override def id: UUID = UUID.randomUUID()
}
