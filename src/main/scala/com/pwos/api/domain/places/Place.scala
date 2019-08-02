package com.pwos.api.domain.places


case class Place(
                  name: String,
                  latitude: Double,
                  longitude: Double,
                  elevation: Double,
                  id: Option[Long] = None,
                )


case class PlaceUpdateModel(
                             name: Option[String] = None,
                             latitude: Option[Double] = None,
                             longitude: Option[Double] = None,
                             elevation: Option[Double] = None,
                           )
