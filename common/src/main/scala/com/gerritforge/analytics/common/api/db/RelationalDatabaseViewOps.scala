// Copyright (C) 2019 GerritForge Ltd
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.analytics.common.api.db
import java.sql.Connection

import resource.managed
import scala.concurrent.Future

trait RelationalDatabaseViewOps {

    def updateViewToTheNewTable(newTableName: String, viewName: String)(implicit connection: Connection): Future[Boolean] = {
      import scala.concurrent.ExecutionContext.Implicits.global
      Future {
        managed(connection.createStatement())
          .map(statement => statement.execute(s"""CREATE OR REPLACE VIEW $viewName AS
          SELECT * FROM $newTableName""")).opt.get
      }
    }
}
