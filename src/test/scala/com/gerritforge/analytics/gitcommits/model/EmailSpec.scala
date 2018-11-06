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

import org.scalatest.{FlatSpec, Matchers}

import scala.util.Try

class EmailSpec extends FlatSpec with Matchers {
  "Email" should "be instantiated when domain has no extension" in {
    "user@domain-simple" match {
      case Email(user, domain) =>
        user shouldBe "user"
        domain shouldBe "domain-simple"
    }
  }

  it should "be instantiated with com extension" in {
    "user@domain-com.com" match {
      case Email(user, domain) =>
        user shouldBe "user"
        domain shouldBe "domain-com"
    }
  }

  it should "be instantiated with co.uk extension" in {
    "user@domain-couk.co.uk" match {
      case Email(user, domain) =>
        user shouldBe "user"
        domain shouldBe "domain-couk"
    }
  }

  it should "be instantiated with info extension" in {
    "user@domain-info.info" match {
      case Email(user, domain) =>
        user shouldBe "user"
        domain shouldBe "domain-info"
    }
  }

  it should "be instantiated with com extension and 'dotted' company name" in {
    "user@my.companyname-com.com" match {
      case Email(user, domain) =>
        user shouldBe "user"
        domain shouldBe "my.companyname-com"
    }
  }

  it should "be instantiated with co.uk extension and 'dotted' company name" in {
    "user@my.companyname-couk.co.uk" match {
      case Email(user, domain) =>
        user shouldBe "user"
        domain shouldBe "my.companyname-couk"
    }
  }

  it should "be instantiated with info extension and 'dotted' company name" in {
    "user@my.companyname-info.info" match {
      case Email(user, domain) =>
        user shouldBe "user"
        domain shouldBe "my.companyname-info"
    }
  }

  it should "not match an invalid mail format" in {
    "invalid email" match {
      case Email(_, _) => fail("Invalid emails should be rejected")
      case _           =>
    }
  }
}
