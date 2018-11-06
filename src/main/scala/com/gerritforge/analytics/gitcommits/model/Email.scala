// Copyright (C) 2017 GerritForge Ltd
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

package com.gerritforge.analytics.gitcommits.model

case class Email(user: String, domain: String)

object Email {
  val emailWithOutExtension = """(.*?)@([^.]+)$""".r
  val emailWithExtension = """(.*?)@(.*?)(?:\.co)?.[a-z]{2,4}$""".r

  def unapply(emailString: String): Option[(String, String)] = {
    emailString.toLowerCase match {
      case emailWithOutExtension(u,d) => Some(u,d)
      case emailWithExtension(u,d) => Some(u,d)
      case _ => None
    }
  }
}
