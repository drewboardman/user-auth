package application.algebras

import com.google.api.client.googleapis.auth.oauth2.{GoogleIdToken, GoogleIdTokenVerifier}
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory

import scala.jdk.CollectionConverters._

/**
 * This exists so I can test things easier
 */
trait GoogleVerificationWrapper {
  def verify(idToken: GoogleIdToken): Boolean
}

//add config to this
final class LiveGoogleVerificationWrapper extends GoogleVerificationWrapper {
  private val builder = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance)

  private val tokenVerifier: GoogleIdTokenVerifier =
    builder
      .setAudience(List("foo").asJava) // this should be encrypted
      .build()

  override def verify(idToken: GoogleIdToken): Boolean = tokenVerifier.verify(idToken)
}
