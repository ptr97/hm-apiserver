package com.pwos.api.domain.opinions.tags


object TagCategory extends Enumeration {
  type TagCategory = Value

  val SUBSOIL: TagCategory = Value("subsoil")
  val EQUIPMENT: TagCategory = Value("equipment")
  val THREATS: TagCategory = Value("threats")
}


case class Tag(
  uuid: String,
  name: String,
  enabled: Boolean,
  tagCategory: TagCategory.Value,
  id: Option[Long]
)
