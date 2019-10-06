package com.pwos.api.domain.opinions.tags


object TagCategory extends Enumeration {
  type TagCategory = Value

  val SUBSOIL: TagCategory = Value("subsoil")
  val EQUIPMENT: TagCategory = Value("equipment")
  val THREATS: TagCategory = Value("threats")
}


case class Tag(
  name: String,
  tagCategory: TagCategory.Value,
  enabled: Boolean = true,
  id: Option[Long] = None
)
