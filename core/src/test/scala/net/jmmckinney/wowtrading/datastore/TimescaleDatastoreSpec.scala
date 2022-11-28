package net.jmmckinney.wowtrading.datastore

import net.jmmckinney.wowtrading.datastore.DatastoreSpec
import org.specs2.specification.core.SpecificationStructure
import org.specs2.execute.Failure
import net.jmmckinney.wowtrading.datastore.Datastore
import org.specs2.specification.core.Fragments
import org.specs2.execute
import org.specs2.main.ArgProperty
import org.specs2.specification.core.Fragment
import org.specs2.data.NamedTag
import org.specs2.execute.Pending
import org.specs2.specification.core.SpecStructure
import org.specs2.main.Arguments
import org.specs2.specification.create.InterpolatedFragment

import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts
import net.jmmckinney.wowtrading.api.json.response.CommoditySnapshot
import fs2.io.file.Files
import cats.effect.IO
import fs2.io.file.Path

class TimescaleDatastoreSpec extends DatastoreSpec {
  
  override def datastoreResource: DatastoreResource = TimescaleDatastoreResource(
    url = "jdbc:postgresql://127.0.0.1/",
    user = "postgres",
    pass = "password"
  )
}
