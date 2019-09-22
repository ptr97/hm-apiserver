package com.pwos.api.domain.opinions.reports

import com.pwos.api.PaginatedResult


trait ReportDAOAlgebra[F[_]] {

  def create(report: Report): F[Report]

  def list(opinionId: Long): F[PaginatedResult[Report]]

}
