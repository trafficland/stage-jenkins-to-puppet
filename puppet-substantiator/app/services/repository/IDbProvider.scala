package services.repository

trait IDbProvider[DB] {
  def db: DB
}
