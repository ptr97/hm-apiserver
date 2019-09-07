package com.pwos.api.domain.tags


object TagCategories extends Enumeration {
  type TagCategory = Value

  val SUBSOIL: TagCategory = Value("subsoil")
  val EQUIPMENT: TagCategory = Value("equipment")
  val THREATS: TagCategory = Value("threats")
}

case class Tag(
                uuid: String,
                name: String,
                enabled: Boolean,
                tagCategory: TagCategories.Value,
                id: Option[Long]
              )
