package com.pwos.api.domain.places


case class Place(
                  name: String,
                  height: Double,
                  id: Option[Long] = None,
                )
