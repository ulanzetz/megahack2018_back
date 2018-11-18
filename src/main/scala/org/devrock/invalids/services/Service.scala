package org.devrock.invalids.services

import com.softwaremill.session.SessionManager
import org.devrock.invalids.domain.Accounts.UserSession
import org.devrock.invalids.utils.{CatsEffectSupport, JsonSupport}

trait Service extends CatsEffectSupport with JsonSupport {
  protected implicit val sessionManager: SessionManager[UserSession]
}
