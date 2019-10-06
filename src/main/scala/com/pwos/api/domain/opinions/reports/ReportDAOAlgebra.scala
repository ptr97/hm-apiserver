package com.pwos.api.domain.opinions.reports


trait ReportDAOAlgebra[F[_]] {

  def create(report: Report): F[Report]

  def list(opinionId: Long): F[List[Report]]

}
