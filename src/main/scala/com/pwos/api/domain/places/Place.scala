package com.pwos.api.domain.places


case class Place(
                  name: String,
                  latitude: Double,
                  longitude: Double,
                  elevation: Double,
                  id: Option[Long] = None,
                )
